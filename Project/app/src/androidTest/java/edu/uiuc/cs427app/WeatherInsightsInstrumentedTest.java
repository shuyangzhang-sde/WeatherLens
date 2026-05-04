package edu.uiuc.cs427app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Espresso instrumented tests for Weather Insights (Iteration 5).
 *
 * <p>Launches {@link WeatherInsightsActivity} with explicit intent extras because the activity may
 * not yet be reachable from the rest of the app. Assertions check behavior only (buttons appear,
 * answer text updates), not exact LLM wording.
 *
 * <p><b>Team reminders (manual coordination):</b> assign owners for the JaCoCo coverage PR, the
 * single mocking-location test, and the two-city live-weather tests; record the full-suite
 * passing video and submit the default-branch commit SHA per course instructions.
 *
 * <p><b>Fixture:</b> {@link #FIXTURE_WEATHER_JSON} is minimal JSON passed to Gemini as weather
 * context; keep it stable so prompts and latency stay predictable.
 */
@RunWith(AndroidJUnit4.class)
public class WeatherInsightsInstrumentedTest {

    /** City label sent to Gemini; stable for reproducibility. */
    private static final String FIXTURE_CITY_NAME = "Champaign";

    /**
     * Minimal JSON treated as ground-truth weather context for insights. Not validated by the app
     * beyond non-empty; structured so Gemini returns usable suggested questions.
     */
    private static final String FIXTURE_WEATHER_JSON =
            "{\"location\":\"Champaign\",\"current\":{"
                    + "\"temperature_c\":21,"
                    + "\"condition\":\"partly cloudy\","
                    + "\"humidity_percent\":55,"
                    + "\"wind_kph\":12"
                    + "}}";

    private ActivityScenario<WeatherInsightsActivity> scenario;

    @Before
    public void setUp() {
        assertTrue(
                "Set GEMINI_KEY in Project/gradle.properties for Weather Insight tests (real Gemini calls).",
                BuildConfig.GEMINI_KEY != null
                        && !BuildConfig.GEMINI_KEY.trim().isEmpty()
                        && !BuildConfig.GEMINI_KEY.contains("API KEY HERE"));

        Intent intent =
                new Intent(
                        ApplicationProvider.getApplicationContext(),
                        WeatherInsightsActivity.class);
        intent.putExtra(WeatherInsightsActivity.EXTRA_CITY_NAME, FIXTURE_CITY_NAME);
        intent.putExtra(WeatherInsightsActivity.EXTRA_WEATHER_JSON, FIXTURE_WEATHER_JSON);
        scenario = ActivityScenario.launch(intent);
    }

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
            scenario = null;
        }
    }

    @Test
    public void testWeatherInsightLoadsAtLeastTwoQuestionButtons() throws InterruptedException {
        waitUntilQuestionButtonsShown(120_000L);
        Thread.sleep(400);
        onView(withId(R.id.weatherInsightsQuestionContainer))
                .check(matches(withMinimumChildCount(2)));
    }

    @Test
    public void testWeatherInsightShowsAnswerAfterQuestionClick() throws InterruptedException {
        waitUntilQuestionButtonsShown(120_000L);
        Thread.sleep(400);

        String before = getAnswerText();
        assertNotNull(before);

        onView(withId(R.id.weatherInsightsQuestionContainer)).perform(clickFirstButtonChild());

        Thread.sleep(500);
        waitUntilAnswerNotEqualTo(before, 120_000L);
        Thread.sleep(400);

        String after = getAnswerText();
        assertNotNull(after);
        assertTrue("Answer area should show non-empty text after a question tap.", after.trim().length() > 0);
    }

    @Test
    public void testWeatherInsightAnswerChangesWhenSecondQuestionClicked()
            throws InterruptedException {
        waitUntilQuestionButtonsShown(120_000L);
        Thread.sleep(400);

        onView(withId(R.id.weatherInsightsQuestionContainer)).perform(clickFirstButtonChild());
        Thread.sleep(500);
        waitUntilAnswerNonEmpty(120_000L);
        String afterFirst = getAnswerText();
        assertNotNull(afterFirst);

        onView(withId(R.id.weatherInsightsQuestionContainer)).perform(clickSecondButtonChild());
        Thread.sleep(500);
        waitUntilAnswerChangedFrom(afterFirst, 120_000L);
        Thread.sleep(400);

        String afterSecond = getAnswerText();
        assertNotNull(afterSecond);
        assertNotEquals(
                "Tapping a second question should change the displayed answer.", afterFirst, afterSecond);
    }

    private void waitUntilQuestionButtonsShown(long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (questionButtonCount() >= 2) {
                return;
            }
            Thread.sleep(500);
        }
        throw new AssertionError(
                "Timed out waiting for at least two question buttons (check network and GEMINI_KEY).");
    }

    private void waitUntilAnswerNonEmpty(long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            String t = getAnswerText();
            if (t != null && t.trim().length() > 0) {
                return;
            }
            Thread.sleep(400);
        }
        throw new AssertionError("Timed out waiting for non-empty answer text.");
    }

    private void waitUntilAnswerNotEqualTo(String previous, long timeoutMs)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            String t = getAnswerText();
            if (t != null && !t.equals(previous) && t.trim().length() > 0) {
                return;
            }
            Thread.sleep(400);
        }
        throw new AssertionError("Timed out waiting for answer to update after question click.");
    }

    private void waitUntilAnswerChangedFrom(String previous, long timeoutMs)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            String t = getAnswerText();
            if (t != null && !t.equals(previous)) {
                return;
            }
            Thread.sleep(400);
        }
        throw new AssertionError("Timed out waiting for answer to change after second question.");
    }

    private int questionButtonCount() {
        final int[] count = {0};
        scenario.onActivity(
                activity -> {
                    ViewGroup container =
                            activity.findViewById(R.id.weatherInsightsQuestionContainer);
                    if (container == null) {
                        return;
                    }
                    int n = 0;
                    for (int i = 0; i < container.getChildCount(); i++) {
                        if (container.getChildAt(i) instanceof Button) {
                            n++;
                        }
                    }
                    count[0] = n;
                });
        return count[0];
    }

    private String getAnswerText() {
        final String[] holder = {null};
        scenario.onActivity(
                activity -> {
                    TextView answer = activity.findViewById(R.id.weatherInsightsAnswer);
                    holder[0] = answer != null ? answer.getText().toString() : null;
                });
        return holder[0];
    }

    private static Matcher<View> withMinimumChildCount(int min) {
        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(org.hamcrest.Description description) {
                description.appendText("ViewGroup with at least " + min + " children");
            }

            @Override
            protected boolean matchesSafely(View view) {
                if (!(view instanceof ViewGroup)) {
                    return false;
                }
                return ((ViewGroup) view).getChildCount() >= min;
            }
        };
    }

    private static ViewAction clickFirstButtonChild() {
        return clickNthButtonChild(0);
    }

    private static ViewAction clickSecondButtonChild() {
        return clickNthButtonChild(1);
    }

    /**
     * Clicks the n-th direct child {@link Button} inside the question container (0-based among
     * buttons only).
     */
    private static ViewAction clickNthButtonChild(int buttonIndex) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return ViewMatchers.isAssignableFrom(ViewGroup.class);
            }

            @Override
            public String getDescription() {
                return "click button child index " + buttonIndex;
            }

            @Override
            public void perform(UiController uiController, View view) {
                ViewGroup group = (ViewGroup) view;
                int seen = 0;
                for (int i = 0; i < group.getChildCount(); i++) {
                    View child = group.getChildAt(i);
                    if (child instanceof Button) {
                        if (seen == buttonIndex) {
                            child.performClick();
                            return;
                        }
                        seen++;
                    }
                }
                throw new IllegalStateException(
                        "No button at index " + buttonIndex + " in question container");
            }
        };
    }
}
