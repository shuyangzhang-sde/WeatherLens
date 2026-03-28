package edu.uiuc.cs427app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import edu.uiuc.cs427app.theme.ThemeContract;
import edu.uiuc.cs427app.theme.ThemeManager;
import edu.uiuc.cs427app.theme.ThemeParser;
import edu.uiuc.cs427app.theme.ThemeSpec;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private String currentUsername;

    /** Restores the active user and applies the current theme. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currentUsername = getIntent().getStringExtra("username");
        if (currentUsername == null) {
            navigateToLogin();
            return;
        }

        TextView titleView = findViewById(R.id.textViewTitle);
        titleView.setText("Team 304 - " + currentUsername);

        Button buttonChampaign = findViewById(R.id.buttonChampaign);
        Button buttonChicago = findViewById(R.id.buttonChicago);
        Button buttonLA = findViewById(R.id.buttonLA);
        Button buttonNew = findViewById(R.id.buttonAddLocation);

        buttonChampaign.setOnClickListener(this);
        buttonChicago.setOnClickListener(this);
        buttonLA.setOnClickListener(this);
        buttonNew.setOnClickListener(this);

        Button buttonLogout = findViewById(R.id.buttonLogout);
        buttonLogout.setOnClickListener(new View.OnClickListener() {
            /** Returns to the login screen. */
            @Override
            public void onClick(View v) {
                navigateToLogin();
            }
        });

        applyResolvedTheme();
    }

    /** Routes location buttons to the correct detail screen. */
    @Override
    public void onClick(View view) {
        Intent intent;
        int id = view.getId();
        if (id == R.id.buttonChampaign) {
            intent = new Intent(this, DetailsActivity.class);
            intent.putExtra("city", "Champaign");
            startActivity(intent);
        } else if (id == R.id.buttonChicago) {
            intent = new Intent(this, DetailsActivity.class);
            intent.putExtra("city", "Chicago");
            startActivity(intent);
        } else if (id == R.id.buttonLA) {
            intent = new Intent(this, DetailsActivity.class);
            intent.putExtra("city", "Los Angeles");
            startActivity(intent);
        } else if (id == R.id.buttonAddLocation) {
            // Implement this action to add a new location to the list of locations
        }
    }

    /** Applies a validated theme, or the default theme when no JSON is available. */
    private void applyResolvedTheme() {
        String themeJson = getIntent().getStringExtra(ThemeContract.EXTRA_THEME_JSON);
        ThemeSpec themeSpec = ThemeParser.parseThemeJsonOrDefault(themeJson);
        ThemeManager.applyTheme(this, themeSpec);
    }

    /** Clears the back stack and returns to login. */
    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
