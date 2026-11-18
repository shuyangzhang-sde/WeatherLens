package edu.uiuc.weatherlens.forecast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Seven-day Open-Meteo forecast. Call on a worker thread. */
public final class ForecastRepository {
    private ForecastRepository() { }

    public static ForecastData fetch(double latitude, double longitude) throws Exception {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                || Math.abs(latitude) > 90 || Math.abs(longitude) > 180) {
            throw new IllegalArgumentException("City coordinates are invalid.");
        }
        URL url = new URL("https://api.open-meteo.com/v1/forecast?latitude=" + latitude
                + "&longitude=" + longitude + "&timezone=auto&forecast_days=7"
                + "&hourly=temperature_2m,weather_code,precipitation_probability,is_day"
                + "&daily=temperature_2m_max,temperature_2m_min,weather_code,precipitation_probability_max");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(20000);
        try {
            int status = connection.getResponseCode();
            if (status != 200) {
                // Do not include request URLs or provider response bodies containing credentials.
                if (status == 429) throw new Exception("Weather service limit reached. Please try again later.");
                throw new Exception("Forecast service returned HTTP " + status + ".");
            }
            StringBuilder body = new StringBuilder();
            try (InputStream stream = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) body.append(line);
            }
            return ForecastData.parse(body.toString());
        } finally {
            connection.disconnect();
        }
    }
}
