package edu.uiuc.cs427app.theme;

import org.junit.Assert;
import org.junit.Test;

/** Tests the parser and fallback behavior. */
public class ThemeParserTest {

    /** Parses a valid payload into a theme spec. */
    @Test
    public void parseThemeJson_returnsThemeForValidPayload() {
        String rawJson = "{"
                + "\"backgroundColor\":\"#112233\","
                + "\"textColor\":\"#F1F2F3\","
                + "\"accentColor\":\"#224466\","
                + "\"cardColor\":\"#335577\","
                + "\"headerColor\":\"#446688\","
                + "\"borderColor\":\"#557799\","
                + "\"emojiHint\":\"🌊\""
                + "}";

        ThemeSpec themeSpec = ThemeParser.parseThemeJson(rawJson);

        Assert.assertNotNull(themeSpec);
        Assert.assertEquals("#112233", themeSpec.getBackgroundColor());
        Assert.assertEquals("#F1F2F3", themeSpec.getTextColor());
    }

    /** Rejects a payload with an invalid required color. */
    @Test
    public void parseThemeJson_returnsNullForInvalidRequiredColor() {
        String rawJson = "{"
                + "\"backgroundColor\":\"blue\","
                + "\"textColor\":\"#F1F2F3\","
                + "\"accentColor\":\"#224466\","
                + "\"cardColor\":\"#335577\","
                + "\"headerColor\":\"#446688\","
                + "\"borderColor\":\"#557799\","
                + "\"emojiHint\":\"🌊\""
                + "}";

        ThemeSpec themeSpec = ThemeParser.parseThemeJson(rawJson);

        Assert.assertNull(themeSpec);
    }

    /** Falls back to the default theme when parsing fails. */
    @Test
    public void parseThemeJsonOrDefault_returnsDefaultThemeOnFailure() {
        ThemeSpec themeSpec = ThemeParser.parseThemeJsonOrDefault("not-json");

        Assert.assertNotNull(themeSpec);
        Assert.assertEquals(ThemeDefaults.defaultTheme().getBackgroundColor(),
                themeSpec.getBackgroundColor());
    }
}
