package com.matrixwall;

final class Settings {
    static final String FILE = "matrix_prefs";
    static final String KEY_STYLE = "style";
    static final String KEY_SIDE = "side";
    static final String KEY_SHAPE = "shape";
    static final String KEY_COLOR = "color";
    static final String KEY_RAIN = "rain";
    static final String KEY_SPLASH_SOUND = "splash_sound";
    static final String KEY_LANGUAGE = "language";
    static final String KEY_ROTATION_SPEED = "rotation_speed";
    static final String KEY_TEXT_SPEED = "text_speed";
    static final String KEY_ICON = "launcher_icon";
    static final String KEY_SECRET_UNLOCKED = "secret_unlocked";
    static final String KEY_SECRET_TAPS = "secret_taps";
    static final String KEY_GLOW = "glow_effect";
    static final String KEY_BATTERY_SAVER = "battery_saver";

    static final String[] STYLE_NAMES = {
        "Фигуры / Shapes", "Матрица / Matrix", "Терминал / Terminal", "Галактика / Galaxy",
        "Тоннель / Tunnel", "Волна / Wave", "Звёздное поле / Starfield", "Кодовая сетка / Code grid",
        "Северное сияние / Aurora", "Орбиты / Orbits", "Пульсация / Pulse", "Кольца / Rings",
        "Частицы / Particles", "Цифровая спираль / Digital spiral", "Дождь символов / Glyph rain",
        "Геометрия / Geometry"
    };
    static final String[] COLOR_NAMES = {
        "Зелёный / Green", "Голубой / Cyan", "Красный / Red", "Янтарный / Amber",
        "Фиолетовый / Purple", "Розовый / Pink", "Синий / Blue", "Белый / White",
        "Оранжевый / Orange", "Бирюзовый / Teal", "Лаймовый / Lime", "Ледяной / Ice",
        "Закат / Sunset", "Лазер / Laser", "Мята / Mint", "Золото / Gold", "Неон / Neon", "Космос / Cosmos"
    };
    static final int[][] COLORS = {
        {0,255,65,190,255,200},{0,220,255,190,245,255},{255,40,40,255,190,190},
        {255,176,0,255,230,160},{190,80,255,235,200,255},{255,70,170,255,200,230},
        {65,105,255,190,205,255},{225,235,245,255,255,255},{255,110,20,255,210,150},
        {0,190,160,180,255,235},{170,255,0,230,255,180},{130,220,255,225,250,255},
        {255,95,65,255,190,120},{70,120,255,190,220,255},{65,255,190,190,255,225},
        {255,205,55,255,245,170},{255,30,220,255,170,250},{125,85,255,205,190,255}
    };
    private Settings() {}
}
