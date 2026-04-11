package edu.uiuc.cs427app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Provides access to current weather from the OpenWeatherMap API using a textual
 * city query consistent with application storage. Networking is implemented with
 * {@link HttpURLConnection}, consistent with other HTTP usage in this project.
 */
public final class WeatherRepository {

    private static final String OPEN_WEATHER_ENDPOINT =
            "https://api.openweathermap.org/data/2.5/weather";

    /**
     * Private constructor: this type exposes only static methods.
     */
    private WeatherRepository() {
    }

    /**
     * Performs a blocking HTTP GET for the given city and API key, parses JSON on success,
     * and returns either {@link WeatherData} or an error description. This method must not
     * be invoked on the Android main thread.
     *
     * @param cityQuery free-text location query (for example, a name stored in the database)
     * @param apiKey    service credential from build configuration (must not be embedded in source)
     * @return {@link WeatherResult#ok(WeatherData)} or {@link WeatherResult#error(String)}
     */
    public static WeatherResult fetchCurrentWeather(String cityQuery, String apiKey) {
        if (cityQuery == null || cityQuery.trim().isEmpty()) {
            return WeatherResult.error("City name is missing.");
        }
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.contains("{")) {
            return WeatherResult.error("Weather API key is not configured.");
        }
        HttpURLConnection connection = null;
        try {
            String encodedQuery = URLEncoder.encode(cityQuery.trim(), StandardCharsets.UTF_8.name());
            String urlString = OPEN_WEATHER_ENDPOINT
                    + "?q=" + encodedQuery
                    + "&units=metric"
                    + "&appid=" + URLEncoder.encode(apiKey.trim(), StandardCharsets.UTF_8.name());
            connection = (HttpURLConnection) new URL(urlString).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(20_000);
            connection.setReadTimeout(20_000);

            int code = connection.getResponseCode();
            String body = readStreamToString(selectStream(connection, code));
            if (code == HttpURLConnection.HTTP_OK) {
                return parseOpenWeatherJson(body, cityQuery.trim());
            }
            return WeatherResult.error(mapHttpError(code, body));
        } catch (Exception e) {
            return WeatherResult.error("Could not load weather (" + e.getMessage() + ").");
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Selects the appropriate response stream for the HTTP status code.
     *
     * @param connection active connection
     * @param code         HTTP status code
     * @return stream containing the response body
     * @throws Exception if the connection cannot provide a stream
     */
    private static InputStream selectStream(HttpURLConnection connection, int code) throws Exception {
        if (code >= 400) {
            InputStream err = connection.getErrorStream();
            return err != null ? err : connection.getInputStream();
        }
        return connection.getInputStream();
    }

    /**
     * Reads the entire UTF-8 response body into a single string.
     *
     * @param stream response stream (may be {@code null})
     * @return concatenated lines, or an empty string when the stream is {@code null}
     * @throws Exception if reading fails
     */
    private static String readStreamToString(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        for (String line; (line = reader.readLine()) != null; ) {
            builder.append(line);
        }
        reader.close();
        return builder.toString();
    }

    /**
     * Maps a non-success HTTP status to a concise message, including the provider's
     * {@code message} field when present in the error JSON.
     *
     * @param code HTTP status code
     * @param body raw response body
     * @return user-facing error text
     */
    private static String mapHttpError(int code, String body) {
        try {
            if (body != null && !body.isEmpty()) {
                JSONObject o = new JSONObject(body);
                if (o.has("message")) {
                    return "Weather service: " + o.getString("message");
                }
            }
        } catch (Exception ignored) {
            // Ignore parse errors; a generic message is returned below.
        }
        return "Weather service returned HTTP " + code + ".";
    }

    /**
     * Parses a successful OpenWeatherMap JSON payload into {@link WeatherData}.
     *
     * @param jsonBody          response body text
     * @param fallbackCityName  city string used when the {@code name} field is empty
     * @return success with {@link WeatherData}, or failure when required fields are missing
     */
    private static WeatherResult parseOpenWeatherJson(String jsonBody, String fallbackCityName) {
        try {
            JSONObject root = new JSONObject(jsonBody);
            String name = root.optString("name", fallbackCityName);
            if (name.isEmpty()) {
                name = fallbackCityName;
            }

            JSONObject main = root.optJSONObject("main");
            if (main == null) {
                return WeatherResult.error("Weather response was missing temperature data.");
            }

            JSONArray weatherArr = root.optJSONArray("weather");
            if (weatherArr == null || weatherArr.length() == 0) {
                return WeatherResult.error("Weather response was missing conditions.");
            }
            JSONObject weather0 = weatherArr.getJSONObject(0);
            String description = weather0.optString("description", weather0.optString("main", ""));

            JSONObject wind = root.optJSONObject("wind");
            double speedMs = wind != null ? wind.optDouble("speed", Double.NaN) : Double.NaN;
            double deg = wind != null ? wind.optDouble("deg", Double.NaN) : Double.NaN;

            long dtSeconds = root.optLong("dt", System.currentTimeMillis() / 1000L);
            int tzSeconds = root.optInt("timezone", 0);
            String timeLine = formatCityLocalTime(dtSeconds, tzSeconds);

            double temp = main.optDouble("temp", Double.NaN);
            if (Double.isNaN(temp)) {
                return WeatherResult.error("Weather response was missing temperature.");
            }
            String tempLine = String.format(Locale.US, "%.1f °C", temp);

            int humidity = main.optInt("humidity", -1);
            String humidityLine = humidity >= 0 ? humidity + "%" : "—";

            String windLine = formatWind(speedMs, deg);

            WeatherData data = new WeatherData(
                    name,
                    timeLine,
                    tempLine,
                    capitalizeWords(description),
                    humidityLine,
                    windLine,
                    jsonBody);
            return WeatherResult.ok(data);
        } catch (Exception e) {
            return WeatherResult.error("Could not read weather data.");
        }
    }

    /**
     * Formats the observation instant in the city's local offset using the API {@code timezone}
     * field (shift in seconds from UTC).
     *
     * @param utcEpochSeconds        Unix epoch seconds for the observation time (UTC)
     * @param timezoneShiftSeconds   offset from UTC to local civil time, in seconds
     * @return formatted date-time string including offset abbreviation
     */
    private static String formatCityLocalTime(long utcEpochSeconds, int timezoneShiftSeconds) {
        ZoneOffset offset = ZoneOffset.ofTotalSeconds(timezoneShiftSeconds);
        ZoneId zoneId = ZoneId.ofOffset("UTC", offset);
        ZonedDateTime zdt = Instant.ofEpochSecond(utcEpochSeconds).atZone(zoneId);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                "EEEE, MMM d, yyyy HH:mm (XXX)", Locale.US);
        return zdt.format(formatter);
    }

    /**
     * Builds a wind summary using metric wind speed and, when available, an eight-point compass.
     *
     * @param speedMs wind speed in meters per second; {@code NaN} when unknown
     * @param deg     wind direction in degrees; {@code NaN} when unknown
     * @return formatted wind line
     */
    private static String formatWind(double speedMs, double deg) {
        if (Double.isNaN(speedMs)) {
            return "—";
        }
        String speedPart = String.format(Locale.US, "%.1f m/s", speedMs);
        if (Double.isNaN(deg)) {
            return speedPart;
        }
        return speedPart + ", from " + degreesToCompass(deg);
    }

    /**
     * Converts a meteorological wind direction in degrees to the nearest cardinal or intercardinal label.
     *
     * @param degrees direction in degrees (clockwise from north)
     * @return compass abbreviation (for example, {@code NE})
     */
    private static String degreesToCompass(double degrees) {
        String[] dirs = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        int idx = (int) Math.round(degrees / 45.0) % 8;
        if (idx < 0) {
            idx += 8;
        }
        return dirs[idx];
    }

    /**
     * Applies title-style capitalization to a short weather description for display consistency.
     *
     * @param raw provider-supplied description text
     * @return capitalized phrase, or an empty string when input is null or blank
     */
    private static String capitalizeWords(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        String[] parts = raw.trim().split("\\s+");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                out.append(' ');
            }
            String p = parts[i];
            if (p.isEmpty()) {
                continue;
            }
            out.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) {
                out.append(p.substring(1).toLowerCase(Locale.US));
            }
        }
        return out.toString();
    }
}
