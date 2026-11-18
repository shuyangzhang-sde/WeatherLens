package edu.uiuc.weatherlens.theme;

/** Provides the fallback theme used when theme data is missing or invalid. */
public final class ThemeDefaults {

    private static final ThemeSpec DEFAULT_THEME = new ThemeSpec(
            "#F6EAE1",
            "#25262B",
            "#F2AFC1",
            "#FFFFFF",
            "#F6EAE1",
            "#8A6778",
            "☀"
    );

    /** Utility class. */
    private ThemeDefaults() {
    }

    /** Returns the default theme spec. */
    public static ThemeSpec defaultTheme() {
        return DEFAULT_THEME;
    }

    /** Returns the default theme as JSON. */
    public static String defaultThemeJson() {
        return DEFAULT_THEME.toJsonString();
    }
}
