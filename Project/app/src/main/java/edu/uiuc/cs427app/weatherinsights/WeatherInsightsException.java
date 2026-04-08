package edu.uiuc.cs427app.weatherinsights;

/**
 * Checked exception used for recoverable Weather Insights generation failures.
 */
public class WeatherInsightsException extends Exception {

    /**
     * Creates an exception with a message suitable for logs/UI handling.
     */
    public WeatherInsightsException(String message) {
        super(message);
    }

    /**
     * Creates an exception with a message and root cause.
     */
    public WeatherInsightsException(String message, Throwable cause) {
        super(message, cause);
    }
}

