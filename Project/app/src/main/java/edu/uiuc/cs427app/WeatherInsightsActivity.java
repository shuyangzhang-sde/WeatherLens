package edu.uiuc.cs427app;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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

/**
 * Weather insights screen for milestone 4. We ask Gemini for a few question ideas based on the
 * weather JSON, show them as buttons, then if you tap one we call Gemini again for the answer.
 * The weather screen is supposed to pass us the city name + JSON from the API (not hardcoded).
 */
public class WeatherInsightsActivity extends AppCompatActivity {

    /** Extra key: city name string (same idea as other activities). */
    public static final String EXTRA_CITY_NAME =
            "edu.uiuc.cs427app.WeatherInsightsActivity.EXTRA_CITY_NAME";

    /**
     * Extra key: the weather response as a JSON string so the LLM has something real to work with.
     */
    public static final String EXTRA_WEATHER_JSON =
            "edu.uiuc.cs427app.WeatherInsightsActivity.EXTRA_WEATHER_JSON";

    private static final String GEMINI_GENERATE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    private LinearLayout questionContainer;
    private TextView answerText;
    private ProgressBar progressBar;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private String cityName = "";
    private String weatherJson = "";

    /** Set up toolbar, read extras (city + weather JSON), then kick off loading questions. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_insights);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.weather_insights_title);
        }

        String themeJson = getIntent().getStringExtra(ThemeContract.EXTRA_THEME_JSON);
        if (themeJson != null && !themeJson.isEmpty()) {
            ThemeManager.applyTheme(this, ThemeParser.parseThemeJsonOrDefault(themeJson));
        }

        cityName = safeString(getIntent().getStringExtra(EXTRA_CITY_NAME));
        weatherJson = safeString(getIntent().getStringExtra(EXTRA_WEATHER_JSON));

        questionContainer = findViewById(R.id.weatherInsightsQuestionContainer);
        answerText = findViewById(R.id.weatherInsightsAnswer);
        progressBar = findViewById(R.id.weatherInsightsProgress);

        if (weatherJson.isEmpty()) {
            Toast.makeText(this, R.string.weather_insights_missing_context, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        loadSuggestedQuestions();
    }

    /** First Gemini call: get suggested questions from the weather data we got in the intent. */
    private void loadSuggestedQuestions() {
        setLoading(true);
        executor.execute(() -> {
            try {
                String prompt = buildQuestionsPrompt(cityName, weatherJson);
                String rawText = geminiGeneratePlainText(prompt);
                String[] questions = parseQuestionsJson(rawText);
                runOnUiThread(() -> {
                    setLoading(false);
                    if (questions.length < 2) {
                        Toast.makeText(this, R.string.weather_insights_questions_failed,
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    renderQuestionButtons(questions);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, R.string.weather_insights_network_error,
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /** Prompt for step 1 — we tell Gemini to only return JSON with a "questions" array. */
    private static String buildQuestionsPrompt(String city, String weatherContextJson) {
        return "You help users interpret weather data. Here is JSON weather data for the city \""
                + escapeForPrompt(city) + "\":\n"
                + weatherContextJson
                + "\n\nGenerate at least two distinct, practical questions a user might ask to plan "
                + "their day based only on this data. Do not answer them. "
                + "Respond with ONLY valid JSON in this exact shape, no markdown or extra text: "
                + "{\"questions\":[\"...\",\"...\"]}\n"
                + "Use the same language as the city name or English if unclear.";
    }

    /** Prompt for step 2 — user picked a button, we send the same weather JSON + their question. */
    private static String buildAnswerPrompt(String city, String weatherContextJson, String question) {
        return "Use the following JSON weather data for \""
                + escapeForPrompt(city)
                + "\" as ground truth. The user asks: "
                + escapeForPrompt(question)
                + "\n\nWeather JSON:\n"
                + weatherContextJson
                + "\n\nGive a concise, helpful answer. If something is not in the data, say so."
                + "Do not use markdown, asterisks, or any special formatting.";
    }

    private static String escapeForPrompt(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /** Try to pull the questions array out of Gemini's reply; if parsing fails we return empty. */
    private static String[] parseQuestionsJson(String rawText) {
        try {
            JSONObject obj = new JSONObject(rawText.trim());
            JSONArray arr = obj.getJSONArray("questions");
            int n = arr.length();
            String[] out = new String[n];
            for (int i = 0; i < n; i++) {
                out[i] = arr.getString(i).trim();
            }
            return out;
        } catch (Exception ignored) {
            return new String[0];
        }
    }

    /** Dynamically add a button for each question string (milestone wants at least 2). */
    private void renderQuestionButtons(String[] questions) {
        questionContainer.removeAllViews();
        for (String q : questions) {
            if (q == null || q.isEmpty()) {
                continue;
            }
            Button b = new Button(this);
            b.setText(q);
            b.setAllCaps(false);
            b.setOnClickListener(v -> fetchAnswerForQuestion(q));
            questionContainer.addView(b);
        }

        String themeJson = getIntent().getStringExtra(ThemeContract.EXTRA_THEME_JSON);
        if (themeJson != null && !themeJson.isEmpty()) {
            ThemeManager.applyTheme(this, ThemeParser.parseThemeJsonOrDefault(themeJson));
        }
    }

    /** Second Gemini call: fill in the answer TextView on the main thread when we're done. */
    private void fetchAnswerForQuestion(String question) {
        setLoading(true);
        answerText.setText("");
        executor.execute(() -> {
            try {
                String prompt = buildAnswerPrompt(cityName, weatherJson, question);
                String rawText = geminiGeneratePlainText(prompt);
                runOnUiThread(() -> {
                    setLoading(false);
                    answerText.setText(rawText.trim());

                    String themeJson = getIntent().getStringExtra(ThemeContract.EXTRA_THEME_JSON);
                    if (themeJson != null && !themeJson.isEmpty()) {
                        ThemeManager.applyTheme(this, ThemeParser.parseThemeJsonOrDefault(themeJson));
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, R.string.weather_insights_network_error,
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /** Simple loading spinner so the user knows we're waiting on the network. */
    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    /**
     * POST to Gemini (same endpoint style as MainActivity's theme code). Returns the first text
     * chunk from the response — throws if HTTP errors or JSON shape is weird.
     */
    private String geminiGeneratePlainText(String userPrompt) throws Exception {
        JSONObject body = new JSONObject()
                .put("contents", new JSONArray().put(
                        new JSONObject().put("parts", new JSONArray().put(
                                new JSONObject().put("text", userPrompt)))))
                .put("generationConfig", new JSONObject().put("temperature", 0.7));

        HttpURLConnection connection =
                (HttpURLConnection) new URL(GEMINI_GENERATE_URL).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("x-goog-api-key", BuildConfig.GEMINI_KEY);
        connection.setDoOutput(true);
        connection.getOutputStream().write(body.toString().getBytes(StandardCharsets.UTF_8));

        int code = connection.getResponseCode();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                code >= 400 ? connection.getErrorStream() : connection.getInputStream(),
                StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        for (String line; (line = reader.readLine()) != null; ) {
            builder.append(line.trim());
        }
        if (code >= 400) {
            throw new IllegalStateException("Gemini HTTP " + code + ": " + builder);
        }

        JSONObject responseJson = new JSONObject(builder.toString());
        JSONArray candidates = responseJson.getJSONArray("candidates");
        JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
        JSONArray parts = content.getJSONArray("parts");
        return parts.getJSONObject(0).getString("text");
    }

    private static String safeString(String s) {
        return s != null ? s : "";
    }

    /** Back navigation (toolbar up). */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /** Handle the home/up arrow the same way. */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** Shut down the background thread pool so we don't leak stuff. */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
