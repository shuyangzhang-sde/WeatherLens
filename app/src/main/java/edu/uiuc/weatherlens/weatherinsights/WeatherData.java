package edu.uiuc.weatherlens.weatherinsights;

/**
 * Immutable weather snapshot passed from UI into Weather Insights generation.
 */
public final class WeatherData {

    private final String city;
    private final String time;
    private final String temperature;
    private final String condition;
    private final String humidity;
    private final String wind;

    /**
     * Creates a normalized weather payload for LLM prompts.
     */
    public WeatherData(
            String city,
            String time,
            String temperature,
            String condition,
            String humidity,
            String wind
    ) {
        this.city = sanitize(city);
        this.time = sanitize(time);
        this.temperature = sanitize(temperature);
        this.condition = sanitize(condition);
        this.humidity = sanitize(humidity);
        this.wind = sanitize(wind);
    }

    /**
     * Returns the selected city name.
     */
    public String getCity() {
        return city;
    }

    /**
     * Returns the weather observation time string.
     */
    public String getTime() {
        return time;
    }

    /**
     * Returns the temperature string (including units if provided).
     */
    public String getTemperature() {
        return temperature;
    }

    /**
     * Returns the weather condition summary.
     */
    public String getCondition() {
        return condition;
    }

    /**
     * Returns the humidity string.
     */
    public String getHumidity() {
        return humidity;
    }

    /**
     * Returns the wind string.
     */
    public String getWind() {
        return wind;
    }

    /**
     * Returns true when all required weather fields are non-empty.
     */
    public boolean isComplete() {
        return !city.isEmpty()
                && !time.isEmpty()
                && !temperature.isEmpty()
                && !condition.isEmpty()
                && !humidity.isEmpty()
                && !wind.isEmpty();
    }

    /**
     * Formats weather data as prompt-friendly key/value lines.
     */
    public String toPromptBlock() {
        return "city: " + city + "\n"
                + "time: " + time + "\n"
                + "temperature: " + temperature + "\n"
                + "condition: " + condition + "\n"
                + "humidity: " + humidity + "\n"
                + "wind: " + wind;
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.trim();
    }
}

