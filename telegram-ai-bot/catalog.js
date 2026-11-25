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
  "iphone-14": "Необычная",
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
  "oneplus-9-pro": "https://image01.oneplus.net/shop/202104/27/1-M00-24-8B-rB8bwmCICVmAN0CwAAVb_9VyE6w579.png",
  "oneplus-10-pro": "https://oasis.opstatics.com/content/dam/oasis/page/press-photos/10pro/green/10pro-1.png",
  "galaxy-s20-plus": "https://api.samsungmobilepress.com/file/0D9B48A22DBA1F99D625EDB565E1607BC88FD99846B6EEC0DBA3067A205D14A1F1D560DBBFC7BC965DE54CD79092504122892DE408FC8455C5A3083C6C9ECBC87B482AAECC0FF1E480B7F6F8D7C678306086B3268A03ECA1A1F9098DE2040B16C9B6FA93DF434905C71A82819ED15AC41EC66FC141CA3AC29C2EDEB0CAD8F02B33B9ABEB97E3A3A4E9DE0C8DE39FA1F46D452C68CE2AC1840E8156F52F498D17",
  "galaxy-s21": "https://api.samsungmobilepress.com/file/879A851E8E9424F9A1100B7523087E4EA0FBFE0628CBAC01B53E88FC05D7071BA759B83C31B46527B9FBF9A7FD10989AC366DE0C9709F45335BE960F1DD3CAF7AEABC5E4058EFD4AA8DB18A172191B8264545E1C72F4D899CB5A016D26058DC60AE3B23774C5BB807AD56047BD63D0AF7737360E80994C025D21C75AD8A987C3D758694F26BD9C35057D125649A864B5FCA128B3F7F84BA6EA689AEF3F01C608",
  "galaxy-s21-plus": "https://api.samsungmobilepress.com/file/38C156A8FE620D3F9A5E7DFE3BDEFB8FB5C21C908B039E4A47745CDF50D75A50C9AE96714862680537EA1D3ADC4A7222FF3A8429F45133E79464F9D712A0CB2FA7D236DCE022E1DB169F26C31C1A68C77612D2BCCECF1218E5E3997EEF66093736707B749007143363CC33F6CCF9D28CE8643518270D94879991B51006C824A39B3C0FF135D2B70E7826C5E8C34229A8F59B306B00D0C080327F3344ACA90BDE",
  "galaxy-s21-ultra": "https://api.samsungmobilepress.com/file/B491848AD7F91044B5C1E5C00BD0FF46C9A3B3EC24AD51BE47EE848FF08D457EEF61F9A177479B322CD005E99F3A580936DD64B3F8BA7DFFEA6FCE334768412F098E844C00653F238410F2E446676DAD7DA9E4181C29B1AD2B8FA8EE6B12CF804BAAE3A94078AED99C9C256F4C674B4A37886A8BB8E4995216D6C90097C476E51F0F663B0FE2FE57E04C50A0FF7DE712C389D87BFA89EBE852CFAB87FFB52A1D",
  "galaxy-s22": "https://api.samsungmobilepress.com/file/C146611D49DB7455B9EF2361C33DCBC074CC856EF6F791C136BBF90606D59F27A2B7553AED7634F89E4F7D5BDC899DB531BED1B20FF49A4DBB02EC0FABED70F7E3D26767C242E15082E74E5050B8FF374BDC17FF9A30EA9B799D6D64327CDCF5E8B7D0D6DDB4887EE613FFF21269705555CC005253E662AE809945A7ECDA43342396B4585BE2D6948F1174C2BF575B61",
  "galaxy-s22-plus": "https://api.samsungmobilepress.com/file/9F752AD9894711CA00230E9AC216857F78AE0379C794416E72FCDF609C508A7757BF0506FA106E944C365DE0FB2FFBFC34392E57E42D0C7812482E65C63985230288F68DC93B30628D2691B2B36A40A11D83EF0825DD74A6907AC522817FEDE2EF24BBC8FB8F190425A6623DBD851A731F455929C5D1EE182FBFA94352DB67C49EDCB8BB7E85EA1F16B5A7941BDD3875",
  "galaxy-s22-ultra": "https://api.samsungmobilepress.com/file/1F5F0B1B053C150EB80A9774AB475AF3E5914002E29248998B73F85D6030568E0C9396CCA44FAE5C9A6C4F47B1058C75D2FE18C33BBCE314F2260E7EF787077A77A1B44B448654AC7893152CD5439E20241F36B97AC6E12CFEE8E128A90C5D0F66A064415879D3CFFE4C64660A1FECAD7D5DD9466A8A16015476BD4F1EA3922ACEFE03DCA2B41E6083C253302175CD8D1EFCD28B56F7BB8D9B765642A846D8C628FDD1CE9C80E4026A1828C3A45D678C",
  "galaxy-s23-fe": "https://api.samsungmobilepress.com/file/06F4A57BE244797C20B0C1C1A69A6D66E867CB63F8FF7200B247FDA7F1F0F1537FE36675FEEE7E659E6C3380DA688B98A7BE8AEA1DB2BADEE63D028C90819884F5A6DF6867F3A94388671798C3BF5C73C4362CFCEC3B23C30B3ECE3A6D465A03B88BB78AE7A0056D383CB2B91FA81CB68FC6D1EB34EDC76FC7B271F10F51656D",
  "galaxy-s24": "https://api.samsungmobilepress.com/file/22C389B1BC85985D0057BACA1FDB03BF607C7AC2D0F2FC60F0946E39C684A8A0A360B6FE61BD3253DB6D839878C29CDF6BDC25E8570147DD5BFFF842650EDA63BBCC1D80D1170055BA8C1A3206379C9C156EF6EA715BB71745C5605B01D895E74667E13BB7A22FA202AF3E66C0D4602F6B48B4CE8BACE7322A653E4E4E791412",
  "galaxy-s24-plus": "https://api.samsungmobilepress.com/file/2B49DE3AF6DC5C385DC88B10B4D7FAB2CB1E257C5E18F6D82B5D28756A5D59E806C620F36AB538A1E6339F4A378CA7885917574E1DD0F19009130A72F0746330A14BA84873D042C9026232590390574F49B6B0BA0DFF940A9A55C80F25BC382A76E0D85765D4CACAA752D97EE8A6539A5200791B9208F6C8E1ECA292FFA57706",
  "galaxy-s20-ultra": "https://api.samsungmobilepress.com/file/EF7ACC5406BBE9FCE26AA11E780E115DEA59BE9130205EDBAAC7C29D7A1FAC3E4FBE089C2B595690440E1487F93521C1AD8BB1A91794A8B12E300547E6FB88CB19CA70F44FAC5B7D59B2AC70DD74D4316ACE369C7D80ED7D629F6370E5A8CBF5947EDF13290B128C8571890E68B6FA5312AF29623E870F6A878DC03CD376A268AFFE4BD64F0698FAF86B24FE01177DEFAF5D0ADA9B9A53E7A815B2A300F3D38F",
  "galaxy-s21-fe": "https://api.samsungmobilepress.com/file/E14FF69E0595D0473BFA4D5865F86D0206395665D2E124F1919A58F9C6E06AECD3C2C87ED532C50BAADBC735F987BBDCC08E0799CA955D7CE810B2074C65EC7728E3B0DE1349EB74D40BC562362BCC79EFBB2D9DA9EBE968EFB6A3820F85C34F94FD35D893FB9551B026ABB6145B7DE8BD5C480F21D8B35F805C0FD39201AF333382B21075917E35138B74467333AB3AFA809B5E9B56C3CB982EBE2398C93089",
  "galaxy-s23-plus": "https://api.samsungmobilepress.com/api/v1/file/DF891757399C82C9CC0CF9B98EE9BC9568F416604A3E3FD5CB9DEC9FDC9A259300B89890B5EBC8AFA6AD9D58BED873F844CF4EA43F672EDAD3EF15678CBC48110485B4392DCD52BD17F92086A29B77A1205A62AE020640D1AFBC86F9B3CB91984BF2390CD43F9288B9ECDDD46777DA46A559FECD7A176DFC6655AD63B6662FFC",
  "galaxy-s23-ultra": "https://api.samsungmobilepress.com/api/v1/file/2792F942A2A2123F030173931A3506693F1F0BF39CEB5BE58D55446CD8A4364370FB854B08F831A146DFC151E3EB85BD8FEC67F402DCF6AFAA1FE4058A429DED12142A032AAB1FCE138FD0078B104B95AAC1A7927213C22AA6D6D29ADDAF39519F70B87AF6816CAF2ECBFD1C19622FD5AD8804802C5FA1D782529E70C3ADE1D5",
  "galaxy-s24-ultra": "https://api.samsungmobilepress.com/api/v1/file/FC05AE750AD6EB0673D9E9D4C157D4FDCA91C0319D37B2827A4ACC850D3D124959298BB89A1D18EEB9531CE7B6BDB56BB43890C99A07EF46FFD865333FEC385C3A4ECDA52E32E217D32C2807BAAF403A9124FE24BD2AD9F141EA995A91D14E9095EC253173B6A26FE1DB745A586CC1ADDE7D6D440FA45C94CA365A5CF1A540E7",
  "galaxy-s24-fe": "https://api.samsungmobilepress.com/api/v1/file/8A133205D1026324AF25D7E37D220F227468B1CF45DE976788A3E12A03F35BE00360B51DB29BA8D886A8ACC0BC12AA2C376DBB4FC929D9F47C1744D9DF8A333CF16A3E2C34C14EE1D654B359704F79CB5FF4D358C5FA13EFC135736574044255E6945B058629474552A60A6A10C292993E6D3C918AC5C8F89FE79B94D1EEFFE9",
  "galaxy-s25": "https://api.samsungmobilepress.com/file/0EE396749D455F0C9C36ECE7D28B13E6CAF134F1A3C9308CE598CE3B20CEFC3B2BA35EE377A98ECA4FF22AADF26D602E4C9B97A825759F888301CD50C93FB4C41E1344D8B18542264B3887648E0053838AB3CF6B10D04A2493F405203A49D30DA97F8EB446519F306D1F5DD967B4033FCFE21AFCB4A6841902EC21E8B812DE49",
  "galaxy-s25-plus": "https://api.samsungmobilepress.com/api/v1/file/936B503015A592F690F8A154EF03AB0B35BA1D74C32471E2887DCDB69640D8247752C836ACBE67102DD7695BC3ED879B3A8EE2DCA07E1A1BC271A824B16C7E9767521C2405FB59BA108EB55341CD9E59CB1E5A22A618698769765F329ED28BE176F3E50275D6815F241A15DB3125FF18964B061C168B58ACBC4A920BB971FDCEAE8478691691EF6D7522057222568784",
  "galaxy-s25-ultra": "https://api.samsungmobilepress.com/file/9A8B0C5E7AE223941FE931B23B810EB4B0889FB378F9B1FA94096036C43DE1D9CBD04954550FBC1C771FCC793F100E9B5F5CA2F530F87B9FD4380D9EADEC2F54798273B45EB93A9033A5AD11EE2772F26A4BAD909A7CC2D855BCEDD00CD694A0D3D74A1A1DFD89AB23A207E29BB7C4FCB75408D09299E0D7A20B2E7C743E5C83BFD92E8F115E7A657CE4E9C368FF4F451E8D9FE7A63E4C776A67EDBF7A360A39",
  "galaxy-s25-edge": "https://api.samsungmobilepress.com/file/45620DA5F6886EEAD98BB82786DCB3154A817FB7154A249D281DA06E4E5F5EF42D7149DB122DE1D18A551E7FE350A6B250346BA90DC44FD5B24FE1E15335408EB612FB33ED6D039BA1A46BD548C604C4476731AAC416D9CEC5A21863B3FBA6CE16D707F52F314A54C811C760245BEC05BF12FBDCCA1FDCA1B617A80C2DE5FB977A1E32D71A389F1960A6BF7CB58E9BC9",
  "iphone-12": "https://www.apple.com/newsroom/images/product/iphone/standard/apple_iphone-12_new-design_10132020_big.jpg.large.jpg",
  "iphone-12-mini": "https://www.apple.com/newsroom/images/product/iphone/standard/apple_iphone-12_new-design_10132020_big.jpg.large.jpg",
  "iphone-12-pro": "https://www.apple.com/newsroom/images/product/iphone/standard/Apple_iphone12pro-stainless-steel-gold_10132020_inline.jpg.large.jpg",
  "iphone-12-pro-max": "https://www.apple.com/newsroom/images/product/iphone/standard/Apple_iphone12pro-stainless-steel-gold_10132020_inline.jpg.large.jpg",
  "iphone-13-pro": "https://www.apple.com/newsroom/images/product/iphone/standard/Apple_iPhone-13-Pro_iPhone-13-Pro-Max_09142021_inline.jpg.large.jpg",
  "iphone-13-pro-max": "https://www.apple.com/newsroom/images/product/iphone/standard/Apple_iPhone-13-Pro_iPhone-13-Pro-Max_09142021_inline.jpg.large.jpg",
  "iphone-14": "https://www.apple.com/newsroom/images/product/iphone/standard/Apple-iPhone-14-iPhone-14-Plus-hero-220907_Full-Bleed-Image.jpg.large.jpg",
  "iphone-14-plus": "https://www.apple.com/newsroom/images/product/iphone/standard/Apple-iPhone-14-iPhone-14-Plus-hero-220907_Full-Bleed-Image.jpg.large.jpg",
  "iphone-14-pro": "https://www.apple.com/newsroom/images/product/iphone/standard/Apple-iPhone-14-Pro-iPhone-14-Pro-Max-deep-purple-220907_inline.jpg.large.jpg",
  "iphone-14-pro-max": "https://www.apple.com/newsroom/images/product/iphone/standard/Apple-iPhone-14-Pro-iPhone-14-Pro-Max-deep-purple-220907_inline.jpg.large.jpg",
  "iphone-13-mini": "https://www.apple.com/newsroom/images/product/iphone/standard/Apple_iphone13_hero_09142021_inline.jpg.large.jpg",
  "iphone-15": "https://www.apple.com/newsroom/images/2023/09/apple-debuts-iphone-15-and-iphone-15-plus/article/Apple-iPhone-15-lineup-hero-230912_inline.jpg.large.jpg",
  "iphone-15-plus": "https://www.apple.com/newsroom/images/2023/09/apple-debuts-iphone-15-and-iphone-15-plus/article/Apple-iPhone-15-lineup-hero-230912_inline.jpg.large.jpg",
  "iphone-15-pro": "https://www.apple.com/newsroom/images/2023/09/apple-unveils-iphone-15-pro-and-iphone-15-pro-max/article/Apple-iPhone-15-Pro-lineup-hero-230912_Full-Bleed-Image.jpg.large.jpg",
  "iphone-15-pro-max": "https://www.apple.com/newsroom/images/2023/09/apple-unveils-iphone-15-pro-and-iphone-15-pro-max/article/Apple-iPhone-15-Pro-lineup-hero-230912_Full-Bleed-Image.jpg.large.jpg",
  "iphone-16": "https://www.apple.com/newsroom/images/2024/09/apple-introduces-iphone-16-and-iphone-16-plus/article/Apple-iPhone-16-hero-240909_inline.jpg.large.jpg",
  "iphone-16-plus": "https://www.apple.com/newsroom/images/2024/09/apple-introduces-iphone-16-and-iphone-16-plus/article/Apple-iPhone-16-hero-240909_inline.jpg.large.jpg",
  "iphone-16-pro": "https://www.apple.com/newsroom/images/2024/09/apple-debuts-iphone-16-pro-and-iphone-16-pro-max/article/Apple-iPhone-16-Pro-hero-240909_inline.jpg.large.jpg",
  "iphone-16-pro-max": "https://www.apple.com/newsroom/images/2024/09/apple-debuts-iphone-16-pro-and-iphone-16-pro-max/article/Apple-iPhone-16-Pro-hero-240909_inline.jpg.large.jpg",
  "oneplus-10t": "https://oasis.opstatics.com/content/dam/oasis/page/press-photos/10t/green/10T-1.png",
  "xiaomi-12": "https://i01.appmifile.com/webfile/globalimg/products/responsive//xiaomi-12/hand.png",
  "xiaomi-12-pro": "https://i01.appmifile.com/webfile/globalimg/products/pc/xiaomi-12-pro/pc-section04-slider01s.png",
  "xiaomi-12t": "https://i02.appmifile.com/mi-com-product/fly-birds/xiaomi-12t/pc/img01l.jpg",
  "xiaomi-12t-pro": "https://i02.appmifile.com/mi-com-product/fly-birds/pc/xiaomi-12t-pro/4a1dc545023a8b2bcd959b79f6d20aa2.png",
  "xiaomi-13": "https://i02.appmifile.com/mi-com-product/fly-birds/xiaomi-13/pc/54d5746eecd5594b8c5f7e8d55e684be.jpg",
  "xiaomi-13-pro": "https://i02.appmifile.com/mi-com-product/fly-birds/pc/xiaomi-13-pro/d6cbfd30906b26fa819309d56c6dad2a.jpg",
  "xiaomi-13-ultra": "https://i02.appmifile.com/mi-com-product/fly-birds/xiaomi-13-ultra/pc/4bc309b95c7ea99ad67e0d86954778e1.jpg",
  "xiaomi-13t": "https://i02.appmifile.com/mi-com-product/fly-birds/xiaomi-13t-leica/PC/342ce95b8ea6494eb7679c1ebf4ed654.jpg",
  "xiaomi-13t-pro": "https://i02.appmifile.com/329_operator_sg/14/08/2023/3fcfdc0e83081349795e70056be19e35.png",
  "xiaomi-14-ultra": "https://i02.appmifile.com/mi-com-product/fly-birds/new-xiaomi-14-ultra/PC/pc-14ultra-header.jpg",
  "pixel-7a": "https://item-shopping.c.yimg.jp/i/n/light-hikari_840244702083",
  "pixel-9": "https://media.wired.com/photos/66ba7431496f3c2c777f3c8b/16%3A9/w_2400%2Ch_1350%2Cc_limit/Google-Pixel-9-Series-SOURCE-Julian-Chokkattu.jpg",
  "pixel-9-pro": "https://static01.galaxus.com/productimages/3/0/3/0/3/5/1/0/6/9/0/1/1/3/3/0/0/8/2e802447-6dde-4d09-9605-2437b1e9c16f_cropped.jpg_sea.jpeg",
  "pixel-9-pro-xl": "https://multimedia.bbycastatic.ca/multimedia/products/1500x1500/181/18165/18165682.jpg",
  "pixel-9a": "https://www.dateks.lv/images/pic/2400/2400/045/1926.jpg",
  "pixel-10": "https://ecat.ge/Home/GetImage?path=DeviceImages%5CSquareImages%5C7c30ba8b-a355-4011-b753-60344b71a48b_google-pixel-10-blue-single-sim-esim-12gb-128gb_thumbnail.webp",
  "pixel-10-pro": "https://d.techtimes.com/en/full/459076/pixel-10-series.png",
  "pixel-10-pro-xl": "https://s3-ap-southeast-2.amazonaws.com/wc-prod-pim/JPEG_1000x1000/GP10PX512M_google_pixel_10_pro_xl_512g_moonstone.jpg",
  "oneplus-11": "https://static01.galaxus.com/productimages/2/4/5/9/5/1/2/8/9/4/7/4/3/9/0/0/0/6/7/5002d33b-c9cd-46b4-b2e2-36e627a580b6_cropped.jpg_sea.jpeg",
  "oneplus-11r": "https://static.digit.in/default/oneplus-11r-2-23536efa17.jpeg",
  "oneplus-12": "https://bludiode.com/img/p/4/6/0/6/9/46069.jpg",
  "oneplus-12r": "https://skay.ua/255225/oneplus-12r-16256gb-cph2609-cool-blue-eu.jpg",
  "oneplus-13": "https://cdn.idealo.com/folder/Product/205561/5/205561545/s1_produktbild_max/oneplus-13-512gb-midnight-ocean.jpg",
  "oneplus-13r": "https://www.sp-computer.ru/upload/iblock/d16/idnvpwh7j1rxr8j3s94ydy5012dz0yl6.jpg",
  "oneplus-open": "https://www.gadgetscout.co.uk/images/articles/the-best-foldable-phones-for-uk-buyers/product-4.webp",
  "phone-1": "https://cdn.shopify.com/s/files/1/0585/2479/5086/products/phone1black1600x2160.png?crop=center&height=144&v=1658472677&width=144",
  "phone-2": "https://cdn.shopify.com/s/files/1/0585/2479/5086/products/BA_ALAK_200.1001.png?crop=center&height=144&v=1688652012&width=144",
  "phone-2a": "https://cdn.shopify.com/s/files/1/0585/2479/5086/files/0000s_0017_Phone-2a-milk.png?crop=center&height=144&v=1753757253&width=144",
  "phone-3": "https://cdn.shopify.com/s/files/1/0585/2479/5086/files/0000s_0011_Phone-3-white.png?crop=center&height=144&v=1753757231&width=144",
  "phone-3a": "https://med.greatecno.com/1482508-large_default/nothing-phone-3a-256gb-32-mp-front-camera-30x-ultra-zoom-50w-fast-charging-6-77-fhd-amoled-display-white.jpg",
  "find-x5": "https://static01.galaxus.com/productimages/6/2/3/9/8/8/6/9/0/3/0/2/4/0/7/4/8/3/6/e3b1fc13-4ce1-4dbb-b60d-83765ed8b288_cropped.png_sea.jpeg",
  "find-x5-pro": "https://cdn1.productnation.co/stg/sites/4/636c89378e03c.webp",
  "find-x6-pro": "https://www.mevzurize.com/uploads/upload-image/2026/2/1771081814392-image.jpg",
  "find-x7-ultra": "https://cdn.shopify.com/s/files/1/1398/4647/files/2_3f2899f4-7149-4457-adfa-e847c87c5a76.jpg?v=1720774132",
  "find-x8": "https://www.notebookcheck.info/fileadmin/Notebooks/News/_nc4/find-x8-pro-bottom92.jpg",
  "find-x8-pro": "https://www.notebookcheck.info/fileadmin/Notebooks/News/_nc4/find-x8-pro-bottom92.jpg",
  "reno10-pro": "https://cdnpro.eraspace.com/media/catalog/product/o/p/oppo_reno10_pro_5g_silvery_grey_1.jpg",
  "reno11-pro": "https://cdn.mobilecity.vn/mobilecity-vn/images/2023/11/oppo-reno11-pro-den.jpg",
  "reno12-pro": "https://cdn.mobilecity.vn/mobilecity-vn/images/2024/07/oppo-reno12-pro-5g-chinh-hang-bac-tinhvan.jpg.webp",
  "reno13-pro": "https://media.ldlc.com/r1600/ld/products/00/06/22/19/LD0006221988.jpg",
  "p50-pro": "https://static01.galaxus.com/productimages/3/8/0/0/2/3/1/9/8/9/8/7/8/9/5/5/8/3/3/8326c736-103d-40f9-8656-a1d7b4d78d9d.jpg_720.jpeg",
  "mate-50-pro": "https://consumer.huawei.com/dam/content/dam/huawei-cbg-site/weu/uk/mkt/pdp/phones/mate50-pro/img/kv/kv.jpg",
  "p60-pro": "https://shop-cdn.huawei.com/ru/pms/uomcdn/RUHW/pms/202304/gbom/6941487294602/800_800_A7172E79CB2E88B173379E77EB40AE56mp.png",
  "mate-60-pro": "https://consumer.huawei.com/content/dam/huawei-cbg-site/cn/mkt/plp/new-phones/images/mate-series/shelf-huawei-mate60-pro.png",
  "pura-70-ultra": "https://media.extra.com/s/aurora/100364655_800/HUAWEI-Pura-70-Ultra%2C-512-GB%2C-Green?locale=en-GB%2Car-%2A%2C%2A",
  "galaxy-s23": "https://api.samsungmobilepress.com/api/v1/file/314B61A1D6903AD4F28A3AE427D3C02A1D32837AC4EDCCFF4C158F3F8F18C8EF49D2971E51CA94D1E7126B7FE86ACAB0CAF5149EE64AA56D6346086EC15D354420675590E377D00F08511974E97409715BAF6BC4E801693EA7685C1DBD1996575941650AEB79552E241DEB15012004100AFE0A230E00DA89B5B5EC8201AE8C58",
  "galaxy-z-fold6": "https://img.global.news.samsung.com/global/wp-content/uploads/2025/07/Samsung-Mobile-Design-Story-Galaxy-Z-Fold6-and-Flip6_Ultra-Modern-Ultra-Sleek_main1.jpg",
  "galaxy-z-flip6": "https://img.global.news.samsung.com/global/wp-content/uploads/2025/07/Samsung-Mobile-Design-Story-Galaxy-Z-Fold6-and-Flip6_Ultra-Modern-Ultra-Sleek_main1.jpg",
  "galaxy-a55": "https://img.global.news.samsung.com/global/wp-content/uploads/2024/03/Galaxy-A55-5G-and-A35-5G_main1.jpg",
  "xiaomi-14t": "https://i02.appmifile.com/mi-com-product/fly-birds/xiaomi-14t/pc/img-box/img01.jpg",
  "xiaomi-14t-pro": "https://i02.appmifile.com/mi-com-product/fly-birds/xiaomi-14t-pro/PC/header_kv.jpg",
  "xiaomi-15": "https://i02.appmifile.com/mi-com-product/fly-birds/xiaomi-15/pc/2fb551f068f8de5461069dab5cf84556.jpg?f=webp",
  "xiaomi-15-ultra": "https://i02.appmifile.com/mi-com-product/fly-birds/xiaomi-15-ultra/pc/8d37a7f2e7af06521ae6b5f32924d141.jpg",
  "xiaomi-15t": "https://i02.appmifile.com/mi-com-product/fly-birds/xiaomi-15t/pc/1c8166f3f7bc62919818d02982f850ef.jpg",
  "xiaomi-15t-pro": "https://i02.appmifile.com/mi-com-product/fly-birds/xiaomi-15t-pro/pc/43ae224614d1cd4bd3d99d187ef0feca.jpg",
  "redmi-note-13": "https://i02.appmifile.com/mi-com-product/fly-birds/redmi-note-13/PC/bac9e4d29124ae838486e7f567d14361.jpg",
  "redmi-note-13-pro": "https://i02.appmifile.com/mi-com-product/fly-birds/redmi-note-13-pro/pc/a69a4089984da42166ce89df3dc83468.jpg",
  "redmi-note-13-pro-plus": "https://i02.appmifile.com/mi-com-product/fly-birds/redmi-note-13-pro-plus-5g/pc/38c91037333e6412aebe0954985af359.jpg",
  "pixel-6": "https://storage.googleapis.com/gweb-uniblog-publish-prod/images/Google_Pixel_Portfolio_Shot.width-1200.format-webp.webp",
  "pixel-6-pro": "https://storage.googleapis.com/gweb-uniblog-publish-prod/images/Google_Pixel_Portfolio_Shot.width-1200.format-webp.webp",
  "pixel-7": "https://storage.googleapis.com/gweb-uniblog-publish-prod/images/USE_THIS_P7_P7_Pro_P_Watch.width-1200.format-webp.webp",
  "pixel-7-pro": "https://storage.googleapis.com/gweb-uniblog-publish-prod/images/USE_THIS_P7_P7_Pro_P_Watch.width-1200.format-webp.webp",
  "pixel-8": "https://storage.googleapis.com/gweb-uniblog-publish-prod/images/Pixel_8_in_Rose.width-1200.format-webp.webp",
  "pixel-8-pro": "https://storage.googleapis.com/gweb-uniblog-publish-prod/images/Pixel_8_Pro_in_Porcelain.width-1200.format-webp.webp",
  "pixel-8a": "https://storage.googleapis.com/gweb-uniblog-publish-prod/images/Pixel8a_Aloe.width-1200.format-webp.webp"
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
