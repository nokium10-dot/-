const express = require("express");
const { Bot, InlineKeyboard } = require("grammy");

const required = ["TELEGRAM_BOT_TOKEN", "WEBHOOK_SECRET"];
for (const name of required) {
  if (!process.env[name]) {
    console.error(`Missing required environment variable: ${name}`);
    process.exit(1);
  }
}

const PORT = Number(process.env.PORT || 10000);
const WEBHOOK_SECRET = process.env.WEBHOOK_SECRET;
const OWNER_ID = 71195190;
const OWNER_USERNAME = "nokium";
const CARD_COOLDOWN_MS = 8 * 60 * 1000;

const RARITIES = {
  "Обычная": { weight: 60, value: 100 },
  "Необычная": { weight: 25, value: 300 },
  "Редкая": { weight: 10, value: 900 },
  "Эпическая": { weight: 4, value: 2500 },
  "Легендарная": { weight: 1, value: 8000 }
};

// Starter catalogue. The complete 110-card catalogue and verified original
// manufacturer images will be added in a later content pass.
const CARDS = [
  { id: "iphone-13", name: "iPhone 13", brand: "Apple", rarity: "Обычная", value: 100 },
  { id: "galaxy-a55", name: "Galaxy A55", brand: "Samsung", rarity: "Обычная", value: 100 },
  { id: "redmi-note-13", name: "Redmi Note 13", brand: "Xiaomi", rarity: "Обычная", value: 100 },
  { id: "iphone-14", name: "iPhone 14", brand: "Apple", rarity: "Необычная", value: 300 },
  { id: "pixel-8", name: "Pixel 8", brand: "Google", rarity: "Необычная", value: 300 },
  { id: "oneplus-12", name: "OnePlus 12", brand: "OnePlus", rarity: "Редкая", value: 900 },
  { id: "xiaomi-14-ultra", name: "Xiaomi 14 Ultra", brand: "Xiaomi", rarity: "Редкая", value: 900 },
  { id: "iphone-16-pro", name: "iPhone 16 Pro", brand: "Apple", rarity: "Эпическая", value: 2500 },
  { id: "galaxy-s25-ultra", name: "Galaxy S25 Ultra", brand: "Samsung", rarity: "Эпическая", value: 2500 },
  { id: "pixel-9-pro-xl", name: "Pixel 9 Pro XL", brand: "Google", rarity: "Легендарная", value: 8000 }
];

const bot = new Bot(process.env.TELEGRAM_BOT_TOKEN);
const app = express();
app.use(express.json({ limit: "1mb" }));

// Temporary in-memory state. A persistent database will be connected before
// production use so collections and cooldowns survive service restarts.
const users = new Map();
const cooldowns = new Map();

function getUser(from) {
  if (!users.has(from.id)) {
    users.set(from.id, {
      id: from.id,
      username: from.username || null,
      name: [from.first_name, from.last_name].filter(Boolean).join(" "),
      balance: 0,
      cards: [],
      achievements: [],
      views: 0,
      title: null,
      profileBackground: null
    });
  }
  const user = users.get(from.id);
  user.username = from.username || user.username;
  user.name = [from.first_name, from.last_name].filter(Boolean).join(" ") || user.name;
  return user;
}

function isOwner(ctx) {
  return Boolean(ctx.from && Number(ctx.from.id) === OWNER_ID);
}

function weightedCard() {
  const total = CARDS.reduce((sum, card) => sum + RARITIES[card.rarity].weight, 0);
  let roll = Math.random() * total;
  for (const card of CARDS) {
    roll -= RARITIES[card.rarity].weight;
    if (roll <= 0) return card;
  }
  return CARDS[0];
}

function remainingCooldown(userId) {
  const until = cooldowns.get(userId) || 0;
  return Math.max(0, until - Date.now());
}

function formatTime(ms) {
  const minutes = Math.floor(ms / 60000);
  const seconds = Math.ceil((ms % 60000) / 1000);
  return minutes ? `${minutes} мин. ${seconds} сек.` : `${seconds} сек.`;
}

bot.command("start", async (ctx) => {
  getUser(ctx.from);
  await ctx.reply(
    "Добро пожаловать в NCARD!\n\n" +
    "🎴 /ncard — получить карточку (кулдаун 8 минут)\n" +
    "👤 /nakk — профиль\n" +
    "🏆 /ntop — рейтинг этой группы\n" +
    "⚙️ /nsettings — настройки профиля"
  );
});

bot.command("ncard", async (ctx) => {
  const user = getUser(ctx.from);
  const wait = remainingCooldown(ctx.from.id);
  if (wait > 0) {
    await ctx.reply(`⏳ Следующую карточку можно получить через ${formatTime(wait)}.`);
    return;
  }

  const card = weightedCard();
  user.cards.push({ ...card, obtainedAt: Date.now() });
  cooldowns.set(ctx.from.id, Date.now() + CARD_COOLDOWN_MS);

  await ctx.reply(
    `🎴 **Новая карточка NCARD!**\n\n` +
    `📱 ${card.name}\n` +
    `🏭 Производитель: ${card.brand}\n` +
    `💠 Редкость: ${card.rarity}\n` +
    `💰 Стоимость: ${card.value.toLocaleString("ru-RU")} ₽\n\n` +
    `⏳ Следующая карточка через 8 минут.`,
    { parse_mode: "Markdown" }
  );
});

bot.command(["nakk", "накк"], async (ctx) => {
  const user = getUser(ctx.from);
  const totalValue = user.cards.reduce((sum, card) => sum + card.value, 0);
  const groupUsers = [...users.values()].sort((a, b) => b.cards.length - a.cards.length);
  const place = groupUsers.findIndex(item => item.id === user.id) + 1;
  await ctx.reply(
    `👤 Профиль ${user.name || "игрок"}\n\n` +
    `🏆 Место в локальном рейтинге: #${place}\n` +
    `💰 Баланс: ${user.balance.toLocaleString("ru-RU")} ₽\n` +
    `📱 Карточек: ${user.cards.length}\n` +
    `💎 Стоимость коллекции: ${totalValue.toLocaleString("ru-RU")} ₽\n` +
    `🎖 Достижений: ${user.achievements.length}`
  );
});

bot.command("ntop", async (ctx) => {
  const ranking = [...users.values()].sort((a, b) => b.cards.length - a.cards.length).slice(0, 10);
  if (!ranking.length) return ctx.reply("Пока рейтинг пуст.");
  const lines = ranking.map((u, i) => `${i + 1}. ${u.name || u.username || u.id} — ${u.cards.length} карточек`);
  await ctx.reply("🏆 Рейтинг NCARD\n\n" + lines.join("\n"));
});

bot.command("nsettings", async (ctx) => {
  getUser(ctx.from);
  await ctx.reply("⚙️ Настройки профиля\nТитулы и фоны появятся после добавления системы достижений.");
});

bot.command("nadmin", async (ctx) => {
  if (!isOwner(ctx)) {
    await ctx.reply("⛔ Доступ запрещён.");
    return;
  }
  if (ctx.chat.type !== "private") {
    await ctx.reply("Открой личный чат с ботом и введи /nadmin.");
    return;
  }
  const keyboard = new InlineKeyboard()
    .text("📊 Статистика", "admin:stats")
    .row()
    .text("📚 Каталог", "admin:catalog");
  await ctx.reply(
    `🔐 Панель владельца\nВладелец: @${OWNER_USERNAME}\nID: ${OWNER_ID}`,
    { reply_markup: keyboard }
  );
});

bot.callbackQuery(/^admin:/, async (ctx) => {
  if (!isOwner(ctx) || ctx.chat?.type !== "private") {
    await ctx.answerCallbackQuery({ text: "Доступ запрещён", show_alert: true });
    return;
  }
  const action = ctx.callbackQuery.data.split(":")[1];
  if (action === "stats") {
    await ctx.answerCallbackQuery();
    await ctx.reply(
      `📊 Статистика NCARD\nИгроков в памяти: ${users.size}\nКарточек в каталоге: ${CARDS.length}\nКулдаун: 8 минут`
    );
  } else if (action === "catalog") {
    await ctx.answerCallbackQuery();
    await ctx.reply("📚 В стартовом каталоге " + CARDS.length + " карточек. Полный каталог из 110 моделей будет добавлен после проверки моделей и официальных изображений.");
  }
});

app.get("/", (_req, res) => res.status(200).send("NCARD Telegram bot is running."));
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
  console.log(`NCARD HTTP server listening on ${PORT}`);
  const publicUrl = process.env.RENDER_EXTERNAL_URL;
  if (!publicUrl) {
    console.error("RENDER_EXTERNAL_URL is missing; webhook was not configured.");
    return;
  }
  try {
    await bot.init();
    console.log(`NCARD bot initialized: @${bot.botInfo.username}`);
    await bot.api.setWebhook(`${publicUrl}/telegram/${WEBHOOK_SECRET}`, {
      secret_token: WEBHOOK_SECRET,
      allowed_updates: ["message", "callback_query"]
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
