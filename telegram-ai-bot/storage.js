const { Pool } = require("pg");

const pool = process.env.DATABASE_URL
  ? new Pool({
      connectionString: process.env.DATABASE_URL,
      ssl: process.env.PGSSLMODE === "disable" ? false : { rejectUnauthorized: false }
    })
  : null;

const memory = {
  users: new Map(),
  cooldowns: new Map(),
  cards: [],
  groupMembers: new Map()
};

async function init() {
  if (!pool) {
    console.warn("DATABASE_URL is not configured. NCARD is using temporary in-memory storage.");
    return;
  }
  await pool.query(`
    CREATE TABLE IF NOT EXISTS ncard_users (
      telegram_id BIGINT PRIMARY KEY,
      username TEXT,
      display_name TEXT NOT NULL DEFAULT '',
      balance BIGINT NOT NULL DEFAULT 0,
      title TEXT,
      profile_background TEXT,
      achievements JSONB NOT NULL DEFAULT '[]'::jsonb,
      profile_views BIGINT NOT NULL DEFAULT 0,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );
    CREATE TABLE IF NOT EXISTS ncard_cards (
      id BIGSERIAL PRIMARY KEY,
      telegram_id BIGINT NOT NULL REFERENCES ncard_users(telegram_id) ON DELETE CASCADE,
      card_key TEXT NOT NULL,
      model TEXT NOT NULL,
      brand TEXT NOT NULL,
      rarity TEXT NOT NULL,
      value BIGINT NOT NULL,
      obtained_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );
    CREATE INDEX IF NOT EXISTS ncard_cards_user_idx ON ncard_cards(telegram_id);
    CREATE TABLE IF NOT EXISTS ncard_cooldowns (
      telegram_id BIGINT PRIMARY KEY REFERENCES ncard_users(telegram_id) ON DELETE CASCADE,
      until_ms BIGINT NOT NULL
    );
    CREATE TABLE IF NOT EXISTS ncard_group_members (
      chat_id BIGINT NOT NULL,
      telegram_id BIGINT NOT NULL REFERENCES ncard_users(telegram_id) ON DELETE CASCADE,
      last_seen TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      PRIMARY KEY(chat_id, telegram_id)
    );
    CREATE INDEX IF NOT EXISTS ncard_group_rank_idx ON ncard_group_members(chat_id, telegram_id);
    CREATE TABLE IF NOT EXISTS ncard_admin_log (
      id BIGSERIAL PRIMARY KEY,
      owner_id BIGINT NOT NULL,
      target_id BIGINT NOT NULL,
      card_key TEXT NOT NULL,
      rarity TEXT NOT NULL,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );
  `);
}

async function upsertUser(from) {
  const name = [from.first_name, from.last_name].filter(Boolean).join(" ");
  if (!pool) {
    const old = memory.users.get(from.id) || {
      id: from.id, username: null, name: "", balance: 0,
      achievements: [], views: 0, title: null, profileBackground: null
    };
    old.username = from.username || old.username;
    old.name = name || old.name;
    memory.users.set(from.id, old);
    return old;
  }
  const result = await pool.query(
    `INSERT INTO ncard_users(telegram_id, username, display_name)
     VALUES($1,$2,$3)
     ON CONFLICT(telegram_id) DO UPDATE SET
       username=EXCLUDED.username,
       display_name=CASE WHEN EXCLUDED.display_name='' THEN ncard_users.display_name ELSE EXCLUDED.display_name END,
       updated_at=NOW()
     RETURNING telegram_id, username, display_name, balance, achievements,
       profile_views, title, profile_background`,
    [String(from.id), from.username || null, name]
  );
  const u = result.rows[0];
  return {
    id: Number(u.telegram_id), username: u.username, name: u.display_name,
    balance: Number(u.balance), achievements: u.achievements || [],
    views: Number(u.profile_views), title: u.title,
    profileBackground: u.profile_background
  };
}

async function recordGroupMember(chatId, userId) {
  if (!chatId || !userId) return;
  if (!pool) {
    const key = String(chatId);
    if (!memory.groupMembers.has(key)) memory.groupMembers.set(key, new Set());
    memory.groupMembers.get(key).add(Number(userId));
    return;
  }
  await pool.query(
    `INSERT INTO ncard_group_members(chat_id,telegram_id)
     VALUES($1,$2)
     ON CONFLICT(chat_id,telegram_id) DO UPDATE SET last_seen=NOW()`,
    [String(chatId), String(userId)]
  );
}

async function getCooldown(userId) {
  if (!pool) return memory.cooldowns.get(Number(userId)) || 0;
  const r = await pool.query("SELECT until_ms FROM ncard_cooldowns WHERE telegram_id=$1", [String(userId)]);
  return r.rowCount ? Number(r.rows[0].until_ms) : 0;
}

async function setCooldown(userId, untilMs) {
  if (!pool) {
    memory.cooldowns.set(Number(userId), untilMs);
    return;
  }
  await pool.query(
    `INSERT INTO ncard_cooldowns(telegram_id,until_ms) VALUES($1,$2)
     ON CONFLICT(telegram_id) DO UPDATE SET until_ms=EXCLUDED.until_ms`,
    [String(userId), String(untilMs)]
  );
}

async function addCard(userId, card) {
  if (!pool) {
    const user = memory.users.get(Number(userId));
    if (user) {
      if (!user.cards) user.cards = [];
      user.cards.push({ ...card, obtainedAt: Date.now() });
    }
    return;
  }
  await pool.query(
    `INSERT INTO ncard_cards(telegram_id,card_key,model,brand,rarity,value)
     VALUES($1,$2,$3,$4,$5,$6)`,
    [String(userId), card.id, card.name, card.brand, card.rarity, card.value]
  );
}

async function getCollection(userId) {
  if (!pool) return memory.users.get(Number(userId))?.cards || [];
  const r = await pool.query(
    "SELECT card_key AS id, model AS name, brand, rarity, value, obtained_at FROM ncard_cards WHERE telegram_id=$1 ORDER BY obtained_at DESC",
    [String(userId)]
  );
  return r.rows.map(c => ({ ...c, value: Number(c.value), obtainedAt: new Date(c.obtained_at).getTime() }));
}

async function getGroupRanking(chatId, limit = 10) {
  if (!pool) {
    const ids = memory.groupMembers.get(String(chatId)) || new Set();
    return [...ids].map(id => {
      const u = memory.users.get(id);
      return u ? { id, name: u.name, username: u.username, count: (u.cards || []).length } : null;
    }).filter(Boolean).sort((a,b) => b.count-a.count).slice(0, limit);
  }
  const r = await pool.query(
    `SELECT u.telegram_id, u.display_name, u.username, COUNT(c.id)::int AS card_count
     FROM ncard_group_members gm
     JOIN ncard_users u ON u.telegram_id=gm.telegram_id
     LEFT JOIN ncard_cards c ON c.telegram_id=u.telegram_id
     WHERE gm.chat_id=$1
     GROUP BY u.telegram_id,u.display_name,u.username
     ORDER BY card_count DESC,u.display_name ASC
     LIMIT $2`,
    [String(chatId), limit]
  );
  return r.rows.map(u => ({
    id: Number(u.telegram_id), name: u.display_name,
    username: u.username, count: u.card_count
  }));
}

async function getStats() {
  if (!pool) {
    return {
      users: memory.users.size,
      cards: [...memory.users.values()].reduce((n,u) => n + (u.cards || []).length, 0)
    };
  }
  const r = await pool.query(
    "SELECT (SELECT COUNT(*) FROM ncard_users)::int AS users, (SELECT COUNT(*) FROM ncard_cards)::int AS cards"
  );
  return r.rows[0];
}

async function logAdminGrant(ownerId, targetId, card) {
  if (!pool) return;
  await pool.query(
    "INSERT INTO ncard_admin_log(owner_id,target_id,card_key,rarity) VALUES($1,$2,$3,$4)",
    [String(ownerId), String(targetId), card.id, card.rarity]
  );
}

async function close() {
  if (pool) await pool.end();
}

module.exports = {
  init, upsertUser, recordGroupMember, getCooldown, setCooldown,
  addCard, getCollection, getGroupRanking, getStats, logAdminGrant, close,
  persistent: Boolean(pool)
};
