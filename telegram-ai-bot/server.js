const express = require("express");
const { Bot, InlineKeyboard } = require("grammy");
const store = require("./storage");

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
  "Обычная": { weight: 60, color: "⚪" },
  "Необычная": { weight: 25, color: "🟢" },
  "Редкая": { weight: 10, color: "🔵" },
  "Эпическая": { weight: 4, color: "🟣" },
  "Легендарная": { weight: 1, color: "🟡" },
  "Мифическая": { weight: 0.5, color: "🔴" },
  "Древняя": { weight: 0.25, color: "🩵" },
  "Божественная": { weight: 0.12, color: "✨" },
  "Секретная": { weight: 0.06, color: "⚫" },
  "Эксклюзив": { weight: 0.03, color: "💗" }
};

// Temporary starter set. Only verified models are listed here; official
// manufacturer images are intentionally not fabricated or AI-generated.
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

function formatTime(ms) {
  const minutes = Math.floor(ms / 60000);
  const seconds = Math.ceil((ms % 60000) / 1000);
  return minutes ? `${minutes} мин. ${seconds} сек.` : `${seconds} сек.`;
}

// Register users and group membership on every incoming message.
bot.use(async (ctx, next) => {
  if (ctx.from) {
    await store.upsertUser(ctx.from);
    if (ctx.chat && ctx.chat.type !== "private") {
      await store.recordGroupMember(ctx.chat.id, ctx.from.id);
    }
  }
  await next();
});

bot.command("start", async (ctx) => {
  await ctx.reply(
    "Добро пожаловать в NCARD!\n\n" +
    "🎴 /ncard — получить карточку (кулдаун 8 минут)\n" +
    "👤 /nakk — профиль\n" +
    "🏆 /ntop — рейтинг этой группы\n" +
    "⚙️ /nsettings — настройки профиля"
  );
});

bot.command("ncard", async (ctx) => {
  const userId = ctx.from.id;
  const now = Date.now();
  const wait = Math.max(0, await store.getCooldown(userId) - now);
  if (wait > 0) {
    await ctx.reply(`⏳ Следующую карточку можно получить через ${formatTime(wait)}.`);
    return;
  }

  const card = weightedCard();
  await store.addCard(userId, card);
  await store.setCooldown(userId, now + CARD_COOLDOWN_MS);
  const rarityColor = RARITIES[card.rarity]?.color || "💠";
  await ctx.reply(
    `🎴 **Новая карточка NCARD!**\n\n` +
    `📱 ${card.name}\n` +
    `🏭 Производитель: ${card.brand}\n` +
    `${rarityColor} Редкость: ${card.rarity}\n` +
    `💰 Стоимость: ${card.value.toLocaleString("ru-RU")} ₽\n\n` +
    `⏳ Следующая карточка через 8 минут.`,
    { parse_mode: "Markdown" }
  );
});

bot.command(["nakk", "накк"], async (ctx) => {
  const collection = await store.getCollection(ctx.from.id);
  const totalValue = collection.reduce((sum, card) => sum + Number(card.value), 0);
  const ranking = ctx.chat.type === "private"
    ? []
    : await store.getGroupRanking(ctx.chat.id, 1000);
  const place = ranking.findIndex(item => item.id === ctx.from.id) + 1;
  const placeText = ctx.chat.type === "private" ? "открой команду в группе" : (place ? `#${place}` : "—");
  const name = [ctx.from.first_name, ctx.from.last_name].filter(Boolean).join(" ") || "игрок";
  await ctx.reply(
    `👤 Профиль ${name}\n\n` +
    `🏆 Место в рейтинге группы: ${placeText}\n` +
    `💰 Баланс: 0 ₽\n` +
    `📱 Карточек: ${collection.length}\n` +
    `💎 Стоимость коллекции: ${totalValue.toLocaleString("ru-RU")} ₽\n` +
    `🎖 Достижений: 0`
  );
});

bot.command("ntop", async (ctx) => {
  if (ctx.chat.type === "private") {
    await ctx.reply("Команда /ntop работает в группе — там показывается рейтинг её участников.");
    return;
  }
  const ranking = await store.getGroupRanking(ctx.chat.id, 10);
  if (!ranking.length) return ctx.reply("Пока рейтинг пуст.");
  const lines = ranking.map((u, i) =>
    `${i + 1}. ${u.name || (u.username ? "@" + u.username : u.id)} — ${u.count} карточек`
  );
  await ctx.reply("🏆 Рейтинг NCARD этой группы\n\n" + lines.join("\n"));
});

bot.command("nsettings", async (ctx) => {
  await ctx.reply("⚙️ Настройки профиля\nСистема титулов и фонов будет подключена вместе с достижениями.");
});

bot.command("nadmin", async (ctx) => {
  if (!isOwner(ctx)) return ctx.reply("⛔ Доступ запрещён.");
  if (ctx.chat.type !== "private") return ctx.reply("Открой личный чат с ботом и введи /nadmin.");
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
    const stats = await store.getStats();
    await ctx.answerCallbackQuery();
    await ctx.reply(
      `📊 Статистика NCARD\nИгроков: ${stats.users}\nВыдано карточек: ${stats.cards}\nКарточек в каталоге: ${CARDS.length}\nХранилище: ${store.persistent ? "PostgreSQL" : "временная память"}`
    );
  } else if (action === "catalog") {
    await ctx.answerCallbackQuery();
    await ctx.reply(
      `📚 В каталоге ${CARDS.length} стартовых карточек. Полный каталог из 110 моделей будет добавлен после проверки точных моделей и официальных изображений.`
    );
  }
});

app.get("/", (_req, res) => res.status(200).send("NCARD Telegram bot is running."));
app.get("/health", (_req, res) => res.json({ ok: true, storage: store.persistent ? "postgres" : "memory" }));

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

let server;
async function start() {
  await store.init();
  server = app.listen(PORT, async () => {
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
}

async function shutdown() {
  try { await bot.api.deleteWebhook(); } catch (_) {}
  if (server) server.close();
  await store.close();
  process.exit(0);
}

process.on("SIGTERM", shutdown);
process.on("SIGINT", shutdown);
start().catch(error => {
  console.error("NCARD startup failed:", error);
  process.exit(1);
});
