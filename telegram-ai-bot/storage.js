const { Pool } = require("pg");

const pool = process.env.DATABASE_URL
  ? new Pool({
      connectionString: process.env.DATABASE_URL,
      ssl: process.env.PGSSLMODE === "disable" ? false : { rejectUnauthorized: false },
      max: 5,
      idleTimeoutMillis: 30000,
      connectionTimeoutMillis: 10000
    })
  : null;

const memory = {
  users: new Map(),
  cooldowns: new Map(),
  cards: [],
  groupMembers: new Map(),
  nextCopyId: 1,
  blockedUsers: new Set()
};

function memoryUser(id) {
  const key = Number(id);
  if (!memory.users.has(key)) {
    memory.users.set(key, {
      id: key, username: null, name: "", balance: 0,
      achievements: [], views: 0, title: null, profileBackground: null
    });
  }
  return memory.users.get(key);
}

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
    ALTER TABLE ncard_users ADD COLUMN IF NOT EXISTS title TEXT;
    ALTER TABLE ncard_users ADD COLUMN IF NOT EXISTS profile_background TEXT;
    ALTER TABLE ncard_users ADD COLUMN IF NOT EXISTS achievements JSONB NOT NULL DEFAULT '[]'::jsonb;
    ALTER TABLE ncard_users ADD COLUMN IF NOT EXISTS balance BIGINT NOT NULL DEFAULT 0;

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
    CREATE INDEX IF NOT EXISTS ncard_cards_rarity_idx ON ncard_cards(telegram_id, rarity);

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
    CREATE TABLE IF NOT EXISTS ncard_user_blocks (
      telegram_id BIGINT PRIMARY KEY REFERENCES ncard_users(telegram_id) ON DELETE CASCADE,
      reason TEXT NOT NULL DEFAULT '',
      blocked_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );
    CREATE TABLE IF NOT EXISTS ncard_admin_log (
      id BIGSERIAL PRIMARY KEY,
      owner_id BIGINT NOT NULL,
      target_id BIGINT NOT NULL,
      card_key TEXT NOT NULL,
      rarity TEXT NOT NULL,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );
  `);
  console.log("NCARD PostgreSQL storage initialized.");
}

async function upsertUser(from) {
  const name = [from.first_name, from.last_name].filter(Boolean).join(" ");
  if (!pool) {
    const u = memoryUser(from.id);
    u.username = from.username || u.username;
    u.name = name || u.name;
    return { ...u };
  }
  const r = await pool.query(
    `INSERT INTO ncard_users(telegram_id, username, display_name)
     VALUES($1,$2,$3)
     ON CONFLICT(telegram_id) DO UPDATE SET
       username=COALESCE(EXCLUDED.username,ncard_users.username),
       display_name=CASE WHEN EXCLUDED.display_name='' THEN ncard_users.display_name ELSE EXCLUDED.display_name END,
       updated_at=NOW()
     RETURNING telegram_id,username,display_name,balance,achievements,profile_views,title,profile_background`,
    [String(from.id), from.username || null, name]
  );
  return mapUser(r.rows[0]);
}

function mapUser(u) {
  if (!u) return null;
  return {
    id: Number(u.telegram_id), username: u.username, name: u.display_name,
    balance: Number(u.balance || 0), achievements: u.achievements || [],
    views: Number(u.profile_views || 0), title: u.title,
    profileBackground: u.profile_background
  };
}

async function getUser(userId) {
  if (!pool) return { ...memoryUser(userId) };
  const r = await pool.query(
    "SELECT telegram_id,username,display_name,balance,achievements,profile_views,title,profile_background FROM ncard_users WHERE telegram_id=$1",
    [String(userId)]
  );
  return mapUser(r.rows[0]);
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
     VALUES($1,$2) ON CONFLICT(chat_id,telegram_id)
     DO UPDATE SET last_seen=NOW()`,
    [String(chatId), String(userId)]
  );
}

async function getCooldown(userId) {
  if (!pool) return memory.cooldowns.get(Number(userId)) || 0;
  const r = await pool.query("SELECT until_ms FROM ncard_cooldowns WHERE telegram_id=$1", [String(userId)]);
  return r.rowCount ? Number(r.rows[0].until_ms) : 0;
}

async function claimCooldown(userId, now, duration) {
  const until = now + duration;
  if (!pool) {
    const current = memory.cooldowns.get(Number(userId)) || 0;
    if (current > now) return { ok: false, until: current };
    memory.cooldowns.set(Number(userId), until);
    return { ok: true, until };
  }
  const r = await pool.query(
    `INSERT INTO ncard_cooldowns(telegram_id,until_ms)
     VALUES($1,$2)
     ON CONFLICT(telegram_id) DO UPDATE SET until_ms=EXCLUDED.until_ms
     WHERE ncard_cooldowns.until_ms <= $3
     RETURNING until_ms`,
    [String(userId), String(until), String(now)]
  );
  if (r.rowCount) return { ok: true, until: Number(r.rows[0].until_ms) };
  return { ok: false, until: await getCooldown(userId) };
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
    const u = memoryUser(userId);
    const entry = { ...card, copyId: memory.nextCopyId++, obtainedAt: Date.now() };
    memory.cards.push({ userId: Number(userId), ...entry });
    return entry;
  }
  const r = await pool.query(
    `INSERT INTO ncard_cards(telegram_id,card_key,model,brand,rarity,value)
     VALUES($1,$2,$3,$4,$5,$6)
     RETURNING id,obtained_at`,
    [String(userId), card.id, card.name, card.brand, card.rarity, card.value]
  );
  return { ...card, copyId: Number(r.rows[0].id), obtainedAt: new Date(r.rows[0].obtained_at).getTime() };
}

async function getCollection(userId) {
  if (!pool) return memory.cards.filter(c => c.userId === Number(userId)).map(({ userId: _id, ...c }) => c);
  const r = await pool.query(
    "SELECT id AS copy_id,card_key AS id,model AS name,brand,rarity,value,obtained_at FROM ncard_cards WHERE telegram_id=$1 ORDER BY obtained_at DESC",
    [String(userId)]
  );
  return r.rows.map(c => ({
    ...c, copyId: Number(c.copy_id), value: Number(c.value),
    obtainedAt: new Date(c.obtained_at).getTime()
  }));
}

async function getGroupRanking(chatId, limit = 10) {
  if (!pool) {
    const ids = memory.groupMembers.get(String(chatId)) || new Set();
    return [...ids].map(id => {
      const u = memory.users.get(id);
      return u ? { id, name: u.name, username: u.username, count: memory.cards.filter(c => c.userId === id).length } : null;
    }).filter(Boolean).sort((a,b) => b.count-a.count).slice(0, limit);
  }
  const r = await pool.query(
    `SELECT u.telegram_id,u.display_name,u.username,COUNT(c.id)::int AS card_count
     FROM ncard_group_members gm
     JOIN ncard_users u ON u.telegram_id=gm.telegram_id
     LEFT JOIN ncard_cards c ON c.telegram_id=u.telegram_id
     WHERE gm.chat_id=$1
     GROUP BY u.telegram_id,u.display_name,u.username
     ORDER BY card_count DESC,u.display_name ASC LIMIT $2`,
    [String(chatId), limit]
  );
  return r.rows.map(u => ({
    id: Number(u.telegram_id), name: u.display_name,
    username: u.username, count: u.card_count
  }));
}

async function massUpgrade(userId, rarity, nextRarity, nextValue, successChance = 0.5) {
  if (!pool) {
    const owned = memory.cards.filter(c => c.userId === Number(userId) && c.rarity === rarity);
    const results = [];
    for (const card of owned) {
      if (Math.random() < successChance) {
        card.rarity = nextRarity;
        card.value = nextValue;
        results.push({ name: card.name, success: true, rarity: nextRarity });
      } else {
        const index = memory.cards.indexOf(card);
        if (index >= 0) memory.cards.splice(index, 1);
        results.push({ name: card.name, success: false });
      }
    }
    return results;
  }
  const client = await pool.connect();
  try {
    await client.query("BEGIN");
    const selected = await client.query(
      "SELECT id,model FROM ncard_cards WHERE telegram_id=$1 AND rarity=$2 FOR UPDATE",
      [String(userId), rarity]
    );
    const results = [];
    for (const card of selected.rows) {
      if (Math.random() < successChance) {
        await client.query("UPDATE ncard_cards SET rarity=$2,value=$3 WHERE id=$1", [card.id, nextRarity, nextValue]);
        results.push({ name: card.model, success: true, rarity: nextRarity });
      } else {
        await client.query("DELETE FROM ncard_cards WHERE id=$1", [card.id]);
        results.push({ name: card.model, success: false });
      }
    }
    await client.query("COMMIT");
    return results;
  } catch (error) {
    await client.query("ROLLBACK");
    throw error;
  } finally {
    client.release();
  }
}

async function addBalance(userId, amount) {
  if (!Number.isSafeInteger(amount)) throw new Error("Balance amount must be a safe integer.");
  if (!pool) {
    const u = memoryUser(userId);
    u.balance += amount;
    return u.balance;
  }
  const r = await pool.query(
    "UPDATE ncard_users SET balance=balance+$2,updated_at=NOW() WHERE telegram_id=$1 RETURNING balance",
    [String(userId), amount]
  );
  if (!r.rowCount) throw new Error("User not found.");
  return Number(r.rows[0].balance);
}

async function awardAchievement(userId, achievementId, rewardRubles = 0) {
  if (!pool) {
    const u = memoryUser(userId);
    if (u.achievements.includes(achievementId)) return false;
    u.achievements.push(achievementId);
    u.balance += rewardRubles;
    return true;
  }
  const r = await pool.query(
    `UPDATE ncard_users
     SET achievements=achievements || to_jsonb($2::text),
         balance=balance+$3,updated_at=NOW()
     WHERE telegram_id=$1 AND NOT (achievements ? $2)
     RETURNING telegram_id`,
    [String(userId), achievementId, rewardRubles]
  );
  return r.rowCount > 0;
}

async function setProfileField(userId, field, value) {
  const columns = { title: "title", profileBackground: "profile_background" };
  const column = columns[field];
  if (!column) throw new Error("Unsupported profile field.");
  if (!pool) {
    memoryUser(userId)[field] = value;
    return;
  }
  await pool.query(`UPDATE ncard_users SET ${column}=$2,updated_at=NOW() WHERE telegram_id=$1`, [String(userId), value]);
}

async function isBlocked(userId) {
  if (!pool) return memory.blockedUsers.has(Number(userId));
  const r = await pool.query("SELECT 1 FROM ncard_user_blocks WHERE telegram_id=$1", [String(userId)]);
  return r.rowCount > 0;
}

async function setBlocked(userId, blocked, reason = "") {
  if (!pool) {
    if (blocked) memory.blockedUsers.add(Number(userId));
    else memory.blockedUsers.delete(Number(userId));
    return;
  }
  if (blocked) {
    await pool.query(
      "INSERT INTO ncard_user_blocks(telegram_id,reason) VALUES($1,$2) ON CONFLICT(telegram_id) DO UPDATE SET reason=EXCLUDED.reason,blocked_at=NOW()",
      [String(userId), reason]
    );
  } else {
    await pool.query("DELETE FROM ncard_user_blocks WHERE telegram_id=$1", [String(userId)]);
  }
}

async function getStats() {
  if (!pool) {
    return {
      users: memory.users.size,
      cards: memory.cards.length,
      balance: [...memory.users.values()].reduce((n,u) => n + u.balance, 0)
    };
  }
  const r = await pool.query(
    "SELECT (SELECT COUNT(*) FROM ncard_users)::int AS users,(SELECT COUNT(*) FROM ncard_cards)::int AS cards,(SELECT COALESCE(SUM(balance),0) FROM ncard_users)::bigint AS balance"
  );
  return { ...r.rows[0], balance: Number(r.rows[0].balance) };
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
  init, upsertUser, getUser, recordGroupMember, getCooldown, claimCooldown,
  setCooldown, addCard, getCollection, getGroupRanking, addBalance,
  awardAchievement, setProfileField, massUpgrade, isBlocked, setBlocked, getStats, logAdminGrant, close,
  persistent: Boolean(pool)
};
