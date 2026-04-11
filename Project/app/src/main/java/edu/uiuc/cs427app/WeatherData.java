package edu.uiuc.cs427app;

public final class WeatherData {

    private final String cityDisplayName;
    private final String dateTimeLine;
    private final String temperatureLine;
    private final String conditionLabel;
    private final String humidityLine;
    private final String windLine;
    private final String rawApiJson;

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

    public String getCityDisplayName() {
        return cityDisplayName;
    }

    public String getDateTimeLine() {
        return dateTimeLine;
    }

    public String getTemperatureLine() {
        return temperatureLine;
    }

    public String getConditionLabel() {
        return conditionLabel;
    }

    public String getHumidityLine() {
        return humidityLine;
    }

    public String getWindLine() {
        return windLine;
    }

    public String getRawApiJson() {
        return rawApiJson;
    }
}
