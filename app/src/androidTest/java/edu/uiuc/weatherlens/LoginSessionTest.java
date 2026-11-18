package edu.uiuc.weatherlens;

import android.content.Context;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import edu.uiuc.weatherlens.theme.ThemeDefaults;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.*;

/** Preserves all pre-existing accounts and the user's remembered session. */
@RunWith(AndroidJUnit4.class)
public class LoginSessionTest {
    private Context context;
    private DatabaseHelper database;
    private String previousUser;
    private String user;
    private ActivityScenario<LoginActivity> scenario;

    @Before public void prepare() {
        context = ApplicationProvider.getApplicationContext();
        previousUser = LoginSession.currentUser(context);
        LoginSession.clear(context);
        database = new DatabaseHelper(context);
        user = "SessionTest " + java.util.UUID.randomUUID();
        assertTrue(database.addUser(user, "temporary-password", "warm cream"));
        database.updateThemeJson(user, ThemeDefaults.defaultThemeJson());
    }

    @After public void restore() {
        if (scenario != null) scenario.close();
        database.getWritableDatabase().delete("users", "username = ?", new String[]{user});
        database.close();
        context.getSharedPreferences("weatherlens", Context.MODE_PRIVATE).edit().remove("city:" + user).apply();
        if (previousUser == null) LoginSession.clear(context);
        else LoginSession.remember(context, previousUser);
    }

    @Test public void successfulLoginSurvivesRelaunchUntilExplicitLogout() {
        scenario = ActivityScenario.launch(LoginActivity.class);
        onView(withId(R.id.editTextUsername)).perform(replaceText(user), closeSoftKeyboard());
        onView(withId(R.id.editTextPassword)).perform(replaceText("temporary-password"), closeSoftKeyboard());
        onView(withId(R.id.buttonSignIn)).perform(click());
        assertEquals(user, LoginSession.currentUser(context));
        onView(withId(R.id.homeNavigation)).check(matches(isDisplayed()));
        scenario.close();
        scenario = ActivityScenario.launch(LoginActivity.class);
        onView(withId(R.id.homeNavigation)).check(matches(isDisplayed()));
        onView(withId(R.id.editTextUsername)).check(doesNotExist());
        onView(withId(R.id.homeMenu)).perform(click());
        onView(withText("Log out")).perform(click());
        onView(withId(R.id.editTextUsername)).check(matches(isDisplayed()));
        assertNull(LoginSession.currentUser(context));
        scenario.close();
        scenario = ActivityScenario.launch(LoginActivity.class);
        onView(withId(R.id.editTextUsername)).check(matches(isDisplayed()));
    }

    @Test public void missingAccountRequiresLoginAndClearsStaleSession() {
        LoginSession.remember(context, "missing-" + user);
        scenario = ActivityScenario.launch(LoginActivity.class);
        onView(withId(R.id.editTextUsername)).check(matches(isDisplayed()));
        assertNull(LoginSession.currentUser(context));
    }

    @Test public void temporaryAccountLogoutDoesNotClearTheRealUsersSession() {
        LoginSession.remember(context, user);
        LoginSession.clearIfCurrentUser(context, "another-test-account");
        assertEquals(user, LoginSession.currentUser(context));
    }
}
