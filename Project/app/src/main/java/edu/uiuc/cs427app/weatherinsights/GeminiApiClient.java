package edu.uiuc.cs427app.weatherinsights;

import edu.uiuc.cs427app.BuildConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Thin Gemini HTTP client wrapper used by Weather Insights.
 */
public class GeminiApiClient {

    private static final String GEMINI_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/"
                    + "gemini-2.5-flash:generateContent";

    /**
     * Sends a prompt to Gemini and returns the generated text part.
     */
    public String generateContent(String prompt, JSONObject generationConfig)
            throws WeatherInsightsException {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new WeatherInsightsException("Prompt cannot be empty.");
        }

        String apiKey = requireApiKey();
        HttpURLConnection connection = null;

        try {
            JSONObject requestBody = buildRequestBody(prompt, generationConfig);
            connection = (HttpURLConnection) new URL(GEMINI_ENDPOINT).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(20000);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("x-goog-api-key", apiKey);
            connection.setDoOutput(true);

            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
            }

            int statusCode = connection.getResponseCode();
            String responseBody = readResponseBody(selectStream(connection, statusCode));
            if (statusCode < 200 || statusCode >= 300) {
                throw new WeatherInsightsException(
                        "Gemini request failed (HTTP " + statusCode + "). " + compact(responseBody));
            }

            return extractTextResponse(responseBody);
        } catch (IOException ioException) {
            throw new WeatherInsightsException(
                    "Failed to call Gemini service.", ioException);
        } catch (JSONException jsonException) {
            throw new WeatherInsightsException(
                    "Unexpected JSON payload from Gemini service.", jsonException);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Reads and validates the configured Gemini API key.
     */
    private String requireApiKey() throws WeatherInsightsException {
        String rawKey = BuildConfig.GEMINI_KEY;
        String apiKey = rawKey == null ? "" : rawKey.trim();
        if (apiKey.isEmpty()
                || "{GEMINI_KEY}".equals(apiKey)
                || "(key here)".equalsIgnoreCase(apiKey)) {
            throw new WeatherInsightsException(
                    "Missing GEMINI_KEY. Set it in gradle.properties so BuildConfig.GEMINI_KEY is populated.");
        }
        return apiKey;
    }

    /**
     * Builds the Gemini request body for a single text prompt.
     */
    private JSONObject buildRequestBody(String prompt, JSONObject generationConfig) throws JSONException {
        JSONObject requestBody = new JSONObject();
        requestBody.put(
                "contents",
                new JSONArray().put(
                        new JSONObject().put(
                                "parts",
                                new JSONArray().put(new JSONObject().put("text", prompt))
                        )
                )
        );

        if (generationConfig != null) {
            requestBody.put("generationConfig", generationConfig);
        }
        return requestBody;
    }

    /**
     * Extracts the first generated text part from the Gemini response.
     */
    private String extractTextResponse(String responseBody) throws JSONException, WeatherInsightsException {
        JSONObject responseJson = new JSONObject(responseBody);
        JSONArray candidates = responseJson.optJSONArray("candidates");
        if (candidates == null || candidates.length() == 0) {
            throw new WeatherInsightsException("Gemini returned no candidates.");
        }

        JSONObject candidate = candidates.optJSONObject(0);
        if (candidate == null) {
            throw new WeatherInsightsException("Gemini candidate payload is invalid.");
        }

        JSONObject content = candidate.optJSONObject("content");
        if (content == null) {
            throw new WeatherInsightsException("Gemini response content is missing.");
        }

        JSONArray parts = content.optJSONArray("parts");
        if (parts == null || parts.length() == 0) {
            throw new WeatherInsightsException("Gemini response parts are missing.");
        }

        JSONObject firstPart = parts.optJSONObject(0);
        String text = firstPart == null ? null : firstPart.optString("text", "").trim();
        if (text == null || text.isEmpty()) {
            throw new WeatherInsightsException("Gemini returned empty generated text.");
        }

        return text;
    }

    /**
     * Selects success or error stream depending on the HTTP status code.
     */
    private InputStream selectStream(HttpURLConnection connection, int statusCode) throws IOException {
        if (statusCode >= 200 && statusCode < 300) {
            return connection.getInputStream();
        }
        InputStream errorStream = connection.getErrorStream();
        return errorStream != null ? errorStream : connection.getInputStream();
    }

    /**
     * Reads a stream as UTF-8 text.
     */
    private String readResponseBody(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            for (String line; (line = reader.readLine()) != null; ) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    /**
     * Truncates noisy error payloads for safer logging/error messages.
     */
    private String compact(String text) {
        if (text == null) {
            return "";
        }
        String compactText = text.replace('\n', ' ').trim();
        return compactText.length() > 240 ? compactText.substring(0, 240) + "..." : compactText;
    }
}

