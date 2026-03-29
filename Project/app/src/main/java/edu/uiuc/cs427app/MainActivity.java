package edu.uiuc.cs427app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

import edu.uiuc.cs427app.theme.ThemeContract;
import edu.uiuc.cs427app.theme.ThemeManager;
import edu.uiuc.cs427app.theme.ThemeParser;
import edu.uiuc.cs427app.theme.ThemeSpec;

public class MainActivity extends AppCompatActivity{

    private String currentUsername;
    private DatabaseHelper databaseHelper;

    private LinearLayout cityContainer;

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

        databaseHelper = new DatabaseHelper(this);

        TextView titleView = findViewById(R.id.textViewTitle);
        titleView.setText("Team 304 - " + currentUsername);

        cityContainer = findViewById(R.id.cityContainer);
        if (cityContainer == null) {
            throw new RuntimeException("cityContainer is null - check activity_main.xml");
        }

        Button buttonNew = findViewById(R.id.buttonAddLocation);
        buttonNew.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddCityActivity.class);
            intent.putExtra("username", currentUsername);
            startActivity(intent);
        });

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

    @Override
    protected void onResume() {
        super.onResume();
        loadCities();
    }

    private void loadCities() {
        cityContainer.removeAllViews();

        ArrayList<String> cities = databaseHelper.getCitiesForUser(currentUsername);

        for (String city : cities) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(12, 12, 12, 12);

            TextView cityName = new TextView(this);
            cityName.setText(city);
            cityName.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1
            ));

            Button detailsButton = new Button(this);
            detailsButton.setText("Show Details");
            detailsButton.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, DetailsActivity.class);
                intent.putExtra("city", city);
                startActivity(intent);
            });

            row.addView(cityName);
            row.addView(detailsButton);
            cityContainer.addView(row);
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
