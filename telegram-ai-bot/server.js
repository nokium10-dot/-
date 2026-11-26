const express = require("express");
const { Bot, InlineKeyboard, InputFile } = require("grammy");
const path = require("path");
const yauzl = require("yauzl");
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
const OWNER_ID = 7119519072;
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
  return `${Number(value || 0).toLocaleString("ru-RU")} AM-очков`;
}

async function sendCardPhoto(ctx, card, options = {}) {
  const savedFileId = await store.getCardPhoto(card.id);
  if (savedFileId) {
    return ctx.replyWithPhoto(savedFileId, options);
  }
  if (!card.imageUrl) return null;

  try {
    // Telegram fetches the image and sends it as a native photo message.
    // Save the returned file_id so future sends no longer depend on the source URL.
    const sent = await ctx.replyWithPhoto(card.imageUrl, options);
    const uploadedPhoto = sent.photo?.at(-1);
    if (uploadedPhoto?.file_id) {
      await store.setCardPhoto(card.id, uploadedPhoto.file_id);
    }
    return sent;
  } catch (error) {
    console.warn(`Could not send card photo for ${card.id}:`, error.message);
    return null;
  }
}


function extractMp4Entries(zipBuffer) {
  return new Promise((resolve, reject) => {
    yauzl.fromBuffer(zipBuffer, { lazyEntries: true, decodeStrings: true }, (error, zip) => {
      if (error) return reject(error);
      const entries = [];
      let totalBytes = 0;
      zip.on("error", reject);
      zip.on("entry", (entry) => {
        const filename = path.basename(entry.fileName);
        if (entry.fileName.endsWith("/") || !/\.mp4$/i.test(filename)) {
          zip.readEntry();
          return;
        }
        if (entry.uncompressedSize > 10 * 1024 * 1024 || totalBytes + entry.uncompressedSize > 30 * 1024 * 1024) {
          zip.close();
          reject(new Error("Видео в архиве превышают допустимый размер."));
          return;
        }
        zip.openReadStream(entry, (streamError, stream) => {
          if (streamError) return reject(streamError);
          const chunks = [];
          stream.on("data", chunk => chunks.push(chunk));
          stream.on("error", reject);
          stream.on("end", () => {
            const data = Buffer.concat(chunks);
            totalBytes += data.length;
            entries.push({ filename, data });
            zip.readEntry();
          });
        });
      });
      zip.on("end", () => resolve(entries));
      zip.readEntry();
    });
  });
}

function normalizeVideoFilename(filename) {
  return path.basename(filename).replace(/\.mp4$/i, "").replace(/^TikTok_/i, "").replace(/\s*\(\d+\)$/, "");
}

async function sendCardVideo(ctx, card, options = {}) {
  const fileId = await store.getCardVideo(card.id);
  if (!fileId) return null;
  try { return await ctx.replyWithVideo(fileId, { supports_streaming: true, ...options }); }
  catch (error) { console.warn("Could not send video for " + card.id + ": " + error.message); return null; }
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
    "Добро пожаловать в MEMCARD!\n\n" +
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

async function giveCard(ctx) {
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
      `🎴 **Новая мем-карточка MEMCARD!**\n\n` +
      `🎬 ${card.name}\n` +
      `${rarityColor} Редкость: ${card.rarity}\n💎 Стоимость: ${money(card.value)}\n\n` +
      "⏳ Следующую карточку можно получить через 8 минут.";
    const videoMessage = await sendCardVideo(ctx, card, { caption: cardCaption, parse_mode: "Markdown" });
    if (!videoMessage) await ctx.reply(cardCaption + "\n\nВидео этой карточки пока не подключено.", { parse_mode: "Markdown" });
    await checkAchievements(ctx, collection, card);
  } catch (error) {
    throw error;
  }
}
bot.command("ncard", giveCard);
bot.hears(/^нкарточка$/i, giveCard);

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
  "Обычная": 1000, "Необычная": 3000, "Редкая": 10000,
  "Эпическая": 30000, "Легендарная": 100000, "Мифическая": 300000,
  "Древняя": 1000000, "Божественная": 3000000, "Секретная": 10000000,
  "Эксклюзив": 30000000
};

const UPGRADE_CHANCES = {
  "Обычная": 50,
  "Необычная": 40,
  "Редкая": 30,
  "Эпическая": 22,
  "Легендарная": 15,
  "Мифическая": 10,
  "Древняя": 7,
  "Божественная": 5,
  "Секретная": 3
};

bot.command("nallup", async (ctx) => {
  const cards = await store.getCollection(ctx.from.id);
  const counts = new Map();
  for (const card of cards) counts.set(card.rarity, (counts.get(card.rarity) || 0) + 1);
  const keyboard = new InlineKeyboard();
  Object.keys(RARITIES).forEach((rarity, index) => {
    const count = counts.get(rarity) || 0;
    if (count >= 1 && index < Object.keys(RARITIES).length - 1) {
      keyboard.text(`${RARITIES[rarity].color} ${rarity} ×${count}`, `upgrade:select:${index}`).row();
    }
  });
  if (!keyboard.inline_keyboard.length) {
    await ctx.reply("У тебя пока нет карточек, которые можно улучшить.");
    return;
  }
  await ctx.reply("⚒️ Выбери редкость. Каждая карточка будет отдельно проверена на шанс улучшения; неудачные попытки сохранят карточки без изменений.", { reply_markup: keyboard });
});

bot.callbackQuery(/^upgrade:select:(\d+)$/, async (ctx) => {
  const index = Number(ctx.match[1]);
  const rarities = Object.keys(RARITIES);
  const rarity = rarities[index];
  const cards = await store.getCollection(ctx.from.id);
  const count = cards.filter(card => card.rarity === rarity).length;
  if (!rarity || count < 1 || index >= rarities.length - 1) {
    await ctx.answerCallbackQuery({ text: "Эти карточки уже недоступны для улучшения.", show_alert: true });
    return;
  }
  const next = rarities[index + 1];
  const chance = UPGRADE_CHANCES[rarity];
  const keyboard = new InlineKeyboard()
    .text("✅ Проверить шанс", `upgrade:confirm:${index}`)
    .text("❌ Отмена", "upgrade:cancel");
  await ctx.answerCallbackQuery();
  await ctx.reply(
    `⚒️ Улучшение редкости «${rarity}» → «${next}»\n` +
    `Карточек для попытки: ${count}\n` +
    `Шанс для каждой карточки: ${chance}%\n\n` +
    `Каждая карточка проверяется отдельно. При успехе она станет «${next}» стоимостью ${money(RARITY_VALUES[next])}; при неудаче останется прежней редкости. Карточки не расходуются.`,
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
  const result = await store.massUpgrade(
    ctx.from.id, rarity, next, RARITY_VALUES[next], UPGRADE_CHANCES[rarity]
  );
  if (!result.attempted) {
    await ctx.editMessageText("Карточек выбранной редкости больше нет.");
    return;
  }
  const lines = result.results.map(item => `✅ ${item.name} → ${item.rarity}`);
  await ctx.editMessageText(
    `⚒️ Проверка завершена\nПроверено карточек: ${result.attempted}\nШанс каждой: ${result.chance}%\nУлучшено: ${result.upgraded}\nНе улучшилось: ${result.failed}\nКарточки не расходовались.\n\n` +
    (lines.length ? lines.join("\n") : "В этот раз ни одна карточка не улучшилась.")
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

function adminKeyboard() {
  return new InlineKeyboard()
    .text("📊 Статистика", "admin:stats").text("🎴 Каталог", "admin:catalog").row()
    .text("🎞 Видео", "admin:media").text("👥 Игроки", "admin:players").row()
    .text("💰 Экономика", "admin:economy").text("🚫 Блокировки", "admin:blockhelp").row()
    .text("🖥 Сервер", "admin:system").text("🌐 Интернет", "admin:internet");
}

bot.command("nadmin", async (ctx) => {
  if (!isOwner(ctx)) return ctx.reply("⛔ Доступ запрещён.");
  if (ctx.chat.type !== "private") return ctx.reply("Открой личный чат с ботом и введи /nadmin.");
  const stats = await store.getStats();
  const summary = "🔐 Панель управления MEMCARD\n" +
    "Владелец: @" + OWNER_USERNAME + " · ID: " + OWNER_ID + "\n\n" +
    "Игроков: " + stats.users + " · Карточек: " + stats.cards + "\n" +
    "Каталог: " + CARDS.length + " мем-карточек\n" +
    "Хранилище: " + (store.persistent ? "PostgreSQL" : "временная память");
  await ctx.reply(summary, { reply_markup: adminKeyboard() });
});
bot.command("ncatalog", async (ctx) => {
  if (!isOwner(ctx) || ctx.chat.type !== "private") return ctx.reply("⛔ Доступ запрещён.");
  const lines = CARDS.map(card => `${card.id} — ${card.name} (${card.brand})`);
  for (let i = 0; i < lines.length; i += 25) {
    await ctx.reply(`📚 Каталог NCARD ${i + 1}–${Math.min(i + 25, lines.length)} из ${lines.length}\n\n${lines.slice(i, i + 25).join("\n")}`);
  }
});

bot.callbackQuery(/^admin:(stats|catalog|system|internet|blockhelp|media|players|economy)$/, async (ctx) => {
  if (!isOwner(ctx) || ctx.chat?.type !== "private") {
    await ctx.answerCallbackQuery({ text: "Доступ запрещён", show_alert: true });
    return;
  }
  const action = ctx.match[1];
  await ctx.answerCallbackQuery();
  if (action === "stats") {
    const stats = await store.getStats();
    await ctx.reply("📊 Статистика MEMCARD\nИгроков: " + stats.users +
      "\nВыдано карточек: " + stats.cards + "\nСуммарный баланс: " + money(stats.balance) +
      "\nКарточек в каталоге: " + CARDS.length +
      "\nХранилище: " + (store.persistent ? "PostgreSQL" : "временная память"), { reply_markup: adminKeyboard() });
  } else if (action === "catalog") {
    const counts = Object.entries(CARDS.reduce((a, card) => (a[card.rarity]=(a[card.rarity]||0)+1,a), {}))
      .map(([r,n]) => r + ": " + n).join("\n");
    await ctx.reply("🎴 Каталог MEMCARD\nВсего карточек: " + CARDS.length + "\n\n" + counts +
      "\n\nПолный список ID: /ncatalog", { reply_markup: adminKeyboard() });
  } else if (action === "media") {
    let connected = 0;
    for (const card of CARDS) if (await store.getCardVideo(card.id)) connected++;
    await ctx.reply("🎞 Видео карточек\nПодключено: " + connected + "/" + CARDS.length +
      "\nНе подключено: " + (CARDS.length-connected) +
      "\n\nОтправь архив с подписью /nimportzip — бот сам сопоставит файлы и подключит видео. Для одного ролика: /nsetvideo mell-01. ID смотри через /ncatalog.", { reply_markup: adminKeyboard() });
  } else if (action === "players") {
    await ctx.reply("👥 Игроки\n/nplayer ID — профиль и коллекция\n/nblock ID — заблокировать\n/nunblock ID — разблокировать\n/nreply ID текст — ответить", { reply_markup: adminKeyboard() });
  } else if (action === "economy") {
    await ctx.reply("💰 Экономика AM-очков\n/naddbalance ID сумма — начислить\n/nsetbalance ID сумма — установить\n/nplayer ID — посмотреть баланс", { reply_markup: adminKeyboard() });
  } else if (action === "system") {
    const cpus = os.cpus(), total = os.totalmem(), free = os.freemem();
    const load = os.loadavg().map(v => v.toFixed(2)).join(" / ");
    await ctx.reply("🖥 Нагрузка хоста\nCPU: " + cpus.length + " ядер (" + (cpus[0]?.model || "неизвестно") +
      ")\nLoad average (1/5/15 мин): " + load + "\nRAM: " +
      ((total-free)/1024/1024/1024).toFixed(2) + " / " + (total/1024/1024/1024).toFixed(2) +
      " GB\nПлатформа: " + os.platform() + " " + os.arch(), { reply_markup: adminKeyboard() });
  } else if (action === "internet") {
    const started = Date.now();
    try {
      const response = await fetch("https://api.telegram.org", { signal: AbortSignal.timeout(5000) });
      await ctx.reply("🌐 Интернет доступен. Telegram API: HTTP " + response.status +
        "; задержка " + (Date.now()-started) + " мс.", { reply_markup: adminKeyboard() });
    } catch (error) {
      await ctx.reply("⚠️ Проверка интернета не прошла: " + error.message, { reply_markup: adminKeyboard() });
    }
  } else {
    await ctx.reply("🚫 Управление игроками:\n/nblock ID — заблокировать\n/nunblock ID — разблокировать\n/nplayer ID — профиль игрока\n/nreply ID текст — ответить\n/ngivecard ID ID_карточки [кол-во] — выдать\n/nremovecard ID ID_карточки [кол-во] — удалить\n/naddbalance ID сумма — начислить AM-очки\n/nsetbalance ID сумма — установить баланс\n/ncardcount ID — количество карточек", { reply_markup: adminKeyboard() });
  }
});
bot.on("message:photo", async (ctx, next) => {
  if (!isOwner(ctx) || ctx.chat.type !== "private") return next();
  const caption = String(ctx.message.caption || "");
  const parts = caption.trim().split(" ").filter(Boolean);
  if (!parts[0] || parts[0].split("@")[0] !== "/nsetphoto" || !parts[1]) return next();
  const cardKey = parts[1].toLowerCase();
  const card = CARDS.find(item => item.id === cardKey);
  if (!card) {
    await ctx.reply("Неизвестный ID карточки. Проверь каталог catalog.js.");
    return;
  }
  const photo = ctx.message.photo.at(-1);
  await store.setCardPhoto(cardKey, photo.file_id);
  await ctx.reply(`Фото для «${card.name}» сохранено. Оригинальный файл Telegram используется без изменения телефона.`);
});

bot.command("nclearphoto", async (ctx) => {
  if (!isOwner(ctx) || ctx.chat.type !== "private") return ctx.reply("⛔ Доступ запрещён.");
  const cardKey = String(ctx.match || "").trim().split(/\s+/)[0];
  if (!CARDS.some(item => item.id === cardKey)) return ctx.reply("Неизвестный ID карточки.");
  await store.clearCardPhoto(cardKey);
  await ctx.reply(`Фото карточки ${cardKey} удалено из настроек.`);
});


bot.command("ngivecard", async (ctx) => {
  if (!isOwner(ctx) || ctx.chat.type !== "private") return ctx.reply("⛔ Доступ запрещён.");
  const args = String(ctx.match || "").trim().split(/\s+/);
  const targetId = args[0];
  const cardKey = (args[1] || "").toLowerCase();
  const quantity = args[2] === undefined ? 1 : Number(args[2]);
  if (!/^\d+$/.test(targetId || "") || !cardKey || !Number.isInteger(quantity) || quantity < 1 || quantity > 100) {
    return ctx.reply("Формат: /ngivecard ID_игрока ID_карточки [количество]\nКоличество: от 1 до 100. Пример: /ngivecard 123456789 iphone-15-pro 5");
  }
  const card = CARDS.find(item => item.id.toLowerCase() === cardKey);
  if (!card) return ctx.reply("❌ Карточка не найдена. Посмотри ID через /ncatalog.");
  try {
    await store.upsertUser({ id: Number(targetId), first_name: "Игрок" });
    for (let i = 0; i < quantity; i++) {
      await store.addCard(Number(targetId), card);
      await store.logAdminGrant(OWNER_ID, Number(targetId), card);
    }
    let notified = false;
    try {
      const caption = `🎁 Тебе выдали карточки MEMCARD!\n🎬 ${card.name}\nРедкость: ${card.rarity}\nСтоимость: ${money(card.value)}\nКоличество: ${quantity}`;
      const videoId = await store.getCardVideo(card.id);
      if (videoId) await bot.api.sendVideo(Number(targetId), videoId, { caption, supports_streaming: true });
      else await bot.api.sendMessage(Number(targetId), caption + "\nВидео пока не подключено.");
      notified = true;
    } catch (e) {
      console.warn("Could not notify card recipient:", e.message);
    }
    await ctx.reply(`✅ Выдано игроку ${targetId}: «${card.name}» × ${quantity}.\n` +
      (notified ? "📨 Игрок получил уведомление." : "⚠️ Уведомление не отправлено; карточки добавлены в коллекцию."));
  } catch (error) {
    console.error("Admin card grant failed:", error);
    await ctx.reply("❌ Не удалось выдать карточки. Проверь логи сервера.");
  }
});

bot.command("nremovecard", async (ctx) => {
  if (!isOwner(ctx) || ctx.chat.type !== "private") return ctx.reply("⛔ Доступ запрещён.");
  const args = String(ctx.match || "").trim().split(/\s+/);
  const targetId = args[0];
  const cardKey = (args[1] || "").toLowerCase();
  const quantity = args[2] === undefined ? 1 : Number(args[2]);
  if (!/^\d+$/.test(targetId || "") || !cardKey || !Number.isInteger(quantity) || quantity < 1 || quantity > 1000) {
    return ctx.reply("Формат: /nremovecard ID_игрока ID_карточки [количество]\nПример: /nremovecard 123456789 iphone-15-pro 2");
  }
  const card = CARDS.find(item => item.id.toLowerCase() === cardKey);
  if (!card) return ctx.reply("❌ Карточка не найдена. Посмотри ID через /ncatalog.");
  try {
    const removed = await store.removeCards(Number(targetId), card.id, quantity);
    await ctx.reply(removed
      ? `🗑 У игрока ${targetId} удалено карточек «${card.name}»: ${removed}.`
      : `У игрока ${targetId} нет карточек «${card.name}».`);
  } catch (error) {
    console.error("Admin card removal failed:", error);
    await ctx.reply("❌ Не удалось удалить карточки. Проверь логи сервера.");
  }
});

bot.command("ncardcount", async (ctx) => {
  if (!isOwner(ctx) || ctx.chat.type !== "private") return ctx.reply("⛔ Доступ запрещён.");
  const targetId = String(ctx.match || "").trim().split(/\s+/)[0];
  if (!/^\d+$/.test(targetId || "")) return ctx.reply("Формат: /ncardcount ID_игрока");
  const collection = await store.getCollection(Number(targetId));
  await ctx.reply(`📦 У игрока ${targetId} всего карточек: ${collection.length}.`);
});


bot.command("nplayer", async (ctx) => {
  if (!isOwner(ctx) || ctx.chat.type !== "private") return ctx.reply("⛔ Доступ запрещён.");
  const id = String(ctx.match || "").trim().split(/\s+/)[0];
  if (!/^\d+$/.test(id || "")) return ctx.reply("Формат: /nplayer ID");
  const user = await store.getUser(Number(id));
  if (!user) return ctx.reply("Игрок пока не зарегистрирован.");
  const collection = await store.getCollection(Number(id));
  await ctx.reply("👤 Игрок " + id + "\nИмя: " + (user.name || "неизвестно") +
    "\nUsername: " + (user.username ? "@" + user.username : "не указан") +
    "\nБаланс: " + money(user.balance) + "\nКарточек: " + collection.length +
    "\nДостижений: " + (user.achievements || []).length +
    "\nБлокировка: " + (await store.isBlocked(Number(id)) ? "включена" : "нет"));
});

bot.command("naddbalance", async (ctx) => {
  if (!isOwner(ctx) || ctx.chat.type !== "private") return ctx.reply("⛔ Доступ запрещён.");
  const args = String(ctx.match || "").trim().split(/\s+/);
  const id = args[0], amount = Number(args[1]);
  if (!/^\d+$/.test(id || "") || !Number.isSafeInteger(amount) || amount < 1 || amount > 1000000000)
    return ctx.reply("Формат: /naddbalance ID сумма (1–1 000 000 000)");
  await store.upsertUser({ id: Number(id), first_name: "Игрок" });
  const balance = await store.addBalance(Number(id), amount);
  await ctx.reply("Начислено " + money(amount) + " игроку " + id + ". Новый баланс: " + money(balance) + ".");
});

bot.command("nsetbalance", async (ctx) => {
  if (!isOwner(ctx) || ctx.chat.type !== "private") return ctx.reply("⛔ Доступ запрещён.");
  const args = String(ctx.match || "").trim().split(/\s+/);
  const id = args[0], amount = Number(args[1]);
  if (!/^\d+$/.test(id || "") || !Number.isSafeInteger(amount) || amount < 0 || amount > 100000000000)
    return ctx.reply("Формат: /nsetbalance ID сумма (от 0 до 100 000 000 000)");
  await store.upsertUser({ id: Number(id), first_name: "Игрок" });
  const balance = await store.setBalance(Number(id), amount);
  await ctx.reply("Баланс игрока " + id + " установлен: " + money(balance) + ".");
});

bot.on("message", async (ctx, next) => {
  if (!isOwner(ctx) || ctx.chat?.type !== "private") return next();
  const caption = String(ctx.message?.caption || "").trim();
  const match = caption.match(/^\/nsetvideo(?:@\w+)?\s+([a-z0-9_-]+)$/i);
  if (!match) return next();
  const card = CARDS.find(item => item.id === match[1].toLowerCase());
  if (!card) return ctx.reply("Неизвестный ID карточки. Посмотри /ncatalog.");
  const media = ctx.message.video || (ctx.message.document?.mime_type?.startsWith("video/") ? ctx.message.document : null);
  if (!media?.file_id) return ctx.reply("Пришли видео MP4 с подписью /nsetvideo " + card.id);
  await store.setCardVideo(card.id, media.file_id);
  await ctx.reply("Видео для «" + card.name + "» сохранено.");
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



bot.on("message:document", async (ctx, next) => {
  if (!isOwner(ctx) || ctx.chat?.type !== "private") return next();
  const caption = String(ctx.message.caption || "").trim();
  if (!/^\/nimportzip(?:@\w+)?$/i.test(caption)) return next();
  const doc = ctx.message.document;
  if (!/\.zip$/i.test(doc.file_name || "") && doc.mime_type !== "application/zip" && doc.mime_type !== "application/x-zip-compressed") {
    return ctx.reply("Прикрепи ZIP-архив с видео и подпиши его /nimportzip.");
  }
  if (Number(doc.file_size || 0) > 20 * 1024 * 1024) return ctx.reply("Архив больше 20 МБ. Отправь ZIP как документ меньшего размера.");
  const status = await ctx.reply("⏳ Получаю архив и подготавливаю импорт видео...");
  try {
    const file = await ctx.api.getFile(doc.file_id);
    if (!file.file_path) throw new Error("Telegram не вернул путь к архиву.");
    const response = await fetch("https://api.telegram.org/file/bot" + process.env.TELEGRAM_BOT_TOKEN + "/" + file.file_path, {
      signal: AbortSignal.timeout(60000)
    });
    if (!response.ok) throw new Error("Не удалось скачать архив из Telegram: HTTP " + response.status);
    const zipBuffer = Buffer.from(await response.arrayBuffer());
    if (zipBuffer.length > 20 * 1024 * 1024) throw new Error("Архив превышает лимит 20 МБ.");
    const entries = await extractMp4Entries(zipBuffer);
    const imported = [];
    const skipped = [];
    const seen = new Set();
    for (const entry of entries) {
      const sourceId = normalizeVideoFilename(entry.filename);
      const card = CARDS.find(item => item.sourceFile === sourceId);
      if (!card || seen.has(card.id)) {
        skipped.push(entry.filename);
        continue;
      }
      seen.add(card.id);
      const sent = await bot.api.sendVideo(OWNER_ID, new InputFile(entry.data, entry.filename), {
        caption: "Импорт видео для карточки " + card.id,
        supports_streaming: true
      });
      const videoId = sent.video?.file_id;
      if (!videoId) {
        skipped.push(entry.filename);
        continue;
      }
      await store.setCardVideo(card.id, videoId);
      try { await bot.api.deleteMessage(OWNER_ID, sent.message_id); } catch (_) {}
      imported.push(card.id);
    }
    await ctx.api.editMessageText(ctx.chat.id, status.message_id,
      "✅ Импорт завершён.\nПодключено видео: " + imported.length + "/" + CARDS.length +
      "\nПропущено файлов: " + skipped.length +
      "\n\nОткрой /nadmin → «Видео», чтобы проверить каталог.");
  } catch (error) {
    console.error("MEMCARD ZIP import failed:", error);
    await ctx.api.editMessageText(ctx.chat.id, status.message_id, "❌ Не удалось импортировать архив: " + error.message);
  }
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
      await bot.api.setMyCommands([
        { command: "ncard", description: "Получить карточку (кулдаун 8 минут)" },
        { command: "nakk", description: "Мой профиль" },
        { command: "ncollection", description: "Моя коллекция" },
        { command: "nachievements", description: "Достижения" },
        { command: "nallup", description: "Улучшить карточки" },
        { command: "ntop", description: "Рейтинг группы" },
        { command: "nsettings", description: "Настройки профиля" }
      ]);
      console.log("Telegram command menu configured.");
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
