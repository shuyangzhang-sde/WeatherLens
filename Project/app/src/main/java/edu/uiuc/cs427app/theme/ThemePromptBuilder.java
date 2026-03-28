package edu.uiuc.cs427app.theme;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Builds the prompt and schema for structured theme generation. */
public final class ThemePromptBuilder {

    /** Utility class. */
    private ThemePromptBuilder() {
    }

    /** Builds a prompt that asks for one theme JSON object. */
    public static String buildUserPrompt(String themeDescription) {
        return "Create a mobile app theme JSON for this description: \"" + themeDescription + "\".\n"
                + "Return one JSON object only.\n"
                + "Use exactly these keys: backgroundColor, textColor, accentColor, cardColor, "
                + "headerColor, borderColor, emojiHint.\n"
                + "Every color must be a valid #RRGGBB string.\n"
                + "Choose colors with strong text/background contrast.";
    }

    /** Builds the JSON schema expected from the model. */
    public static JSONObject buildJsonSchema() throws JSONException {
        JSONObject schema = new JSONObject();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("required", new JSONArray()
                .put("backgroundColor")
                .put("textColor")
                .put("accentColor")
                .put("cardColor")
                .put("headerColor")
                .put("borderColor")
                .put("emojiHint"));

        JSONObject properties = new JSONObject();
        properties.put("backgroundColor", buildHexColorProperty());
        properties.put("textColor", buildHexColorProperty());
        properties.put("accentColor", buildHexColorProperty());
        properties.put("cardColor", buildHexColorProperty());
        properties.put("headerColor", buildHexColorProperty());
        properties.put("borderColor", buildHexColorProperty());

        JSONObject emojiProperty = new JSONObject();
        emojiProperty.put("type", "string");
        emojiProperty.put("maxLength", 4);
        properties.put("emojiHint", emojiProperty);

        schema.put("properties", properties);
        return schema;
    }

    /** Creates the shared schema fragment for a hex color field. */
    private static JSONObject buildHexColorProperty() throws JSONException {
        JSONObject property = new JSONObject();
        property.put("type", "string");
        property.put("pattern", "^#[0-9A-Fa-f]{6}$");
        return property;
    }
}
