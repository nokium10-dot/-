const express = require("express");
const { Bot, InputFile } = require("grammy");

const required = ["TELEGRAM_BOT_TOKEN", "OPENAI_API_KEY", "WEBHOOK_SECRET"];
for (const name of required) {
  if (!process.env[name]) {
    console.error(`Missing required environment variable: ${name}`);
    process.exit(1);
  }
}

const PORT = Number(process.env.PORT || 10000);
const BASE_URL = (process.env.AI_BASE_URL || "https://api.openai.com/v1").replace(/\/$/, "");
const MODEL = process.env.AI_MODEL || "gpt-4o-mini";
const OWNER_ID = process.env.BOT_OWNER_ID ? Number(process.env.BOT_OWNER_ID) : null;
const WEBHOOK_SECRET = process.env.WEBHOOK_SECRET;
const APK_DOWNLOAD_URL = "https://github.com/nokium10-dot/-/releases/download/matrixwall-latest/MatrixWall-Pro.apk";
const bot = new Bot(process.env.TELEGRAM_BOT_TOKEN);
const app = express();

app.use(express.json({ limit: "1mb" }));

// Each member gets a separate conversation, even inside a shared group.
const histories = new Map();
const disabledChats = new Set();
const MAX_MESSAGES = 12;

function historyKey(ctx) {
  return `${ctx.chat.id}:${ctx.from.id}`;
}

function splitMessage(text, limit = 3900) {
  const parts = [];
  let remaining = text;
  while (remaining.length > limit) {
    let cut = remaining.lastIndexOf("\n", limit);
    if (cut < limit * 0.5) cut = limit;
    parts.push(remaining.slice(0, cut));
    remaining = remaining.slice(cut).trimStart();
  }
  if (remaining) parts.push(remaining);
  return parts;
}

async function sendLatestApk(ctx) {
  await ctx.replyWithChatAction("upload_document");

  // Download directly from the stable GitHub Release asset URL.
  // This avoids the unauthenticated GitHub Releases API and its rate limits.
  const apkResponse = await fetch(APK_DOWNLOAD_URL, {
    headers: { "User-Agent": "MatrixWall-Telegram-Bot" },
    redirect: "follow"
  });

  if (apkResponse.status === 404) {
    await ctx.reply("Пока нет опубликованной сборки MatrixWall Pro. Попробуй ещё раз после завершения ближайшей сборки.");
    return;
  }
  if (!apkResponse.ok) {
    throw new Error(`APK download failed: HTTP ${apkResponse.status}`);
  }

  const apkBuffer = Buffer.from(await apkResponse.arrayBuffer());
  if (apkBuffer.length < 1024) {
    throw new Error("Downloaded APK is unexpectedly small");
  }

  await ctx.replyWithDocument(
    new InputFile(apkBuffer, "MatrixWall-Pro.apk"),
    {
      caption: `MatrixWall Pro — последняя опубликованная версия\nРазмер: ${(apkBuffer.length / 1024 / 1024).toFixed(1)} МБ`,
      reply_parameters: { message_id: ctx.message.message_id }
    }
  );
}

async function askAI(messages) {
  const response = await fetch(`${BASE_URL}/chat/completions`, {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${process.env.OPENAI_API_KEY}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      model: MODEL,
      messages,
      temperature: 0.7,
      max_tokens: 700
    })
  });

  if (!response.ok) {
    const detail = await response.text();
    console.error("AI provider error:", response.status, detail.slice(0, 500));
    throw new Error("AI provider request failed");
  }

  const data = await response.json();
  const answer = data.choices?.[0]?.message?.content;
  if (!answer) throw new Error("AI provider returned an empty response");
  return answer.trim();
}

bot.command("start", async (ctx) => {
  await ctx.reply(
    "Привет! Я ИИ-помощник группы. Напиши /ask и свой вопрос. Команда /download отправит последнюю версию MatrixWall Pro, а /clear очистит только твою историю диалога."
  );
});

// Download the latest APK published by the GitHub Actions build.
bot.command(["download", "apk"], async (ctx) => {
  try {
    await sendLatestApk(ctx);
  } catch (error) {
    console.error("APK delivery failed:", error.message);
    await ctx.reply("Не удалось получить APK. Попробуй немного позже.");
  }
});

bot.command("ask", async (ctx) => {
  if (disabledChats.has(ctx.chat.id)) {
    await ctx.reply("ИИ-ответы сейчас отключены администратором.");
    return;
  }

  const prompt = ctx.match?.trim();
  if (!prompt) {
    await ctx.reply("Напиши вопрос после команды, например: /ask Как работает MatrixWall Pro?");
    return;
  }

  const key = historyKey(ctx);
  const previous = histories.get(key) || [];
  const messages = [
    {
      role: "system",
      content: "Ты полезный и безопасный ИИ-помощник Telegram-группы. Отвечай по-русски, если пользователь не просит другой язык. Пиши ясно, спокойно и без ненормативной лексики. Не выдумывай факты."
    },
    ...previous,
    { role: "user", content: prompt }
  ];

  try {
    await ctx.replyWithChatAction("typing");
    const answer = await askAI(messages);
    const updated = [...previous, { role: "user", content: prompt }, { role: "assistant", content: answer }];
    histories.set(key, updated.slice(-MAX_MESSAGES));
    for (const part of splitMessage(answer)) {
      await ctx.reply(part, { reply_parameters: { message_id: ctx.message.message_id } });
    }
  } catch (error) {
    console.error("AI reply failed:", error.message);
    await ctx.reply("Не удалось получить ответ от ИИ. Попробуй немного позже.");
  }
});

bot.command("clear", async (ctx) => {
  histories.delete(historyKey(ctx));
  await ctx.reply("Твоя история диалога очищена.");
});

bot.command("ai_off", async (ctx) => {
  const isOwner = OWNER_ID && ctx.from.id === OWNER_ID;
  const member = ctx.chat.type !== "private" ? await ctx.getChatMember(ctx.from.id).catch(() => null) : null;
  const isAdmin = member && ["administrator", "creator"].includes(member.status);
  if (!isOwner && !isAdmin) {
    await ctx.reply("Эта команда доступна только администратору группы.");
    return;
  }
  disabledChats.add(ctx.chat.id);
  await ctx.reply("ИИ-ответы отключены в этой группе.");
});

bot.command("ai_on", async (ctx) => {
  const isOwner = OWNER_ID && ctx.from.id === OWNER_ID;
  const member = ctx.chat.type !== "private" ? await ctx.getChatMember(ctx.from.id).catch(() => null) : null;
  const isAdmin = member && ["administrator", "creator"].includes(member.status);
  if (!isOwner && !isAdmin) {
    await ctx.reply("Эта команда доступна только администратору группы.");
    return;
  }
  disabledChats.delete(ctx.chat.id);
  await ctx.reply("ИИ-ответы включены в этой группе.");
});

app.get("/", (_req, res) => res.status(200).send("MatrixWall Telegram AI bot is running."));
app.get("/health", (_req, res) => res.json({ ok: true }));

app.post(`/telegram/${WEBHOOK_SECRET}`, async (req, res) => {
  if (req.get("X-Telegram-Bot-Api-Secret-Token") !== WEBHOOK_SECRET) {
    return res.sendStatus(401);
  }
  res.sendStatus(200);
  try {
    await bot.handleUpdate(req.body);
  } catch (error) {
    console.error("Telegram update failed:", error);
  }
});

const server = app.listen(PORT, async () => {
  console.log(`HTTP server listening on ${PORT}`);
  const publicUrl = process.env.RENDER_EXTERNAL_URL;
  if (!publicUrl) {
    console.error("RENDER_EXTERNAL_URL is missing; webhook was not configured.");
    return;
  }
  try {
    await bot.init();
    console.log(`Telegram bot initialized: @${bot.botInfo.username}`);
    await bot.api.setWebhook(`${publicUrl}/telegram/${WEBHOOK_SECRET}`, {
      secret_token: WEBHOOK_SECRET,
      allowed_updates: ["message"]
    });
    console.log("Telegram webhook configured.");
  } catch (error) {
    console.error("Failed to configure Telegram webhook:", error.message);
  }
});

async function shutdown() {
  try { await bot.api.deleteWebhook(); } catch (_) {}
  server.close(() => process.exit(0));
}
process.on("SIGTERM", shutdown);
process.on("SIGINT", shutdown);
