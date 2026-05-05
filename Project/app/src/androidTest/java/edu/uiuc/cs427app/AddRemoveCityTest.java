package edu.uiuc.cs427app;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.uiuc.cs427app.theme.ThemeContract;
import edu.uiuc.cs427app.theme.ThemeDefaults;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static org.hamcrest.Matchers.allOf;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class AddRemoveCityTest {

    private ActivityScenario<MainActivity> scenario;
    private DatabaseHelper databaseHelper;

    /**
     * Creates a clean database and launches MainActivity with a cached theme so network calls are avoided.
     */
    private void setUpCleanMainScreen(String username) {
        Context context = ApplicationProvider.getApplicationContext();

        context.deleteDatabase("cs427app.db");
        databaseHelper = new DatabaseHelper(context);

        // Create a user and cache a theme JSON to avoid remote calls in MainActivity
        Assert.assertTrue(databaseHelper.addUser(username, "password", "test theme"));
        databaseHelper.updateThemeJson(username, ThemeDefaults.defaultThemeJson());

        android.content.Intent intent = new android.content.Intent(ApplicationProvider.getApplicationContext(), MainActivity.class);
        intent.putExtra("username", username);
        // MainActivity requires a non-null prompt extra; provide a placeholder and cached JSON
        intent.putExtra(ThemeContract.EXTRA_THEME_PROMPT, "placeholder prompt");
        intent.putExtra(ThemeContract.EXTRA_THEME_JSON, ThemeDefaults.defaultThemeJson());
        // tell MainActivity to propagate a test bypass so AddCityActivity skips Geocoder validation
        intent.putExtra("TEST_BYPASS_GEOCODER", true);

        scenario = ActivityScenario.launch(intent);
    }

    /**
     * Cleans up after all tests
     */
    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }

        ApplicationProvider.getApplicationContext().deleteDatabase("cs427app.db");
    }

    /**
     * Test that a city added to the database for a user appears in the main screen.
     */
    @Test
    public void cityAddedInDbAppearsInMain() throws InterruptedException {
        String username = "cityUser";
        setUpCleanMainScreen(username);

        String city = "Champaign";

        // Launch AddCityActivity via the Add button and use the test bypass so geocoder is skipped
        onView(withId(R.id.buttonAddLocation)).check(matches(isDisplayed())).perform(click());

        // Enter the city and save
        Thread.sleep(200);
        onView(withId(R.id.editTextCityName)).perform(replaceText(city), closeSoftKeyboard());
        Thread.sleep(200);
        onView(withId(R.id.buttonSaveCity)).perform(click());

        // Allow UI thread to update
        Thread.sleep(500);

        onView(withText(city)).check(matches(isDisplayed()));
    }

    /**
     * Test that pressing the Remove button for a city removes it from the database and UI.
     */
    @Test
    public void removeCityFromMainRemovesFromDbAndUi() throws InterruptedException {
        String username = "removeUser";
        setUpCleanMainScreen(username);
        String city = "Springfield";

        // Add city through the UI (so UI wiring is exercised)
        onView(withId(R.id.buttonAddLocation)).check(matches(isDisplayed())).perform(click());
        Thread.sleep(200);
        onView(withId(R.id.editTextCityName)).perform(replaceText(city), closeSoftKeyboard());
        Thread.sleep(200);
        onView(withId(R.id.buttonSaveCity)).perform(click());

        Thread.sleep(500);

        // Click the Remove button that is a sibling of the city name
        onView(allOf(withText("Remove"), hasSibling(withText(city)))).perform(click());

        Thread.sleep(500);

        Assert.assertFalse(databaseHelper.cityExistsForUser(username, city));

        onView(withText(city)).check(doesNotExist());
    }
}