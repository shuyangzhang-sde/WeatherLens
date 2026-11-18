package edu.uiuc.weatherlens.weatherinsights;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Gemini-backed repository implementation for Weather Insights.
 */
public class GeminiWeatherInsightsRepository implements WeatherInsightsRepository {

    private static final String TAG = "WeatherInsightsRepo";

    private final GeminiApiClient geminiApiClient;
    private final ExecutorService executorService;
    private final Handler callbackHandler;

    /**
     * Creates a repository with default Gemini client and single background worker.
     */
    public GeminiWeatherInsightsRepository() {
        this(new GeminiApiClient(), Executors.newSingleThreadExecutor(), defaultMainHandler());
    }

    /**
     * Creates a repository with injected dependencies.
     */
    public GeminiWeatherInsightsRepository(
            GeminiApiClient geminiApiClient,
            ExecutorService executorService
    ) {
        this(geminiApiClient, executorService, defaultMainHandler());
    }

    /**
     * Creates a repository with injected dependencies and callback handler.
     */
    public GeminiWeatherInsightsRepository(
            GeminiApiClient geminiApiClient,
            ExecutorService executorService,
            Handler callbackHandler
    ) {
        this.geminiApiClient = geminiApiClient;
        this.executorService = executorService;
        this.callbackHandler = callbackHandler;
    }

    /**
     * Generates dynamic suggested questions using the provided weather data.
     */
    @Override
    public List<String> generateQuestions(WeatherData weatherData) throws WeatherInsightsException {
        requireCompleteWeatherData(weatherData);

        try {
            JSONObject generationConfig = new JSONObject();
            generationConfig.put("responseMimeType", "application/json");
            generationConfig.put("responseJsonSchema", WeatherInsightsPromptBuilder.questionsJsonSchema());

            String prompt = WeatherInsightsPromptBuilder.generateQuestions(weatherData);
            String rawResponse = geminiApiClient.generateContent(prompt, generationConfig);
            return WeatherInsightsParser.parseQuestions(rawResponse);
        } catch (JSONException jsonException) {
            throw new WeatherInsightsException(
                    "Failed to build question-generation request.", jsonException);
        }
    }

    /**
     * Generates a dynamic answer for a selected weather question.
     */
    @Override
    public String generateAnswer(WeatherData weatherData, String question) throws WeatherInsightsException {
        requireCompleteWeatherData(weatherData);
        String normalizedQuestion = normalizeQuestion(question);

        try {
            JSONObject generationConfig = new JSONObject();
            generationConfig.put("responseMimeType", "text/plain");

            String prompt = WeatherInsightsPromptBuilder.generateAnswer(
                    weatherData,
                    normalizedQuestion
            );
            String rawResponse = geminiApiClient.generateContent(prompt, generationConfig);
            return WeatherInsightsParser.parseAnswer(rawResponse);
        } catch (JSONException jsonException) {
            throw new WeatherInsightsException(
                    "Failed to build answer-generation request.", jsonException);
        }
    }

    /**
     * Runs question generation asynchronously.
     */
    @Override
    public void generateQuestionsAsync(
            WeatherData weatherData,
            WeatherInsightsCallback<List<String>> callback
    ) {
        if (callback == null) {
            Log.w(TAG, "generateQuestionsAsync called with null callback.");
            return;
        }

        executorService.execute(() -> {
            try {
                List<String> result = generateQuestions(weatherData);
                dispatch(() -> callback.onSuccess(result));
            } catch (WeatherInsightsException exception) {
                Log.w(TAG, "Question generation failed.", exception);
                dispatch(() -> callback.onError(exception));
            }
        });
    }

    /**
     * Runs answer generation asynchronously.
     */
    @Override
    public void generateAnswerAsync(
            WeatherData weatherData,
            String question,
            WeatherInsightsCallback<String> callback
    ) {
        if (callback == null) {
            Log.w(TAG, "generateAnswerAsync called with null callback.");
            return;
        }

        executorService.execute(() -> {
            try {
                String result = generateAnswer(weatherData, question);
                dispatch(() -> callback.onSuccess(result));
            } catch (WeatherInsightsException exception) {
                Log.w(TAG, "Answer generation failed.", exception);
                dispatch(() -> callback.onError(exception));
            }
        });
    }

    /**
     * Stops background execution for this repository.
     */
    @Override
    public void shutdown() {
        executorService.shutdown();
    }

    /**
     * Validates that all required weather fields are present.
     */
    private void requireCompleteWeatherData(WeatherData weatherData) throws WeatherInsightsException {
        if (weatherData == null || !weatherData.isComplete()) {
            throw new WeatherInsightsException(
                    "Weather data is incomplete. city/time/temperature/condition/humidity/wind are required.");
        }
    }

    /**
     * Normalizes and validates the selected question string.
     */
    private String normalizeQuestion(String question) throws WeatherInsightsException {
        String normalized = question == null ? "" : question.trim();
        if (normalized.isEmpty()) {
            throw new WeatherInsightsException("Selected question cannot be blank.");
        }
        return normalized;
    }

    /**
     * Dispatches callback work to the configured callback handler when available.
     */
    private void dispatch(Runnable runnable) {
        if (callbackHandler == null) {
            runnable.run();
            return;
        }
        callbackHandler.post(runnable);
    }

    /**
     * Returns a main-thread handler when the main looper is available.
     */
    private static Handler defaultMainHandler() {
        Looper mainLooper = Looper.getMainLooper();
        return mainLooper != null ? new Handler(mainLooper) : null;
    }
}
