package edu.uiuc.cs427app.theme;

import org.json.JSONException;
import org.json.JSONObject;

/** Immutable theme model used by parsing and UI application. */
public class ThemeSpec {

    private final String backgroundColor;
    private final String textColor;
    private final String accentColor;
    private final String cardColor;
    private final String headerColor;
    private final String borderColor;
    private final String emojiHint;

    /** Creates a theme with the colors and display hints used by the UI. */
    public ThemeSpec(String backgroundColor, String textColor, String accentColor,
                     String cardColor, String headerColor, String borderColor,
                     String emojiHint) {
        this.backgroundColor = backgroundColor;
        this.textColor = textColor;
        this.accentColor = accentColor;
        this.cardColor = cardColor;
        this.headerColor = headerColor;
        this.borderColor = borderColor;
        this.emojiHint = emojiHint;
    }

    /** Returns the background color. */
    public String getBackgroundColor() {
        return backgroundColor;
    }

    /** Returns the primary text color. */
    public String getTextColor() {
        return textColor;
    }

    /** Returns the accent color. */
    public String getAccentColor() {
        return accentColor;
    }

    /** Returns the secondary container color. */
    public String getCardColor() {
        return cardColor;
    }

    /** Returns the header color. */
    public String getHeaderColor() {
        return headerColor;
    }

    /** Returns the border color. */
    public String getBorderColor() {
        return borderColor;
    }

    /** Returns the emoji or icon hint. */
    public String getEmojiHint() {
        return emojiHint;
    }

    /** Serializes the theme into a JSON object. */
    public JSONObject toJsonObject() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("backgroundColor", backgroundColor);
        jsonObject.put("textColor", textColor);
        jsonObject.put("accentColor", accentColor);
        jsonObject.put("cardColor", cardColor);
        jsonObject.put("headerColor", headerColor);
        jsonObject.put("borderColor", borderColor);
        jsonObject.put("emojiHint", emojiHint);
        return jsonObject;
    }

    /** Serializes the theme into a compact JSON string. */
    public String toJsonString() {
        try {
            return toJsonObject().toString();
        } catch (JSONException exception) {
            return ThemeDefaults.defaultThemeJson();
        }
    }
}
