package edu.uiuc.weatherlens;

import android.content.Context;
import android.content.SharedPreferences;

/** Local sign-in state; never stores the user's password. */
final class LoginSession {
    static final String SHOW_SIGN_IN = "edu.uiuc.weatherlens.SHOW_SIGN_IN";
    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences("login_session", Context.MODE_PRIVATE);
    }
    static String currentUser(Context context) {
        return preferences(context).getString("username", null);
    }
    static void remember(Context context, String username) {
        preferences(context).edit().putString("username", username).apply();
    }
    static void clear(Context context) {
        preferences(context).edit().remove("username").apply();
    }
    static void clearIfCurrentUser(Context context, String username) {
        if (username != null && username.equals(currentUser(context))) clear(context);
    }
    private LoginSession() { }
}
