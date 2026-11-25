"use strict";

const { Pool } = require("pg");
const crypto = require("crypto");

const REQUIRED = ["DATABASE_URL", "GITHUB_BACKUP_TOKEN", "BACKUP_ENCRYPTION_KEY"];
for (const key of REQUIRED) {
  if (!process.env[key]) throw new Error(`Missing required environment variable: ${key}`);
}
if (!/^[a-f0-9]{64}$/i.test(process.env.BACKUP_ENCRYPTION_KEY)) {
  throw new Error("BACKUP_ENCRYPTION_KEY must be 64 hexadecimal characters (32 bytes).");
}

const owner = process.env.GITHUB_BACKUP_OWNER || "nokium10-dot";
const repo = process.env.GITHUB_BACKUP_REPO || "ncard-backups";
const branch = process.env.GITHUB_BACKUP_BRANCH || "main";
const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: process.env.PGSSLMODE === "disable" ? false : { rejectUnauthorized: false }
});
const headers = {
  Authorization: `Bearer ${process.env.GITHUB_BACKUP_TOKEN}`,
  Accept: "application/vnd.github+json",
  "X-GitHub-Api-Version": "2022-11-28"
};

async function notify(message) {
  const token = process.env.TELEGRAM_BOT_TOKEN;
  const ownerId = process.env.OWNER_ID || "71195190";
  if (!token) return;
  try {
    await fetch(`https://api.telegram.org/bot${token}/sendMessage`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ chat_id: ownerId, text: message })
    });
  } catch (e) {
    console.error("Telegram notification failed:", e.message);
  }
}

async function github(path, options = {}) {
  const response = await fetch(`https://api.github.com/repos/${owner}/${repo}/contents/${path}`, {
    ...options,
    headers: { ...headers, ...(options.headers || {}) }
  });
  if (!response.ok) {
    const body = await response.text();
    throw new Error(`GitHub API ${response.status}: ${body.slice(0, 300)}`);
  }
  return response.status === 204 ? null : response.json();
}

async function main() {
  const tableNames = [
    "ncard_users",
    "ncard_cards",
    "ncard_cooldowns",
    "ncard_group_members",
    "ncard_admin_log",
    "ncard_user_blocks",
    "ncard_card_assets"
  ];
  const client = await pool.connect();
  try {
    await client.query("BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY");
    const data = {
      format: "ncard-postgres-json-v1",
      createdAt: new Date().toISOString(),
      database: "ncard_db",
      tables: {}
    };
    for (const table of tableNames) {
      const result = await client.query(`SELECT * FROM "${table}"`);
      data.tables[table] = result.rows;
    }
    await client.query("COMMIT");

    const iv = crypto.randomBytes(12);
    const key = Buffer.from(process.env.BACKUP_ENCRYPTION_KEY, "hex");
    const cipher = crypto.createCipheriv("aes-256-gcm", key, iv);
    const plaintext = Buffer.from(JSON.stringify(data));
    const encrypted = Buffer.concat([cipher.update(plaintext), cipher.final()]);
    const payload = Buffer.concat([iv, cipher.getAuthTag(), encrypted]).toString("base64");
    const stamp = new Date().toISOString().replace(/[:.]/g, "-");
    const path = `backups/ncard-${stamp}.json.enc`;

    await github(path, {
      method: "PUT",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        message: `Automated encrypted NCARD backup ${stamp}`,
        branch,
        content: Buffer.from(payload).toString("base64")
      })
    });

    const files = await github("backups");
    const backups = Array.isArray(files)
      ? files.filter(f => f.type === "file" && /^ncard-.*\.json\.enc$/.test(f.name))
          .sort((a, b) => b.name.localeCompare(a.name))
      : [];
    for (const old of backups.slice(7)) {
      await github(old.path, {
        method: "DELETE",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({
          message: `Remove expired backup ${old.name}`,
          sha: old.sha,
          branch
        })
      });
    }

    console.log(`Backup uploaded: ${path}; retained: ${Math.min(backups.length + 1, 7)}`);
    await notify("NCARD: резервная копия базы данных успешно создана. Хранится до 7 последних копий.");
  } finally {
    client.release();
    await pool.end();
  }
}

main().catch(async error => {
  console.error("NCARD backup failed:", error.message);
  await notify("NCARD: ошибка создания резервной копии. Проверьте логи Render.");
  await pool.end().catch(() => {});
  process.exitCode = 1;
});
