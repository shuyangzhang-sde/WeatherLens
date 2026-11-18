package edu.uiuc.weatherlens.forecast;

import org.json.JSONArray;
import org.json.JSONObject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Hourly and daily predictions in the forecast location's actual time zone. */
public final class ForecastData {
    public static final class Slot {
        public final LocalDateTime localTime;
        public final double temperature;
        public final boolean isDay;
        public final String condition;
        public final double rainProbability;
        private Slot(LocalDateTime time, double temperature, int code, double rain, boolean isDay) {
            this.localTime = time;
            this.temperature = temperature;
            this.isDay = isDay;
            this.condition = condition(code);
            this.rainProbability = rain;
        }
    }

    public static final class Day {
        public final LocalDate date;
        public final double low;
        public final double high;
        public final String condition;
        public final double rainProbability;
        private Day(LocalDate date, double low, double high, int code, double rain) {
            this.date = date;
            this.low = low;
            this.high = high;
            this.condition = condition(code);
            this.rainProbability = rain;
        }
    }

    public final ZoneId timezone;
    public final List<Slot> hourly;
    public final List<Day> daily;

    private ForecastData(ZoneId timezone, List<Slot> hourly, List<Day> daily) {
        this.timezone = timezone;
        this.hourly = Collections.unmodifiableList(hourly);
        this.daily = Collections.unmodifiableList(daily);
    }

    public static ForecastData parse(String body) throws Exception {
        JSONObject root = new JSONObject(body);
        ZoneId zone = ZoneId.of(root.getString("timezone"));
        JSONObject hours = root.getJSONObject("hourly");
        JSONArray times = hours.getJSONArray("time");
        List<Slot> hourly = new ArrayList<>();
        for (int i = 0; i < times.length(); i++) {
            hourly.add(new Slot(LocalDateTime.parse(times.getString(i)),
                    temperature(hours.getJSONArray("temperature_2m"), i),
                    hours.getJSONArray("weather_code").getInt(i),
                    probability(hours.optJSONArray("precipitation_probability"), i),
                    hours.getJSONArray("is_day").getInt(i) == 1));
        }
        hourly.sort((a, b) -> a.localTime.compareTo(b.localTime));
        JSONObject days = root.getJSONObject("daily");
        JSONArray dates = days.getJSONArray("time");
        List<Day> daily = new ArrayList<>();
        for (int i = 0; i < dates.length(); i++) {
            double low = temperature(days.getJSONArray("temperature_2m_min"), i);
            double high = temperature(days.getJSONArray("temperature_2m_max"), i);
            if (low > high) throw new IllegalArgumentException("Invalid daily temperature range.");
            daily.add(new Day(LocalDate.parse(dates.getString(i)), low, high,
                    days.getJSONArray("weather_code").getInt(i),
                    probability(days.optJSONArray("precipitation_probability_max"), i)));
        }
        daily.sort((a, b) -> a.date.compareTo(b.date));
        if (daily.isEmpty() || hourly.isEmpty()) throw new IllegalArgumentException("No forecast was returned.");
        return new ForecastData(zone, hourly, daily);
    }

    private static double temperature(JSONArray values, int index) throws Exception {
        double value = values.getDouble(index);
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Invalid forecast temperature.");
        return value;
    }

    private static double probability(JSONArray values, int index) {
        double value = values == null ? Double.NaN : values.optDouble(index, Double.NaN);
        return Double.isFinite(value) && value >= 0 && value <= 100 ? value / 100 : Double.NaN;
    }

    private static String condition(int code) {
        switch (code) {
            case 0: return "Clear sky";
            case 1: return "Mostly clear";
            case 2: return "Partly cloudy";
            case 3: return "Overcast";
            case 45: case 48: return "Fog";
            case 51: case 53: case 55: return "Drizzle";
            case 56: case 57: return "Freezing drizzle";
            case 61: case 63: case 65: return "Rain";
            case 66: case 67: return "Freezing rain";
            case 71: case 73: case 75: case 77: return "Snow";
            case 80: case 81: case 82: return "Rain showers";
            case 85: case 86: return "Snow showers";
            case 95: case 96: case 99: return "Thunderstorm";
            default: return "Weather unavailable";
        }
    }
}
