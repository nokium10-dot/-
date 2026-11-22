package com.matrixwall;

final class Settings {
    static final String FILE = "matrix_prefs";
    static final String KEY_STYLE = "style";       // стиль обоев
    static final String KEY_SIDE = "side";         // 0 слева, 1 справа, 2 обе
    static final String KEY_SHAPE = "shape";       // 0 авто, 1..5 конкретная фигура
    static final String KEY_COLOR = "color";       // цветовая тема
    static final String KEY_RAIN = "rain";         // фоновый дождь из символов
    static final String KEY_SPLASH_SOUND = "splash_sound";
    static final String[] STYLE_NAMES = {
            "Фигуры", "Матрица", "Терминал", "Галактика",
            "Тоннель", "Волна", "Звёздное поле", "Кодовая сетка"
    };
    static final String[] COLOR_NAMES = {
            "Зелёный", "Голубой", "Красный", "Янтарный", "Фиолетовый"
    };

    /** {r, g, b, r светлый, g светлый, b светлый} */
    static final int[][] COLORS = {
            {0, 255, 65, 190, 255, 200},
            {0, 220, 255, 190, 245, 255},
            {255, 40, 40, 255, 190, 190},
            {255, 176, 0, 255, 230, 160},
            {190, 80, 255, 235, 200, 255}
    };

    private Settings() {}
}
