package edu.uiuc.cs427app;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;

/**
 * Instrumented coverage for Weather Insights interaction:
 * user taps a suggested question and sees the generated answer text update.
 */
@RunWith(AndroidJUnit4.class)
public class WeatherInsightsActivityTest {

    private static final String SAMPLE_WEATHER_JSON =
            "{"
                    + "\"name\":\"Chicago\","
                    + "\"timezone\":-18000,"
                    + "\"dt\":1711584000,"
                    + "\"main\":{\"temp\":8.3,\"humidity\":72},"
                    + "\"weather\":[{\"main\":\"Rain\",\"description\":\"light rain\"}],"
                    + "\"wind\":{\"speed\":6.2,\"deg\":200}"
                    + "}";

    @Before
    public void setUp() {
        WeatherInsightsActivity.setInsightsTextGeneratorForTesting(prompt -> {
            if (prompt.contains("Respond with ONLY valid JSON")) {
                return "{\"questions\":["
                        + "\"Do I need an umbrella today?\","
                        + "\"Is it comfortable for outdoor exercise?\""
                        + "]}";
            }
            if (prompt.contains("The user asks:")) {
                return "Bring an umbrella today. Rain is present and humidity is high.";
            }
            return "No response";
        });
    }

    @After
    public void tearDown() {
        WeatherInsightsActivity.clearInsightsTextGeneratorForTesting();
    }

    @Test
    public void selectingSuggestedQuestionShowsGeneratedAnswer() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                WeatherInsightsActivity.class);
        intent.putExtra(WeatherInsightsActivity.EXTRA_CITY_NAME, "Chicago");
        intent.putExtra(WeatherInsightsActivity.EXTRA_WEATHER_JSON, SAMPLE_WEATHER_JSON);

        ActivityScenario.launch(intent);

        onView(withText("Do I need an umbrella today?")).check(matches(isDisplayed()));
        onView(withText("Is it comfortable for outdoor exercise?")).check(matches(isDisplayed()));

        onView(withText("Do I need an umbrella today?")).perform(click());
        onView(withId(R.id.weatherInsightsAnswer))
                .check(matches(withText(containsString("umbrella"))));
    }
}

