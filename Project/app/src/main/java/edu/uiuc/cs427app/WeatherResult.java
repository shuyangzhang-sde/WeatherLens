package edu.uiuc.cs427app;

public final class WeatherResult {

    private final WeatherData data;
    private final String errorMessage;

    private WeatherResult(WeatherData data, String errorMessage) {
        this.data = data;
        this.errorMessage = errorMessage;
    }

    public static WeatherResult ok(WeatherData data) {
        return new WeatherResult(data, null);
    }

    public static WeatherResult error(String message) {
        return new WeatherResult(null, message);
    }

    public boolean isOk() {
        return data != null;
    }

    public WeatherData getData() {
        return data;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
