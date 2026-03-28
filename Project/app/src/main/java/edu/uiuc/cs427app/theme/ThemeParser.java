package edu.uiuc.cs427app.theme;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Pattern;

/** Parses and validates structured theme JSON. */
public final class ThemeParser {

    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    /** Utility class. */
    private ThemeParser() {
    }

    /** Parses theme JSON and returns null when validation fails. */
    public static ThemeSpec parseThemeJson(String rawThemeJson) {
        if (rawThemeJson == null || rawThemeJson.trim().isEmpty()) {
            return null;
        }

        try {
            JSONObject jsonObject = new JSONObject(rawThemeJson);
            ThemeSpec defaults = ThemeDefaults.defaultTheme();

            String backgroundColor = readRequiredColor(jsonObject, "backgroundColor");
            String textColor = readRequiredColor(jsonObject, "textColor");
            String accentColor = readOptionalColor(jsonObject, "accentColor", defaults.getAccentColor());
            String cardColor = readOptionalColor(jsonObject, "cardColor", defaults.getCardColor());
            String headerColor = readOptionalColor(jsonObject, "headerColor", defaults.getHeaderColor());
            String borderColor = readOptionalColor(jsonObject, "borderColor", defaults.getBorderColor());
            String emojiHint = jsonObject.optString("emojiHint", defaults.getEmojiHint()).trim();

            if (backgroundColor == null || textColor == null || accentColor == null
                    || cardColor == null || headerColor == null || borderColor == null) {
                return null;
            }

            if (emojiHint.isEmpty()) {
                emojiHint = defaults.getEmojiHint();
            }

            return new ThemeSpec(backgroundColor, textColor, accentColor,
                    cardColor, headerColor, borderColor, emojiHint);
        } catch (JSONException exception) {
            return null;
        }
    }

    /** Parses theme JSON and falls back to the default theme on failure. */
    public static ThemeSpec parseThemeJsonOrDefault(String rawThemeJson) {
        ThemeSpec parsedTheme = parseThemeJson(rawThemeJson);
        return parsedTheme != null ? parsedTheme : ThemeDefaults.defaultTheme();
    }

    /** Reads a required color field. */
    private static String readRequiredColor(JSONObject jsonObject, String key) {
        String color = jsonObject.optString(key, "").trim();
        return isValidHexColor(color) ? color.toUpperCase() : null;
    }

    /** Reads an optional color field or returns a default value. */
    private static String readOptionalColor(JSONObject jsonObject, String key, String defaultColor) {
        if (!jsonObject.has(key)) {
            return defaultColor;
        }

        String color = jsonObject.optString(key, "").trim();
        return isValidHexColor(color) ? color.toUpperCase() : null;
    }

    /** Checks whether a string matches #RRGGBB. */
    private static boolean isValidHexColor(String color) {
        return color != null && HEX_COLOR_PATTERN.matcher(color).matches();
    }
}
