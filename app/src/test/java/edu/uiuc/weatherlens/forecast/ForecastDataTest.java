package edu.uiuc.weatherlens.forecast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.time.LocalDate;
import java.time.ZoneId;
import static org.junit.Assert.*;

public class ForecastDataTest {
    private JSONObject response() throws Exception {
        return new JSONObject().put("timezone", "America/Chicago")
                .put("hourly", new JSONObject()
                        .put("time", new JSONArray().put("2026-11-18T23:00").put("2026-11-19T00:00"))
                        .put("temperature_2m", new JSONArray().put(4).put(2))
                        .put("weather_code", new JSONArray().put(0).put(61))
                        .put("is_day", new JSONArray().put(0).put(0))
                        .put("precipitation_probability", new JSONArray().put(0).put(40)))
                .put("daily", new JSONObject()
                        .put("time", new JSONArray().put("2026-11-18").put("2026-11-19"))
                        .put("temperature_2m_min", new JSONArray().put(-2).put(0))
                        .put("temperature_2m_max", new JSONArray().put(10).put(8))
                        .put("weather_code", new JSONArray().put(0).put(61))
                        .put("precipitation_probability_max", new JSONArray().put(0).put(70)));
    }

    @Test public void preservesLocalMidnightAndDailyExtremes() throws Exception {
        ForecastData data = ForecastData.parse(response().toString());
        assertEquals(ZoneId.of("America/Chicago"), data.timezone);
        assertFalse(data.hourly.get(0).isDay);
        assertEquals(LocalDate.of(2026, 11, 18), data.hourly.get(0).localTime.toLocalDate());
        assertEquals(LocalDate.of(2026, 11, 19), data.hourly.get(1).localTime.toLocalDate());
        assertEquals(-2, data.daily.get(0).low, 0.001);
        assertEquals(10, data.daily.get(0).high, 0.001);
        assertEquals(0.4, data.hourly.get(1).rainProbability, 0.001);
        assertEquals(0.7, data.daily.get(1).rainProbability, 0.001);
        assertEquals("Rain", data.daily.get(1).condition);
    }

    @Test public void missingOrInvalidRainIsUnavailableRatherThanZero() throws Exception {
        JSONObject response = response();
        response.getJSONObject("hourly").remove("precipitation_probability");
        response.getJSONObject("daily").getJSONArray("precipitation_probability_max").put(0, 110);
        ForecastData data = ForecastData.parse(response.toString());
        assertTrue(Double.isNaN(data.hourly.get(0).rainProbability));
        assertTrue(Double.isNaN(data.daily.get(0).rainProbability));
    }

    @Test public void incompleteTemperatureArraysAreRejected() throws Exception {
        JSONObject response = response();
        response.getJSONObject("hourly").put("temperature_2m", new JSONArray().put(2));
        try { ForecastData.parse(response.toString()); fail("Missing temperature must not become zero"); }
        catch (org.json.JSONException expected) { }
    }

    @Test(expected = IllegalArgumentException.class) public void invertedDailyRangeIsRejected() throws Exception {
        JSONObject response = response();
        response.getJSONObject("daily").getJSONArray("temperature_2m_min").put(0, 20);
        ForecastData.parse(response.toString());
    }

    @Test public void sevenDailyEntriesAreRetained() throws Exception {
        JSONObject response = response();
        JSONObject daily = response.getJSONObject("daily");
        for (int i = 2; i < 7; i++) {
            daily.getJSONArray("time").put(LocalDate.of(2026, 11, 18).plusDays(i).toString());
            daily.getJSONArray("temperature_2m_min").put(-i);
            daily.getJSONArray("temperature_2m_max").put(i);
            daily.getJSONArray("weather_code").put(3);
            daily.getJSONArray("precipitation_probability_max").put(20);
        }
        assertEquals(7, ForecastData.parse(response.toString()).daily.size());
    }
}
