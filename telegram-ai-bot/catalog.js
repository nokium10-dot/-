const RARITY_ORDER = [
  "Обычная", "Необычная", "Редкая", "Эпическая", "Легендарная",
  "Мифическая", "Древняя", "Божественная", "Секретная", "Эксклюзив"
];

const RARITY_COUNTS = [40, 25, 18, 10, 6, 4, 3, 2, 1, 1];
const RARITY_VALUES = {
  "Обычная": 100, "Необычная": 300, "Редкая": 900, "Эпическая": 2500,
  "Легендарная": 8000, "Мифическая": 20000, "Древняя": 50000,
  "Божественная": 100000, "Секретная": 250000, "Эксклюзив": 1000000
};

const MODELS = [
  ["Apple", [
    "iPhone 12", "iPhone 12 mini", "iPhone 12 Pro", "iPhone 12 Pro Max",
    "iPhone 13", "iPhone 13 mini", "iPhone 13 Pro", "iPhone 13 Pro Max",
    "iPhone 14", "iPhone 14 Plus", "iPhone 14 Pro", "iPhone 14 Pro Max",
    "iPhone 15", "iPhone 15 Plus", "iPhone 15 Pro", "iPhone 15 Pro Max",
    "iPhone 16", "iPhone 16 Plus", "iPhone 16 Pro", "iPhone 16 Pro Max"
  ]],
  ["Samsung", [
    "Galaxy S20", "Galaxy S20+", "Galaxy S20 Ultra",
    "Galaxy S21", "Galaxy S21+", "Galaxy S21 Ultra", "Galaxy S21 FE",
    "Galaxy S22", "Galaxy S22+", "Galaxy S22 Ultra",
    "Galaxy S23", "Galaxy S23+", "Galaxy S23 Ultra", "Galaxy S23 FE",
    "Galaxy S24", "Galaxy S24+", "Galaxy S24 Ultra", "Galaxy S24 FE",
    "Galaxy S25", "Galaxy S25+", "Galaxy S25 Ultra", "Galaxy S25 Edge",
    "Galaxy Z Fold6", "Galaxy Z Flip6", "Galaxy A55"
  ]],
  ["Xiaomi", [
    "Xiaomi 12", "Xiaomi 12 Pro", "Xiaomi 12T", "Xiaomi 12T Pro",
    "Xiaomi 13", "Xiaomi 13 Pro", "Xiaomi 13 Ultra", "Xiaomi 13T", "Xiaomi 13T Pro",
    "Xiaomi 14", "Xiaomi 14 Ultra", "Xiaomi 14T", "Xiaomi 14T Pro",
    "Xiaomi 15", "Xiaomi 15 Ultra", "Xiaomi 15T", "Xiaomi 15T Pro",
    "Redmi Note 13", "Redmi Note 13 Pro", "Redmi Note 13 Pro+"
  ]],
  ["Google", [
    "Pixel 6", "Pixel 6 Pro", "Pixel 7", "Pixel 7 Pro", "Pixel 7a",
    "Pixel 8", "Pixel 8 Pro", "Pixel 8a",
    "Pixel 9", "Pixel 9 Pro", "Pixel 9 Pro XL", "Pixel 9a",
    "Pixel 10", "Pixel 10 Pro", "Pixel 10 Pro XL"
  ]],
  ["OnePlus", [
    "OnePlus 9 Pro", "OnePlus 10 Pro", "OnePlus 10T", "OnePlus 11", "OnePlus 11R",
    "OnePlus 12", "OnePlus 12R", "OnePlus 13", "OnePlus 13R", "OnePlus Open"
  ]],
  ["Nothing", ["Phone (1)", "Phone (2)", "Phone (2a)", "Phone (3)", "Phone (3a)"]],
  ["OPPO", [
    "Find X5", "Find X5 Pro", "Find X6 Pro", "Find X7 Ultra", "Find X8", "Find X8 Pro",
    "Reno10 Pro", "Reno11 Pro", "Reno12 Pro", "Reno13 Pro"
  ]],
  ["Huawei", ["P50 Pro", "Mate 50 Pro", "P60 Pro", "Mate 60 Pro", "Pura 70 Ultra"]]
];

const overrides = {
  "iphone-13": "Обычная",
  "galaxy-a55": "Обычная",
  "redmi-note-13": "Обычная",
  "iphone-14": "Необычная",
  "pixel-8": "Необычная",
  "oneplus-12": "Редкая",
  "xiaomi-14-ultra": "Редкая",
  "iphone-16-pro": "Эпическая",
  "galaxy-s25-ultra": "Эпическая",
  "pixel-9-pro-xl": "Легендарная"
};

function slug(value) {
  return value.toLowerCase()
    .replace(/[+]/g, "-plus")
    .replace(/[()]/g, "")
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-|-$/g, "");
}

const flattened = MODELS.flatMap(([brand, models]) =>
  models.map(name => ({ id: slug(name), name, brand }))
);

if (flattened.length !== 110) {
  throw new Error(`NCARD catalog must contain exactly 110 phones; got ${flattened.length}`);
}

let rarityIndex = 0;
let rarityEnd = RARITY_COUNTS[0];
const OFFICIAL_IMAGES = {
  "iphone-13": "https://www.apple.com/newsroom/images/product/iphone/standard/Apple_iphone13_hero_09142021_inline.jpg.large.jpg",
  "galaxy-s20": "https://api.samsungmobilepress.com/file/CB6814F3B827117DE81CB4217CEF4B0CE06A8258F8816B8380620A18A0260E65BF2BAB5D5B851997D3B3093C0E80E8D86B68C701EDFCA26E05788F06629273C944324155B9E0076E334284371AE817D0E2FBA60F2C7094F174FBF242B24BDA027206F9E6582D29260A53E290575176EFCA8A57648E7D28F707EE460C2C4FE15EF534EDA13D1B17557833ABB78B0D7993",
  "xiaomi-14": "https://i02.appmifile.com/mi-com-product/fly-birds/xiaomi-14/PC/headAss2_img.jpeg",
  "oneplus-9-pro": "https://image01.oneplus.net/shop/202104/27/1-M00-24-8B-rB8bwmCICVmAN0CwAAVb_9VyE6w579.png"
};

const cards = flattened.map((model, index) => {
  while (index >= rarityEnd && rarityIndex < RARITY_COUNTS.length - 1) {
    rarityIndex += 1;
    rarityEnd += RARITY_COUNTS[rarityIndex];
  }
  const rarity = overrides[model.id] || RARITY_ORDER[rarityIndex];
  return {
    ...model,
    rarity,
    value: RARITY_VALUES[rarity],
    imageUrl: OFFICIAL_IMAGES[model.id] || null
  };
});

module.exports = cards;
