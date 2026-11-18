package edu.uiuc.weatherlens;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import edu.uiuc.weatherlens.theme.ThemeContract;
import edu.uiuc.weatherlens.theme.ThemeDefaults;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertTrue;

/** Verifies live city temperatures, removal with Undo, and Forecast detail navigation. */
@RunWith(AndroidJUnit4.class)
public class SavedPlacesPresentationTest {
    @Test
    public void cityOverviewShowsTemperaturesAndKeepsDetailsInForecast() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        DatabaseHelper database = new DatabaseHelper(context);
        String user = "Explorer";
        if (database.userExists(user)) user = "Preview " + System.nanoTime();
        assertTrue(database.addUser(user, java.util.UUID.randomUUID().toString(), "warm cream"));
        ActivityScenario<MainActivity> scenario = null;
        try {
            assertTrue(database.addCityForUser(user, "Champaign"));
            assertTrue(database.addCityForUser(user, "Chicago"));
            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra("username", user);
            intent.putExtra(ThemeContract.EXTRA_THEME_PROMPT, "warm cream");
            intent.putExtra(ThemeContract.EXTRA_THEME_JSON, ThemeDefaults.defaultThemeJson());
            scenario = ActivityScenario.launch(intent);
            onView(withId(R.id.navCities)).perform(click());
            onView(allOf(withText("Champaign"), isDisplayed())).check(matches(isDisplayed()));
            onView(allOf(withText("Map"), isDisplayed())).check(androidx.test.espresso.assertion.ViewAssertions.doesNotExist());
            onView(allOf(withText("Weather"), isDisplayed())).check(androidx.test.espresso.assertion.ViewAssertions.doesNotExist());
            onView(allOf(withContentDescription("Remove Champaign"), hasSibling(withText("Champaign"))))
                    .check(matches(isDisplayed()));
            long deadline = android.os.SystemClock.elapsedRealtime() + 45000;
            java.util.concurrent.atomic.AtomicBoolean loaded = new java.util.concurrent.atomic.AtomicBoolean();
            while (!loaded.get() && android.os.SystemClock.elapsedRealtime() < deadline) {
                scenario.onActivity(activity -> {
                    java.util.ArrayList<View> temperatures = new java.util.ArrayList<>();
                    collectTemperatures(activity.findViewById(R.id.cityContainer), temperatures);
                    loaded.set(temperatures.size() == 2);
                });
                if (!loaded.get()) android.os.SystemClock.sleep(150);
            }
            assertTrue("Each saved city should show a real temperature", loaded.get());
            assertTrue(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
                    .takeScreenshot(new File(context.getExternalFilesDir(null), "saved-places.png")));
            onView(withContentDescription("Remove Chicago")).perform(click());
            assertTrue(!database.cityExistsForUser(user, "Chicago"));
            onView(withText("Undo")).perform(click());
            assertTrue(database.cityExistsForUser(user, "Chicago"));
            onView(withId(R.id.navForecast)).perform(click());
            onView(withId(R.id.forecastMap)).check(matches(isDisplayed()));
            onView(withId(R.id.forecastWeather)).perform(click());
            onView(withId(R.id.weatherCityName)).check(matches(withText("Champaign")));
            pressBack();
            onView(withId(R.id.navCities)).perform(click());
            onView(allOf(withText("Champaign"), isDisplayed())).check(matches(isDisplayed()));
        } finally {
            if (scenario != null) scenario.close();
            // Remove only this test's records; retain all existing users and cities.
            database.getWritableDatabase().delete("cities", "username = ?", new String[]{user});
            database.getWritableDatabase().delete("users", "username = ?", new String[]{user});
            database.close();
        }
    }
    private void collectTemperatures(View view, java.util.List<View> result) {
        CharSequence description = view.getContentDescription();
        if (description != null && description.toString().endsWith("° Celsius")) result.add(view);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) collectTemperatures(group.getChildAt(i), result);
        }
    }

}
