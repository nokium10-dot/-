const express = require("express");
const { Bot, InlineKeyboard } = require("grammy");
const os = require("os");
const store = require("./storage");

for (const name of ["TELEGRAM_BOT_TOKEN", "WEBHOOK_SECRET"]) {
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

const CARDS = require("./catalog");

const ACHIEVEMENTS = {
  first_card: {
    name: "Первый экземпляр", description: "Получи первую карточку",
    reward: 100, title: "Новичок"
  },
  collector_10: {
    name: "Начинающий коллекционер", description: "Собери 10 карточек",
    reward: 500, background: "Техно-сетка"
  },
  collector_50: {
    name: "Большая коллекция", description: "Собери 50 карточек",
    reward: 1500, title: "Коллекционер"
  },
  legendary: {
    name: "Легендарная находка", description: "Получи легендарную карточку или выше",
    reward: 1000, background: "Золотой контур"
  },
  exclusive: {
    name: "За гранью редкости", description: "Получи эксклюзивную карточку",
    reward: 5000, title: "Охотник за редкостями"
  }
};

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

function money(value) {
  return `${Number(value || 0).toLocaleString("ru-RU")} ₽`;
}

async function checkAchievements(ctx, collection, card) {
  const userId = ctx.from.id;
  const ids = [];
  if (collection.length >= 1) ids.push("first_card");
  if (collection.length >= 10) ids.push("collector_10");
  if (collection.length >= 50) ids.push("collector_50");
  const rarityOrder = Object.keys(RARITIES);
  if (rarityOrder.indexOf(card.rarity) >= rarityOrder.indexOf("Легендарная")) ids.push("legendary");
  if (card.rarity === "Эксклюзив") ids.push("exclusive");

  for (const id of ids) {
    const a = ACHIEVEMENTS[id];
    const added = await store.awardAchievement(userId, id, a.reward);
    if (added) {
      await ctx.reply(
        `🏅 Достижение получено: ${a.name}\n${a.description}\nНаграда: +${money(a.reward)}` +
        (a.title ? `\nОткрыт титул: «${a.title}»` : "") +
        (a.background ? `\nОткрыт фон: «${a.background}»` : "")
      );
    }
  }
}

async function sendProfile(ctx, userId, chatId) {
  const user = await store.getUser(userId);
  const collection = await store.getCollection(userId);
  const totalValue = collection.reduce((sum, card) => sum + Number(card.value), 0);
  const ranking = chatId ? await store.getGroupRanking(chatId, 1000) : [];
  const place = ranking.findIndex(item => item.id === userId) + 1;
  const placeText = chatId ? (place ? `#${place}` : "—") : "открой профиль в группе";
  const name = user?.name || ctx.from.first_name || "Игрок";
  const username = user?.username ? `@ ${user.username}`.replace("@ ", "@") : "не указан";
  const achievementNames = (user?.achievements || []).map(id => ACHIEVEMENTS[id]?.name).filter(Boolean);
  const text =
    `👤 Профиль ${name}\n` +
    `🔗 ${username}\n\n` +
    `🏆 Место в группе: ${placeText}\n` +
    `💰 Баланс: ${money(user?.balance)}\n` +
    `📱 Карточек: ${collection.length}\n` +
    `💎 Стоимость коллекции: ${money(totalValue)}\n` +
    `🏅 Достижений: ${achievementNames.length}/${Object.keys(ACHIEVEMENTS).length}\n` +
    `🎖 Титул: ${user?.title || "не выбран"}\n` +
    `🖼 Фон: ${user?.profileBackground || "стандартный"}`;
  try {
    const photos = await ctx.api.getUserProfilePhotos(userId, { limit: 1 });
    const sizes = photos.photos?.[0] || [];
    const avatar = sizes[sizes.length - 1];
    if (avatar?.file_id) {
      await ctx.replyWithPhoto(avatar.file_id, { caption: text });
      return;
    }
  } catch (error) {
    console.warn("Could not load profile avatar:", error.message);
  }
  await ctx.reply(text);
}

bot.use(async (ctx, next) => {
  if (ctx.from) {
    await store.upsertUser(ctx.from);
    if (!isOwner(ctx) && await store.isBlocked(ctx.from.id)) {
      if (ctx.chat?.type === "private" && ctx.message) {
        await ctx.reply("⛔ Доступ к боту ограничен владельцем.");
      }
      return;
    }
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
    "📚 /ncollection — моя коллекция\n" +
    "🏅 /nachievements — достижения\n" +
    "⚒️ /nallup — массовое улучшение карточек\n" +
    "🏆 /ntop — рейтинг этой группы\n" +
    "⚙️ /nsettings — титулы и фоны профиля\n" +
    "📨 Поддержка — просто напиши сообщение боту.",
    { reply_markup: new InlineKeyboard().url("TikTok", "https://www.tiktok.com/pirozhki380") }
  );
});

bot.command("ncard", async (ctx) => {
  const now = Date.now();
  const claim = await store.claimCooldown(ctx.from.id, now, CARD_COOLDOWN_MS);
  if (!claim.ok) {
    await ctx.reply(`⏳ Следующую карточку можно получить через ${formatTime(Math.max(0, claim.until - now))}.`);
    return;
  }
  try {
    const card = weightedCard();
    try {
      await store.addCard(ctx.from.id, card);
    } catch (error) {
      await store.setCooldown(ctx.from.id, now);
      throw error;
    }
    const collection = await store.getCollection(ctx.from.id);
    const rarityColor = RARITIES[card.rarity]?.color || "💠";
    const cardCaption =
      `🎴 **Новая карточка NCARD!**\\n\\n` +
      `📱 ${card.name}\\n🏭 Производитель: ${card.brand}\\n` +
      `${rarityColor} Редкость: ${card.rarity}\\n💰 Стоимость: ${money(card.value)}\\n\\n` +
      "⏳ Следующая карточка через 8 минут.";
    const storedPhoto = await store.getCardPhoto(card.id);
    if (storedPhoto) {
      await ctx.replyWithPhoto(storedPhoto, { caption: cardCaption, parse_mode: "Markdown" });
    } else if (card.imageUrl) {
      await ctx.replyWithPhoto(card.imageUrl, { caption: cardCaption, parse_mode: "Markdown" });
    } else {
      await ctx.reply(cardCaption, { parse_mode: "Markdown" });
    }
    await checkAchievements(ctx, collection, card);
  } catch (error) {
    throw error;
  }
});

bot.command(["nakk", "накк"], async (ctx) => {
  await sendProfile(ctx, ctx.from.id, ctx.chat.type === "private" ? null : ctx.chat.id);
});
bot.hears(/^накк$/i, async (ctx) => {
  if (!ctx.from) return;
  await sendProfile(ctx, ctx.from.id, ctx.chat.type === "private" ? null : ctx.chat.id);
});

bot.command("ncollection", async (ctx) => {
  const cards = await store.getCollection(ctx.from.id);
  if (!cards.length) return ctx.reply("Коллекция пока пуста. Получи первую карточку командой /ncard.");
  const counts = new Map();
  for (const card of cards) counts.set(card.id, (counts.get(card.id) || 0) + 1);
  const lines = [...counts.entries()].map(([id, count]) => {
    const card = cards.find(item => item.id === id);
    return `• ${card.name} — ${card.rarity} ×${count}`;
  });
  await ctx.reply(`📚 Твоя коллекция (${cards.length} карточек)\n\n${lines.slice(0, 40).join("\n")}${lines.length > 40 ? "\n…и другие." : ""}`);
});

const RARITY_VALUES = {
  "Обычная": 100, "Необычная": 300, "Редкая": 900,
  "Эпическая": 2500, "Легендарная": 8000, "Мифическая": 20000,
  "Древняя": 50000, "Божественная": 100000, "Секретная": 250000,
  "Эксклюзив": 1000000
};

bot.command("nallup", async (ctx) => {
  const cards = await store.getCollection(ctx.from.id);
  const counts = new Map();
  for (const card of cards) counts.set(card.rarity, (counts.get(card.rarity) || 0) + 1);
  const keyboard = new InlineKeyboard();
  Object.keys(RARITIES).forEach((rarity, index) => {
    const count = counts.get(rarity) || 0;
    if (count > 0 && index < Object.keys(RARITIES).length - 1) {
      keyboard.text(`${RARITIES[rarity].color} ${rarity} ×${count}`, `upgrade:select:${index}`).row();
    }
  });
  if (!keyboard.inline_keyboard.length) {
    await ctx.reply("У тебя пока нет карточек, которые можно улучшить.");
    return;
  }
  await ctx.reply("⚒️ Выбери редкость. Будут обработаны ВСЕ карточки этой редкости. Успех — 50%, при неудаче карточка теряется.", { reply_markup: keyboard });
});

bot.callbackQuery(/^upgrade:select:(\d+)$/, async (ctx) => {
  const index = Number(ctx.match[1]);
  const rarities = Object.keys(RARITIES);
  const rarity = rarities[index];
  const cards = await store.getCollection(ctx.from.id);
  const count = cards.filter(card => card.rarity === rarity).length;
  if (!rarity || !count || index >= rarities.length - 1) {
    await ctx.answerCallbackQuery({ text: "Эти карточки уже недоступны для улучшения.", show_alert: true });
    return;
  }
  const next = rarities[index + 1];
  const keyboard = new InlineKeyboard()
    .text("✅ Подтвердить", `upgrade:confirm:${index}`)
    .text("❌ Отмена", "upgrade:cancel");
  await ctx.answerCallbackQuery();
  await ctx.reply(
    `⚠️ Улучшить все карточки редкости «${rarity}»?\nКоличество: ${count}\n\n` +
    `При успехе (50%) карточка станет «${next}» и её стоимость будет ${money(RARITY_VALUES[next])}.\n` +
    "При неудаче карточка будет потеряна. Отменить операцию после подтверждения нельзя.",
    { reply_markup: keyboard }
  );
});

bot.callbackQuery("upgrade:cancel", async (ctx) => {
  await ctx.answerCallbackQuery({ text: "Улучшение отменено." });
  await ctx.editMessageText("❌ Массовое улучшение отменено.");
});

bot.callbackQuery(/^upgrade:confirm:(\d+)$/, async (ctx) => {
  const index = Number(ctx.match[1]);
  const rarities = Object.keys(RARITIES);
  const rarity = rarities[index];
  const next = rarities[index + 1];
  if (!rarity || !next) {
    await ctx.answerCallbackQuery({ text: "Неверная редкость.", show_alert: true });
    return;
  }
  await ctx.answerCallbackQuery({ text: "Начинаю улучшение..." });
  const results = await store.massUpgrade(ctx.from.id, rarity, next, RARITY_VALUES[next], 0.5);
  if (!results.length) {
    await ctx.editMessageText("Карточек выбранной редкости больше нет.");
    return;
  }
  const lines = results.map(item =>
    `${item.success ? "✅" : "❌"} ${item.name}${item.success ? ` → ${item.rarity}` : " — карточка потеряна"}`
  );
  const successes = results.filter(item => item.success).length;
  await ctx.editMessageText(
    `⚒️ Улучшение завершено\nУспешно: ${successes}\nПотеряно: ${results.length - successes}\n\n${lines.join("\n")}`
  );
});

bot.command("nachievements", async (ctx) => {
  const user = await store.getUser(ctx.from.id);
  const owned = new Set(user?.achievements || []);
  const lines = Object.entries(ACHIEVEMENTS).map(([id, a]) =>
    `${owned.has(id) ? "✅" : "🔒"} ${a.name} — ${a.description} (+${money(a.reward)})`
  );
  await ctx.reply("🏅 Достижения NCARD\n\n" + lines.join("\n"));
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

function settingsKeyboard(user) {
  const keyboard = new InlineKeyboard();
  keyboard.text(`🎖 Титул: ${user.title || "нет"}`, "settings:titles").row();
  keyboard.text(`🖼 Фон: ${user.profileBackground || "стандартный"}`, "settings:backgrounds");
  return keyboard;
}

bot.command("nsettings", async (ctx) => {
  const user = await store.getUser(ctx.from.id);
  await ctx.reply("⚙️ Настройки профиля. Здесь можно выбрать только открытые достижения.", {
    reply_markup: settingsKeyboard(user)
  });
});

bot.callbackQuery(/^settings:(titles|backgrounds)$/, async (ctx) => {
  const user = await store.getUser(ctx.from.id);
  const type = ctx.match[1];
  const owned = new Set(user?.achievements || []);
  const keyboard = new InlineKeyboard();
  if (type === "titles") {
    keyboard.text("Без титула", "equip:title:none").row();
    for (const [id, a] of Object.entries(ACHIEVEMENTS)) {
      if (a.title && owned.has(id)) keyboard.text(a.title, `equip:title:${id}`).row();
    }
  } else {
    keyboard.text("Стандартный фон", "equip:background:none").row();
    for (const [id, a] of Object.entries(ACHIEVEMENTS)) {
      if (a.background && owned.has(id)) keyboard.text(a.background, `equip:background:${id}`).row();
    }
  }
  await ctx.answerCallbackQuery();
  await ctx.reply(type === "titles" ? "Выбери открытый титул:" : "Выбери открытый фон:", { reply_markup: keyboard });
});

bot.callbackQuery(/^equip:(title|background):(none|[a-z0-9_]+)$/, async (ctx) => {
  const [, type, id] = ctx.match;
  const field = type === "title" ? "title" : "profileBackground";
  const achievement = id === "none" ? null : ACHIEVEMENTS[id];
  const user = await store.getUser(ctx.from.id);
  if (id !== "none") {
    const unlocked = (user?.achievements || []).includes(id);
    const item = achievement?.[type];
    if (!unlocked || !item) {
      await ctx.answerCallbackQuery({ text: "Этот элемент ещё не открыт.", show_alert: true });
      return;
    }
    await store.setProfileField(ctx.from.id, field, item);
  } else {
    await store.setProfileField(ctx.from.id, field, null);
  }
  await ctx.answerCallbackQuery({ text: "Профиль обновлён." });
  const updated = await store.getUser(ctx.from.id);
  await ctx.reply("Настройки сохранены.", { reply_markup: settingsKeyboard(updated) });
});

bot.command("nadmin", async (ctx) => {
  if (!isOwner(ctx)) return ctx.reply("⛔ Доступ запрещён.");
  if (ctx.chat.type !== "private") return ctx.reply("Открой личный чат с ботом и введи /nadmin.");
  const keyboard = new InlineKeyboard()
    .text("📊 Статистика", "admin:stats")
    .text("📚 Каталог", "admin:catalog")
    .row()
    .text("🖥 Нагрузка сервера", "admin:system")
    .text("🌐 Интернет", "admin:internet")
    .row()
    .text("🚫 Блокировка", "admin:blockhelp");
  await ctx.reply(`🔐 Панель владельца\nВладелец: @${OWNER_USERNAME}\nID: ${OWNER_ID}`, { reply_markup: keyboard });
});

bot.callbackQuery(/^admin:(stats|catalog|system|internet|blockhelp)$/, async (ctx) => {
  if (!isOwner(ctx) || ctx.chat?.type !== "private") {
    await ctx.answerCallbackQuery({ text: "Доступ запрещён", show_alert: true });
    return;
  }
  const action = ctx.match[1];
  await ctx.answerCallbackQuery();
  if (action === "stats") {
    const stats = await store.getStats();
    await ctx.reply(
      `📊 Статистика NCARD\nИгроков: ${stats.users}\nВыдано карточек: ${stats.cards}\nСуммарный баланс: ${money(stats.balance)}\nКарточек в каталоге: ${CARDS.length}\nХранилище: ${store.persistent ? "PostgreSQL" : "временная память"}`
    );
  } else if (action === "catalog") {
    await ctx.reply(`📚 В каталоге ${CARDS.length} моделей. Официальные фото точных моделей ещё не подключены; для них нужны реальные фото или рендеры производителя.`);
  } else if (action === "system") {
    const cpus = os.cpus();
    const total = os.totalmem();
    const free = os.freemem();
    const load = os.loadavg().map(v => v.toFixed(2)).join(" / ");
    await ctx.reply(`🖥 Нагрузка хоста\nCPU: ${cpus.length} ядер (${cpus[0]?.model || "неизвестно"})\nLoad average (1/5/15 мин): ${load}\nRAM: ${((total-free)/1024/1024/1024).toFixed(2)} / ${(total/1024/1024/1024).toFixed(2)} GB\nПлатформа: ${os.platform()} ${os.arch()}`);
  } else if (action === "internet") {
    const started = Date.now();
    try {
      const response = await fetch("https://api.telegram.org", { signal: AbortSignal.timeout(5000) });
      await ctx.reply(`🌐 Интернет доступен. Telegram API ответил HTTP ${response.status}; задержка ${Date.now()-started} мс.`);
    } catch (error) {
      await ctx.reply(`⚠️ Проверка интернета не прошла: ${error.message}`);
    }
  } else {
    await ctx.reply("🚫 Управление блокировками:\n/nblock ID — заблокировать пользователя\n/nunblock ID — снять блокировку\n/nreply ID текст — ответить пользователю через бота.");
  }
});

bot.command("nsetphoto", async (ctx) => {
  if (!isOwner(ctx) || ctx.chat.type !== "private") return ctx.reply("⛔ Доступ запрещён.");
  const cardKey = String(ctx.match || "").trim().split(/\s+/)[0];
  const card = CARDS.find(item => item.id === cardKey);
  const photo = ctx.message?.photo?.at(-1);
  if (!card || !photo) {
    return ctx.reply("Пришли официальное фото телефона с подписью /nsetphoto ID карточки. ID можно посмотреть в каталоге проекта.");
  }
  await store.setCardPhoto(cardKey, photo.file_id);
  await ctx.reply(`Фото для «${card.name}» сохранено. Используется оригинальный файл Telegram без изменения телефона.`);
});

bot.command("nclearphoto", async (ctx) => {
  if (!isOwner(ctx) || ctx.chat.type !== "private") return ctx.reply("⛔ Доступ запрещён.");
  const cardKey = String(ctx.match || "").trim().split(/\s+/)[0];
  if (!CARDS.some(item => item.id === cardKey)) return ctx.reply("Неизвестный ID карточки.");
  await store.clearCardPhoto(cardKey);
  await ctx.reply(`Фото карточки ${cardKey} удалено из настроек.`);
});

bot.command("nblock", async (ctx) => {
  if (!isOwner(ctx) || ctx.chat.type !== "private") return ctx.reply("⛔ Доступ запрещён.");
  const id = String(ctx.match || "").trim().split(/\s+/)[0];
  if (!/^\d+$/.test(id) || Number(id) === OWNER_ID) return ctx.reply("Формат: /nblock ID");
  await store.setBlocked(id, true, "Заблокирован владельцем");
  await ctx.reply(`Пользователь ${id} заблокирован.`);
});

bot.command("nunblock", async (ctx) => {
  if (!isOwner(ctx) || ctx.chat.type !== "private") return ctx.reply("⛔ Доступ запрещён.");
  const id = String(ctx.match || "").trim().split(/\s+/)[0];
  if (!/^\d+$/.test(id)) return ctx.reply("Формат: /nunblock ID");
  await store.setBlocked(id, false);
  await ctx.reply(`Блокировка пользователя ${id} снята.`);
});

bot.command("nreply", async (ctx) => {
  if (!isOwner(ctx) || ctx.chat.type !== "private") return ctx.reply("⛔ Доступ запрещён.");
  const match = String(ctx.match || "").match(/^(\d+)\s+([\s\S]+)/);
  if (!match) return ctx.reply("Формат: /nreply ID текст ответа");
  await ctx.api.sendMessage(Number(match[1]), match[2]);
  await ctx.reply("Ответ отправлен.");
});


bot.on("message", async (ctx) => {
  if (!ctx.from || !ctx.chat || ctx.chat.type !== "private" || isOwner(ctx)) return;
  try {
    await ctx.api.sendMessage(
      OWNER_ID,
      `📩 Сообщение в поддержку\nОт: ${ctx.from.first_name || "пользователь"} ${ctx.from.username ? "(@" + ctx.from.username + ")" : ""}\nID: ${ctx.from.id}\nОтвет: /nreply ${ctx.from.id} текст`
    );
    await ctx.api.forwardMessage(OWNER_ID, ctx.chat.id, ctx.message.message_id);
    await ctx.reply("Сообщение передано владельцу бота.");
  } catch (error) {
    console.error("Support forwarding failed:", error.message);
    await ctx.reply("Не удалось передать сообщение. Попробуй позже.");
  }
});

bot.catch((error) => {
  console.error("NCARD Telegram handler error:", error);
});

app.get("/", (_req, res) => res.status(200).send("NCARD Telegram bot is running."));
app.get("/health", (_req, res) => res.json({ ok: true, storage: store.persistent ? "postgres" : "memory" }));

app.post(`/telegram/${WEBHOOK_SECRET}`, async (req, res) => {
  if (req.get("X-Telegram-Bot-Api-Secret-Token") !== WEBHOOK_SECRET) return res.sendStatus(401);
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
