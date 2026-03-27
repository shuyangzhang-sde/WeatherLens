package edu.uiuc.cs427app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Authentication screen that serves as the app's launcher activity.
 * Provides sign-in for returning users and sign-up for new users.
 * Collects username, password, and a natural-language theme description
 * used for LLM-driven UI customization.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText editUsername;
    private EditText editPassword;
    private EditText editThemeDescription;
    private Button buttonSignIn;
    private Button buttonSignUp;
    private DatabaseHelper databaseHelper;

    /**
     * Initializes the login UI components and sets click listeners
     * for the sign-in and sign-up buttons.
     *
     * @param savedInstanceState the previously saved instance state, if any
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        databaseHelper = new DatabaseHelper(this);

        editUsername = findViewById(R.id.editTextUsername);
        editPassword = findViewById(R.id.editTextPassword);
        editThemeDescription = findViewById(R.id.editTextTheme);
        buttonSignIn = findViewById(R.id.buttonSignIn);
        buttonSignUp = findViewById(R.id.buttonSignUp);

        buttonSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleSignIn();
            }
        });

        buttonSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleSignUp();
            }
        });
    }

    /**
     * Handles sign-in by validating the entered username and password
     * against stored credentials. On success, navigates to MainActivity
     * with the authenticated username.
     */
    private void handleSignIn() {
        String username = editUsername.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both username and password",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (databaseHelper.checkUser(username, password)) {
            Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
            navigateToMain(username);
        } else {
            Toast.makeText(this, "Invalid username or password",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handles sign-up by creating a new user account. Validates that username,
     * password, and theme description are all provided, and that the username
     * is not already taken. On success, navigates to MainActivity.
     */
    private void handleSignUp() {
        
    }

    /**
     * Navigates to MainActivity, passing the logged-in username via Intent extras.
     * Clears the activity back stack so pressing back won't return to the login screen.
     *
     * @param username the authenticated user's username
     */
    private void navigateToMain(String username) {
        
    }
}