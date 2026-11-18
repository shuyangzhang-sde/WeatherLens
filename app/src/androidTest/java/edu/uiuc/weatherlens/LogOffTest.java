package edu.uiuc.weatherlens;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.uiuc.weatherlens.theme.ThemeContract;
import edu.uiuc.weatherlens.theme.ThemeDefaults;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.Espresso.pressBackUnconditionally;


@RunWith(AndroidJUnit4.class)
public class LogOffTest {

    private ActivityScenario<MainActivity> scenario;

    /**
     * Performs teardown after running logoff test
     */
    @After
    public void tearDown() {
        try {
            pressBackUnconditionally();
        } catch (Exception ignored) {}

        if (scenario != null) {
            scenario.close();
            scenario = null;
        }
    }

    /**
     * Test that ensures user navigates back to login screen when logout is hit
     */
    @Test
    public void logOffReturn() throws InterruptedException {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class);

        intent.putExtra("username", "testUser");
        intent.putExtra(ThemeContract.EXTRA_THEME_PROMPT, "UIUC Theme");
        intent.putExtra(ThemeContract.EXTRA_THEME_JSON, ThemeDefaults.defaultThemeJson());

        Thread.sleep(1000);

        scenario = ActivityScenario.launch(intent);

        Thread.sleep(1000);

        onView(withId(R.id.homeMenu)).perform(click());
        onView(androidx.test.espresso.matcher.ViewMatchers.withText("Log out")).perform(click());
        onView(withId(R.id.editTextUsername)).check(matches(isDisplayed()));
        onView(withId(R.id.editTextPassword)).check(matches(isDisplayed()));
        onView(withId(R.id.buttonSignIn)).check(matches(isDisplayed()));
        onView(withId(R.id.buttonSignUp)).check(matches(isDisplayed()));
    }

}