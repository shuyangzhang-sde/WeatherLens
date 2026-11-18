package edu.uiuc.weatherlens.weatherinsights;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Builds prompts and schemas for Weather Insights LLM calls.
 */
public final class WeatherInsightsPromptBuilder {

    /**
     * Utility class.
     */
    private WeatherInsightsPromptBuilder() {
    }

    /**
     * Builds the prompt for dynamic question suggestions.
     */
    public static String generateQuestions(WeatherData weatherData) {
        return "You are helping a user make practical decisions for their day.\n"
                + "Use ONLY this weather data:\n"
                + weatherData.toPromptBlock() + "\n\n"
                + "Generate 2 to 4 practical, weather-specific questions.\n"
                + "Questions must depend on this weather data and be useful for daily planning.\n"
                + "Avoid generic filler.\n"
                + "Return ONLY valid JSON with exactly this shape and no markdown:\n"
                + "{\"questions\":[\"...\",\"...\"]}";
    }

    /**
     * Builds the prompt for an answer to a selected question.
     */
    public static String generateAnswer(WeatherData weatherData, String question) {
        return "You are helping a user with weather-based advice.\n"
                + "Use ONLY this weather data:\n"
                + weatherData.toPromptBlock() + "\n\n"
                + "User question: " + question + "\n"
                + "Answer briefly, practically, and in user-friendly plain text.\n"
                + "Do not make claims not supported by the provided weather data.\n"
                + "Do not use markdown or extra formatting.";
    }

    /**
     * Builds the JSON schema expected from question generation.
     */
    public static JSONObject questionsJsonSchema() throws JSONException {
        JSONObject schema = new JSONObject();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("required", new JSONArray().put("questions"));

        JSONObject questionsProperty = new JSONObject();
        questionsProperty.put("type", "array");
        questionsProperty.put("minItems", 2);
        questionsProperty.put("maxItems", 4);
        questionsProperty.put("uniqueItems", true);
        questionsProperty.put("items", new JSONObject()
                .put("type", "string")
                .put("minLength", 1));

        JSONObject properties = new JSONObject();
        properties.put("questions", questionsProperty);
        schema.put("properties", properties);

        return schema;
    }
}

