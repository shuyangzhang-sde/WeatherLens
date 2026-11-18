package edu.uiuc.weatherlens.weatherinsights;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Parses and validates LLM responses for Weather Insights.
 */
public final class WeatherInsightsParser {

    /**
     * Utility class.
     */
    private WeatherInsightsParser() {
    }

    /**
     * Parses question JSON, removing duplicates and blanks.
     */
    public static List<String> parseQuestions(String rawJson) throws WeatherInsightsException {
        if (rawJson == null || rawJson.trim().isEmpty()) {
            throw new WeatherInsightsException("Empty question response from Gemini.");
        }

        try {
            JSONObject response = new JSONObject(rawJson);
            JSONArray questionsArray = response.optJSONArray("questions");
            if (questionsArray == null) {
                throw new WeatherInsightsException("Missing 'questions' array in Gemini response.");
            }

            Set<String> seen = new LinkedHashSet<>();
            List<String> questions = new ArrayList<>();
            for (int index = 0; index < questionsArray.length(); index++) {
                String question = questionsArray.optString(index, "").trim();
                if (question.isEmpty()) {
                    continue;
                }

                String dedupeKey = question.toLowerCase(Locale.US);
                if (seen.contains(dedupeKey)) {
                    continue;
                }

                seen.add(dedupeKey);
                questions.add(question);
            }

            if (questions.size() < 2) {
                throw new WeatherInsightsException("Expected at least 2 valid suggested questions.");
            }

            if (questions.size() > 4) {
                return new ArrayList<>(questions.subList(0, 4));
            }

            return questions;
        } catch (JSONException jsonException) {
            throw new WeatherInsightsException("Failed to parse question JSON response.", jsonException);
        }
    }

    /**
     * Normalizes the generated answer and validates non-empty output.
     */
    public static String parseAnswer(String rawAnswer) throws WeatherInsightsException {
        if (rawAnswer == null) {
            throw new WeatherInsightsException("Empty answer response from Gemini.");
        }

        String answer = rawAnswer.trim();
        if (answer.isEmpty()) {
            throw new WeatherInsightsException("Gemini returned a blank answer.");
        }

        return answer;
    }
}

