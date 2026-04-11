package edu.uiuc.cs427app;

/**
 * Immutable value object holding parsed weather fields for display on
 * {@link WeatherActivity}, together with the original JSON response body for use
 * by {@link WeatherInsightsActivity}.
 */
public final class WeatherData {

    private final String cityDisplayName;
    private final String dateTimeLine;
    private final String temperatureLine;
    private final String conditionLabel;
    private final String humidityLine;
    private final String windLine;
    private final String rawApiJson;

    /**
     * Constructs a snapshot of weather information and preserves the provider response.
     *
     * @param cityDisplayName  resolved city label for presentation
     * @param dateTimeLine     formatted local date and time for the location
     * @param temperatureLine  temperature text including units
     * @param conditionLabel   textual description of conditions (for example, clear sky)
     * @param humidityLine     relative humidity text for the user interface
     * @param windLine         wind speed and direction summary
     * @param rawApiJson       complete JSON document returned by the weather service
     */
    public WeatherData(
            String cityDisplayName,
            String dateTimeLine,
            String temperatureLine,
            String conditionLabel,
            String humidityLine,
            String windLine,
            String rawApiJson) {
        this.cityDisplayName = cityDisplayName;
        this.dateTimeLine = dateTimeLine;
        this.temperatureLine = temperatureLine;
        this.conditionLabel = conditionLabel;
        this.humidityLine = humidityLine;
        this.windLine = windLine;
        this.rawApiJson = rawApiJson;
    }

    /**
     * @return the city name returned by the weather provider, or a fallback when absent
     */
    public String getCityDisplayName() {
        return cityDisplayName;
    }

    /**
     * @return formatted local time for the city, derived from API timestamp and timezone offset
     */
    public String getDateTimeLine() {
        return dateTimeLine;
    }

    /**
     * @return temperature string including metric units
     */
    public String getTemperatureLine() {
        return temperatureLine;
    }

    /**
     * @return short natural-language description of current conditions
     */
    public String getConditionLabel() {
        return conditionLabel;
    }

    /**
     * @return humidity text suitable for display (numeric percentage or placeholder)
     */
    public String getHumidityLine() {
        return humidityLine;
    }

    /**
     * @return wind speed and optional compass direction text
     */
    public String getWindLine() {
        return windLine;
    }

    /**
     * @return unmodified JSON response from the weather API for downstream processing
     */
    public String getRawApiJson() {
        return rawApiJson;
    }
}
