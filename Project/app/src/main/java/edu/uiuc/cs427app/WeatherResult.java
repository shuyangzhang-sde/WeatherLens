package edu.uiuc.cs427app;

/**
 * Outcome of a weather HTTP request: either parsed weather in a WeatherData instance,
 * or a plain-text error message. Failed requests never invent fake temperature or conditions.
 */
public final class WeatherResult {

    private final WeatherData data;
    private final String errorMessage;

    /**
     * Builds a result directly. Callers normally use the static ok or error factory methods
     * instead of invoking this constructor.
     *
     * @param data          parsed weather on success, or null when the request failed
     * @param errorMessage  human-readable failure text, or null when the request succeeded
     */
    private WeatherResult(WeatherData data, String errorMessage) {
        this.data = data;
        this.errorMessage = errorMessage;
    }

    /**
     * Wraps a successful parse. The data argument must not be null for a meaningful success.
     *
     * @param data non-null weather payload from the repository
     * @return a result whose isOk method returns true and whose getData method returns that payload
     */
    public static WeatherResult ok(WeatherData data) {
        return new WeatherResult(data, null);
    }

    /**
     * Builds a failure result with no weather payload.
     *
     * @param message user-visible explanation of what went wrong
     * @return a result with null getData and the given message from getErrorMessage
     */
    public static WeatherResult error(String message) {
        return new WeatherResult(null, message);
    }

    /**
     * @return true when getData would return a non-null WeatherData instance
     */
    public boolean isOk() {
        return data != null;
    }

    /**
     * @return the parsed WeatherData on success, or null after a failed fetch or parse
     */
    public WeatherData getData() {
        return data;
    }

    /**
     * @return the error text after failure, or null when isOk is true
     */
    public String getErrorMessage() {
        return errorMessage;
    }
}
