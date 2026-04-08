package edu.uiuc.cs427app.weatherinsights;

import java.util.List;

/**
 * Service abstraction for Weather Insights generation.
 */
public interface WeatherInsightsRepository {

    /**
     * Generates 2-4 weather-specific suggested questions from current weather data.
     */
    List<String> generateQuestions(WeatherData weatherData) throws WeatherInsightsException;

    /**
     * Generates an answer for a selected question using only the provided weather data.
     */
    String generateAnswer(WeatherData weatherData, String question) throws WeatherInsightsException;

    /**
     * Async wrapper for question generation.
     *
     * Work runs off the main thread; callback methods are delivered on the main thread.
     */
    void generateQuestionsAsync(WeatherData weatherData, WeatherInsightsCallback<List<String>> callback);

    /**
     * Async wrapper for answer generation.
     *
     * Work runs off the main thread; callback methods are delivered on the main thread.
     */
    void generateAnswerAsync(
            WeatherData weatherData,
            String question,
            WeatherInsightsCallback<String> callback
    );

    /**
     * Shuts down background resources held by this repository.
     */
    void shutdown();
}
