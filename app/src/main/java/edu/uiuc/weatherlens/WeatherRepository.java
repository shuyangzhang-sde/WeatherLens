package edu.uiuc.weatherlens;

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
 * Fetches current weather from the OpenWeatherMap HTTP API using the same kind of
 * city name string the app stores for each user. Requests use HttpURLConnection, in
 * line with how other network code in the project is written.
 */
public final class WeatherRepository {

    private static final String OPEN_WEATHER_ENDPOINT =
            "https://api.openweathermap.org/data/2.5/weather";

    /**
     * Hides the default constructor; callers should use the static fetch method only.
     */
    private WeatherRepository() {
    }

    /**
     * Looks up current weather for the given city text and API key. The call blocks until
     * the HTTP request finishes and the body is parsed, so it should run on a background
     * thread, not the UI thread. On success you get a WeatherResult carrying WeatherData;
     * on failure the result carries an error message instead of invented numbers.
     *
     * @param cityQuery free-text location query, typically the city string stored for the user
     * @param apiKey    OpenWeatherMap API key supplied from build configuration, not hardcoded in source
     * @return a WeatherResult containing parsed data on success, or an error description on failure
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
     * For client or server error status codes the body usually sits on the error stream;
     * otherwise the normal input stream is used. Throws if the connection cannot supply a stream.
     *
     * @param connection open HTTP connection for the weather request
     * @param code         HTTP response status code
     * @return the input stream that should be read for the response body
     * @throws Exception if the connection cannot provide a response stream
     */
    private static InputStream selectStream(HttpURLConnection connection, int code) throws Exception {
        if (code >= 400) {
            InputStream err = connection.getErrorStream();
            return err != null ? err : connection.getInputStream();
        }
        return connection.getInputStream();
    }

    /**
     * Reads the whole response as UTF-8 text. A null stream yields an empty string rather than failing.
     * Any I/O problem while reading is thrown to the caller.
     *
     * @param stream response body stream, or null
     * @return full body as one string, or an empty string when the stream is null
     * @throws Exception if reading or closing the stream fails
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
     * Turns a failed HTTP status into a short message for the user. When the JSON body
     * includes a top-level "message" string from OpenWeatherMap, that text is included;
     * otherwise the method falls back to a generic line that names the status code.
     *
     * @param code HTTP status code from the failed response
     * @param body raw response body text, if any
     * @return a concise, user-facing error message
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
            // If the error body is not JSON, ignore and use the generic message below.
        }
        return "Weather service returned HTTP " + code + ".";
    }

    /**
     * Expects the JSON from a successful current-weather response. It fills a WeatherData
     * object when temperature, conditions, and related fields are present, and keeps the
     * original JSON string so other screens can reuse it. If a piece is missing, it returns
     * an error WeatherResult instead of guessing values. The fallback city name is used only
     * when the payload does not include a usable "name" field.
     *
     * @param jsonBody          raw JSON response body from a 200 OK response
     * @param fallbackCityName  city label to use when the JSON name field is empty or absent
     * @return a successful WeatherResult with populated WeatherData, or an error WeatherResult
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
     * Formats the observation time in the city's local zone. The API supplies a timezone
     * value as seconds east or west of UTC; together with the Unix observation time in UTC,
     * that yields a civil clock time and offset label for the UI.
     *
     * @param utcEpochSeconds       observation instant as Unix epoch seconds in UTC
     * @param timezoneShiftSeconds  offset from UTC to local civil time, in seconds, as provided by the API
     * @return formatted date, time, and zone offset suitable for display
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
     * Produces a single line for wind: metric speed in meters per second, and when the
     * service sends a direction in degrees, an approximate compass direction such as NE.
     * Missing speed is shown as an em dash; missing direction simply omits the compass part.
     *
     * @param speedMs wind speed in meters per second, or NaN when not provided
     * @param deg     wind direction in degrees clockwise from north, or NaN when not provided
     * @return a single formatted line describing wind speed and optional direction
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
     * Maps a wind direction in degrees, measured clockwise from north, to the nearest
     * of eight compass labels (N, NE, E, and so on).
     *
     * @param degrees wind direction in degrees, clockwise from north
     * @return the nearest compass octant abbreviation
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
     * Tidies a short description from the API by capitalizing each word so it reads cleanly
     * in the weather screen. Null or blank input returns an empty string.
     *
     * @param raw provider-supplied weather description text
     * @return title-cased words, or an empty string when the input is null or blank
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
