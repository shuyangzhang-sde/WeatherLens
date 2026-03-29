package edu.uiuc.cs427app;
import edu.uiuc.cs427app.BuildConfig;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.uiuc.cs427app.theme.ThemeContract;
import edu.uiuc.cs427app.theme.ThemeDefaults;
import edu.uiuc.cs427app.theme.ThemeManager;
import edu.uiuc.cs427app.theme.ThemeParser;
import edu.uiuc.cs427app.theme.ThemePromptBuilder;
import edu.uiuc.cs427app.theme.ThemeSpec;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private String currentUsername;
    private String currentPrompt;
    private View hider;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /** Restores the active user and applies the current theme. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currentUsername = getIntent().getStringExtra("username");
        currentPrompt = getIntent().getStringExtra(ThemeContract.EXTRA_THEME_PROMPT);
        if (currentUsername == null || currentPrompt == null) {
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

        hider = findViewById(android.R.id.content);
        hider.setVisibility(View.INVISIBLE);
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

    /** Applies a validated theme, or the default theme when generation fails. */
    private void applyResolvedTheme() {
        executor.execute(() -> {
            try {
                ThemeSpec theme = ThemeParser.parseThemeJsonOrDefault(jsonRequest(currentPrompt));

                runOnUiThread(() -> {
                    try {
                        ThemeManager.applyTheme(this, theme);
                    } catch (Throwable uiThrowable) {
                        ThemeManager.applyTheme(this, ThemeDefaults.defaultTheme());
                    }
                    hider.setVisibility(View.VISIBLE);
                });
            } catch (Throwable throwable) {
                runOnUiThread(() -> {
                    ThemeManager.applyTheme(this, ThemeDefaults.defaultTheme());
                    hider.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    /** Performs LLM API Call and returns response */
    private String jsonRequest(String input) throws Exception {
        JSONObject body = new JSONObject().put("contents", new JSONArray().put(new JSONObject().put("parts", new JSONArray().put(
                new JSONObject().put("text", ThemePromptBuilder.buildUserPrompt(input)))))).put("generationConfig", new JSONObject()
                .put("responseMimeType", "application/json").put("responseJsonSchema", ThemePromptBuilder.buildJsonSchema()));

        HttpURLConnection connection = (HttpURLConnection) new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent").openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("x-goog-api-key", BuildConfig.GEMINI_KEY);
        connection.setDoOutput(true);

        connection.getOutputStream().write(body.toString().getBytes(StandardCharsets.UTF_8));
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        for (String line; (line = reader.readLine()) != null; ) builder.append(line.trim());

        JSONObject responseJson = new JSONObject(builder.toString());
        JSONArray candidates = responseJson.getJSONArray("candidates");
        JSONObject firstCandidate = candidates.getJSONObject(0);
        JSONObject content = firstCandidate.getJSONObject("content");
        JSONArray parts = content.getJSONArray("parts");
        String theme = parts.getJSONObject(0).getString("text");

        return theme;
    }

    /** Clears the back stack and returns to login. */
    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}