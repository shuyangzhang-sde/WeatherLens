package edu.uiuc.cs427app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import edu.uiuc.cs427app.theme.ThemeContract;

/** Authentication screen for sign-in, sign-up, and theme prompt capture. */
public class LoginActivity extends AppCompatActivity {

    private EditText editUsername;
    private EditText editPassword;
    private EditText editThemeDescription;
    private Button buttonSignIn;
    private Button buttonSignUp;
    private DatabaseHelper databaseHelper;

    /** Initializes the auth form and button handlers. */
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
            /** Starts sign-in from the current form values. */
            @Override
            public void onClick(View v) {
                handleSignIn();
            }
        });

        buttonSignUp.setOnClickListener(new View.OnClickListener() {
            /** Starts sign-up from the current form values. */
            @Override
            public void onClick(View v) {
                handleSignUp();
            }
        });
    }

    /** Validates credentials and forwards the optional theme prompt. */
    private void handleSignIn() {
        String username = editUsername.getText().toString().trim();
        String password = editPassword.getText().toString().trim();
        String themeDescription = editThemeDescription.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both username and password",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (themeDescription.isEmpty()) {
            Toast.makeText(this, "Please enter a theme description",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (databaseHelper.checkUser(username, password)) {
            Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
            navigateToMain(username, editThemeDescription.getText().toString().trim(), null);
        } else {
            Toast.makeText(this, "Invalid username or password",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /** Creates a user account and forwards the entered theme prompt. */
    private void handleSignUp() {
        String username = editUsername.getText().toString().trim();
        String password = editPassword.getText().toString().trim();
        String themeDescription = editThemeDescription.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both username and password",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (themeDescription.isEmpty()) {
            Toast.makeText(this, "Please enter a theme description",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (databaseHelper.userExists(username)) {
            Toast.makeText(this, "Username already exists",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (databaseHelper.addUser(username, password, themeDescription)) {
            Toast.makeText(this, "Account created successfully",
                    Toast.LENGTH_SHORT).show();
            navigateToMain(username, themeDescription, null);
        } else {
            Toast.makeText(this, "Registration failed. Please try again.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /** Opens the main screen with the current username and theme handoff data. */
    private void navigateToMain(String username, String themePrompt, String themeJson) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("username", username);
        intent.putExtra(ThemeContract.EXTRA_THEME_PROMPT, themePrompt);
        intent.putExtra(ThemeContract.EXTRA_THEME_JSON, themeJson);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
