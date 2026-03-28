package edu.uiuc.cs427app.theme;

/** Provides the fallback theme used when theme data is missing or invalid. */
public final class ThemeDefaults {

    private static final ThemeSpec DEFAULT_THEME = new ThemeSpec(
            "#F4EFE6",
            "#1F1F1F",
            "#2F6B5F",
            "#FFF8ED",
            "#D8BFA3",
            "#BFA27A",
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
