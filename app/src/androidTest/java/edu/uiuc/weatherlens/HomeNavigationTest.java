package edu.uiuc.weatherlens;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import edu.uiuc.weatherlens.theme.ThemeContract;
import edu.uiuc.weatherlens.theme.ThemeDefaults;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/** Live integration coverage without clearing existing users or their cities. */
@RunWith(AndroidJUnit4.class)
public class HomeNavigationTest {
    private interface Condition { boolean ready(MainActivity activity); }
    private void await(ActivityScenario<MainActivity> scenario, Condition condition) {
        long deadline = SystemClock.elapsedRealtime() + 60000;
        AtomicBoolean ready = new AtomicBoolean();
        while (SystemClock.elapsedRealtime() < deadline) {
            scenario.onActivity(activity -> ready.set(condition.ready(activity)));
            if (ready.get()) return;
            SystemClock.sleep(150);
        }
        fail("Timed out waiting for live weather or forecast. Check API credentials and connectivity.");
    }
    private void capture(Context context, String name) {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        device.waitForIdle();
        if ("true".equals(InstrumentationRegistry.getArguments().getString("keepPreview"))) {
            // Let transient install/launcher toasts disappear in human-facing previews.
            SystemClock.sleep(4000);
        }
        assertTrue(device.takeScreenshot(new File(context.getExternalFilesDir(null), name + ".png")));
    }
    private boolean forecastReady(MainActivity activity) {
        return activity.findViewById(R.id.forecastDailyRows) != null
                && ((LinearLayout) activity.findViewById(R.id.forecastDailyRows)).getChildCount() == 13
                && activity.findViewById(R.id.forecastProgress).getVisibility() == View.GONE;
    }

    @Test public void tabsUseRealForecastAndShareTheSelectedCity() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        DatabaseHelper database = new DatabaseHelper(context);
        boolean keepPreview = "true".equals(InstrumentationRegistry.getArguments().getString("keepPreview"));
        String user = "Explorer";
        if (database.userExists(user)) user += " " + System.currentTimeMillis();
        assertTrue(database.addUser(user, UUID.randomUUID().toString(), "warm cream and pink"));
        database.updateThemeJson(user, ThemeDefaults.defaultThemeJson());
        assertTrue(database.addCityForUser(user, "Champaign"));
        assertTrue(database.addCityForUser(user, "Chicago"));
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("username", user);
        intent.putExtra(ThemeContract.EXTRA_THEME_PROMPT, "warm cream and pink");
        intent.putExtra(ThemeContract.EXTRA_THEME_JSON, ThemeDefaults.defaultThemeJson());
        ActivityScenario<MainActivity> scenario = null;
        boolean completed = false;
        try {
            scenario = ActivityScenario.launch(intent);
            onView(withId(R.id.navToday)).check(matches(isDisplayed()));
            await(scenario, activity -> activity.findViewById(R.id.weatherProgress).getVisibility() == View.GONE
                    && !((TextView) activity.findViewById(R.id.weatherTemperature)).getText().toString().equals("—"));
            onView(withId(R.id.weatherCityName)).check(matches(withText("Champaign")));
            capture(context, "tabs-today");
            onView(withId(R.id.navForecast)).perform(click());
            await(scenario, this::forecastReady);
            scenario.onActivity(activity -> {
                assertTrue(((LinearLayout) activity.findViewById(R.id.forecastHours)).getChildCount() > 0);
                assertEquals("Seven days with six separators", 13,
                        ((LinearLayout) activity.findViewById(R.id.forecastDailyRows)).getChildCount());
            });
            capture(context, "tabs-forecast");
            onView(withId(R.id.forecastScroll)).perform(androidx.test.espresso.action.ViewActions.swipeUp());
            capture(context, "tabs-forecast-week");
            scenario.onActivity(activity -> ((android.widget.ScrollView) activity.findViewById(R.id.forecastScroll)).scrollTo(0, 0));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            onView(withId(R.id.forecastHourlyScroll)).perform(androidx.test.espresso.action.ViewActions.swipeLeft());
            scenario.onActivity(activity -> assertTrue("Hourly temperatures scroll horizontally",
                    activity.findViewById(R.id.forecastHourlyScroll).getScrollX() > 0));
            scenario.recreate();
            await(scenario, this::forecastReady);
            onView(withId(R.id.citySelector)).perform(click());
            capture(context, "city-dropdown-forecast");
            onData(is("Chicago")).perform(click());
            await(scenario, this::forecastReady);
            onView(withId(R.id.citySelector)).check(matches(withSpinnerText("Chicago")));
            onView(withId(R.id.navToday)).perform(click());
            onView(withId(R.id.weatherCityName)).check(matches(withText("Chicago")));
            capture(context, "tabs-today-chicago");
            scenario.onActivity(activity -> {
                Spinner selector = activity.findViewById(R.id.citySelector);
                assertEquals("Chicago", selector.getSelectedItem());
                assertEquals("City name should fit without truncation", 0,
                        ((TextView) selector.getSelectedView()).getLayout().getEllipsisCount(0));
                assertSame(activity.findViewById(R.id.todayCitySelectorSlot), selector.getParent().getParent());
            });
            onView(withId(R.id.citySelector)).perform(click());
            capture(context, "city-dropdown-today");
            onData(is("Champaign")).perform(click());
            await(scenario, activity -> activity.findViewById(R.id.weatherProgress).getVisibility() == View.GONE);
            onView(withId(R.id.navForecast)).perform(click());
            await(scenario, this::forecastReady);
            onView(withId(R.id.citySelector)).check(matches(withSpinnerText("Champaign")));
            scenario.onActivity(activity -> {
                Spinner selector = activity.findViewById(R.id.citySelector);
                assertEquals("Champaign", selector.getSelectedItem());
                assertEquals("City name should fit without truncation", 0,
                        ((TextView) selector.getSelectedView()).getLayout().getEllipsisCount(0));
                assertSame(activity.findViewById(R.id.forecastCitySelectorSlot), selector.getParent().getParent());
            });
            onView(withId(R.id.citySelector)).perform(click());
            onData(is("Chicago")).perform(click());
            await(scenario, this::forecastReady);
            capture(context, "tabs-forecast-chicago");
            onView(withId(R.id.navCities)).perform(click());
            onView(withContentDescription("Remove Chicago")).check(matches(isDisplayed()));
            capture(context, "tabs-cities");
            if (!keepPreview) {
                onView(withContentDescription("Remove Chicago")).perform(click());
                assertFalse(database.cityExistsForUser(user, "Chicago"));
                onView(withId(R.id.navToday)).perform(click());
                await(scenario, activity -> activity.findViewById(R.id.weatherProgress).getVisibility() == View.GONE);
                onView(withId(R.id.weatherCityName)).check(matches(withText("Champaign")));
                onView(withId(R.id.navCities)).perform(click());
                onView(withContentDescription("Remove Champaign")).perform(click());
                onView(withId(R.id.navForecast)).perform(click());
                scenario.onActivity(activity -> assertEquals(0,
                        ((LinearLayout) activity.findViewById(R.id.forecastPeriods)).getChildCount()));
                onView(withId(R.id.homeMenu)).perform(click());
                onView(withText("Log out")).perform(click());
                onView(withId(R.id.editTextUsername)).check(matches(isDisplayed()));
            }
            completed = true;
            Bundle result = new Bundle();
            result.putString("preview_user", user);
            InstrumentationRegistry.getInstrumentation().sendStatus(0, result);
        } finally {
            if (scenario != null) scenario.close();
            if (!keepPreview || !completed) {
                database.getWritableDatabase().delete("cities", "username = ?", new String[]{user});
                database.getWritableDatabase().delete("users", "username = ?", new String[]{user});
                context.getSharedPreferences("weatherlens", Context.MODE_PRIVATE).edit().remove("city:" + user).apply();
            }
            database.close();
        }
    }
}
