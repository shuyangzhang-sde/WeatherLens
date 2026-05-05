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

    /**
     * Ran before as setup based on prompt
     */
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

    private ActivityScenario<WeatherInsightsActivity> scenario;

    /**
     * Ran to cleanup after tests
     */
    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
            scenario = null;
        }

        WeatherInsightsActivity.clearInsightsTextGeneratorForTesting();
    }

    /**
     * Tests if generated answer from llm is shown after click
     */
    @Test
    public void selectingSuggestedQuestionShowsGeneratedAnswer() throws InterruptedException {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                WeatherInsightsActivity.class);
        intent.putExtra(WeatherInsightsActivity.EXTRA_CITY_NAME, "Chicago");
        intent.putExtra(WeatherInsightsActivity.EXTRA_WEATHER_JSON, SAMPLE_WEATHER_JSON);

        scenario = ActivityScenario.launch(intent);

        onView(withText("Do I need an umbrella today?")).check(matches(isDisplayed()));
        onView(withText("Is it comfortable for outdoor exercise?")).check(matches(isDisplayed()));

        onView(withText("Do I need an umbrella today?")).perform(click());
        Thread.sleep(500);
        onView(withId(R.id.weatherInsightsAnswer))
                .check(matches(withText(containsString("umbrella"))));
    }
}

