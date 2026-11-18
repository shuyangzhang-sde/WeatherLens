package edu.uiuc.weatherlens;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.uiuc.weatherlens.theme.ThemeDefaults;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class LoginSignUpTest {

    private ActivityScenario<LoginActivity> scenario;
    private DatabaseHelper databaseHelper;

    /**
     * Creates a clean database and prevents MainActivity from actually launching.
     * This avoids LLM/theme API calls during tests.
     */
    private void setUpCleanLoginScreen() {
        Context context = ApplicationProvider.getApplicationContext();

        context.deleteDatabase("cs427app.db");
        databaseHelper = new DatabaseHelper(context);

        Intents.init();

        intending(hasComponent(MainActivity.class.getName()))
                .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));

        scenario = ActivityScenario.launch(LoginActivity.class);
    }

    /**
     * Performs teardown after each login/sign-up test.
     */
    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }

        try {
            Intents.release();
        } catch (IllegalStateException ignored) {
            // Intents may not have been initialized if setup failed early.
        }

        ApplicationProvider.getApplicationContext().deleteDatabase("cs427app.db");
    }

    /**
     * Test that signing up with a new username, password, and theme creates the user.
     */
    @Test
    public void signUpCreatesNewUser() throws InterruptedException {
        setUpCleanLoginScreen();

        String username = "signupUser";
        String password = "signupPassword";
        String themeDescription = "blue orange UIUC theme";

        Thread.sleep(500);

        onView(withId(R.id.editTextUsername))
                .check(matches(isDisplayed()))
                .perform(replaceText(username), closeSoftKeyboard());

        Thread.sleep(500);

        onView(withId(R.id.editTextPassword))
                .check(matches(isDisplayed()))
                .perform(replaceText(password), closeSoftKeyboard());

        Thread.sleep(500);

        onView(withId(R.id.editTextTheme))
                .check(matches(isDisplayed()))
                .perform(replaceText(themeDescription), closeSoftKeyboard());

        Thread.sleep(500);

        onView(withId(R.id.buttonSignUp))
                .check(matches(isDisplayed()))
                .perform(click());

        Thread.sleep(500);

        Assert.assertTrue(databaseHelper.userExists(username));
        Assert.assertTrue(databaseHelper.checkUser(username, password));
        Assert.assertEquals(themeDescription, databaseHelper.getUserThemeDescription(username));

        intended(hasComponent(MainActivity.class.getName()));
    }

    /**
     * Test that login succeeds for an existing user with the correct password.
     */
    @Test
    public void logInExistingUserSucceeds() throws InterruptedException {
        setUpCleanLoginScreen();

        String username = "loginUser";
        String password = "loginPassword";
        String themeDescription = "default cached theme";

        Assert.assertTrue(databaseHelper.addUser(username, password, themeDescription));
        databaseHelper.updateThemeJson(username, ThemeDefaults.defaultThemeJson());

        Thread.sleep(500);

        onView(withId(R.id.editTextUsername))
                .check(matches(isDisplayed()))
                .perform(replaceText(username), closeSoftKeyboard());

        Thread.sleep(500);

        onView(withId(R.id.editTextPassword))
                .check(matches(isDisplayed()))
                .perform(replaceText(password), closeSoftKeyboard());

        Thread.sleep(500);

        onView(withId(R.id.buttonSignIn))
                .check(matches(isDisplayed()))
                .perform(click());

        Thread.sleep(500);

        Assert.assertTrue(databaseHelper.checkUser(username, password));

        intended(hasComponent(MainActivity.class.getName()));
    }

    /**
     * Test that login with the wrong password does not enter the main screen.
     */
    @Test
    public void logInWrongPasswordStaysOnLoginScreen() throws InterruptedException {
        setUpCleanLoginScreen();

        String username = "wrongPasswordUser";
        String correctPassword = "correctPassword";
        String wrongPassword = "wrongPassword";

        Assert.assertTrue(databaseHelper.addUser(username, correctPassword, "dark theme"));

        Thread.sleep(500);

        onView(withId(R.id.editTextUsername))
                .check(matches(isDisplayed()))
                .perform(replaceText(username), closeSoftKeyboard());

        Thread.sleep(500);

        onView(withId(R.id.editTextPassword))
                .check(matches(isDisplayed()))
                .perform(replaceText(wrongPassword), closeSoftKeyboard());

        Thread.sleep(500);

        onView(withId(R.id.buttonSignIn))
                .check(matches(isDisplayed()))
                .perform(click());

        Thread.sleep(500);

        onView(withId(R.id.editTextUsername)).check(matches(isDisplayed()));
        onView(withId(R.id.editTextPassword)).check(matches(isDisplayed()));
        onView(withId(R.id.buttonSignIn)).check(matches(isDisplayed()));
        onView(withId(R.id.buttonSignUp)).check(matches(isDisplayed()));

        Assert.assertFalse(databaseHelper.checkUser(username, wrongPassword));
    }

    /**
     * Test that sign-up fails when the theme description is missing.
     */
    @Test
    public void signUpMissingThemeDoesNotCreateUser() throws InterruptedException {
        setUpCleanLoginScreen();

        String username = "missingThemeUser";
        String password = "missingThemePassword";

        Thread.sleep(500);

        onView(withId(R.id.editTextUsername))
                .check(matches(isDisplayed()))
                .perform(replaceText(username), closeSoftKeyboard());

        Thread.sleep(500);

        onView(withId(R.id.editTextPassword))
                .check(matches(isDisplayed()))
                .perform(replaceText(password), closeSoftKeyboard());

        Thread.sleep(500);

        onView(withId(R.id.buttonSignUp))
                .check(matches(isDisplayed()))
                .perform(click());

        Thread.sleep(500);

        onView(withId(R.id.editTextUsername)).check(matches(isDisplayed()));
        onView(withId(R.id.editTextPassword)).check(matches(isDisplayed()));
        onView(withId(R.id.editTextTheme)).check(matches(isDisplayed()));

        Assert.assertFalse(databaseHelper.userExists(username));
    }

    /**
     * Test that duplicate usernames are rejected during sign-up.
     */
    @Test
    public void signUpDuplicateUsernameDoesNotOverwriteUser() throws InterruptedException {
        setUpCleanLoginScreen();

        String username = "duplicateUser";
        String originalPassword = "originalPassword";
        String duplicatePassword = "duplicatePassword";

        Assert.assertTrue(databaseHelper.addUser(username, originalPassword, "original theme"));

        Thread.sleep(500);

        onView(withId(R.id.editTextUsername))
                .check(matches(isDisplayed()))
                .perform(replaceText(username), closeSoftKeyboard());

        Thread.sleep(500);

        onView(withId(R.id.editTextPassword))
                .check(matches(isDisplayed()))
                .perform(replaceText(duplicatePassword), closeSoftKeyboard());

        Thread.sleep(500);

        onView(withId(R.id.editTextTheme))
                .check(matches(isDisplayed()))
                .perform(replaceText("new theme"), closeSoftKeyboard());

        Thread.sleep(500);

        onView(withId(R.id.buttonSignUp))
                .check(matches(isDisplayed()))
                .perform(click());

        Thread.sleep(500);

        onView(withId(R.id.editTextUsername)).check(matches(isDisplayed()));
        onView(withId(R.id.editTextPassword)).check(matches(isDisplayed()));
        onView(withId(R.id.buttonSignUp)).check(matches(isDisplayed()));

        Assert.assertTrue(databaseHelper.userExists(username));
        Assert.assertTrue(databaseHelper.checkUser(username, originalPassword));
        Assert.assertFalse(databaseHelper.checkUser(username, duplicatePassword));
    }
}
