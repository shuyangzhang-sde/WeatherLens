package edu.uiuc.cs427app;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.uiuc.cs427app.theme.ThemeContract;
import edu.uiuc.cs427app.theme.ThemeDefaults;
import edu.uiuc.cs427app.theme.ThemeManager;
import edu.uiuc.cs427app.theme.ThemeParser;
import edu.uiuc.cs427app.theme.ThemePromptBuilder;
import edu.uiuc.cs427app.theme.ThemeSpec;

/**
 * Activity that displays current weather for a city selected from the main list.
 * Weather data is loaded from a remote API on a background thread. When loading
 * completes successfully, the user may open {@link WeatherInsightsActivity} with
 * the same city identifier and raw JSON payload passed as intent extras.
 */
public class WeatherActivity extends AppCompatActivity {

    /**
     * Intent extra key for the city name.
     */
    public static final String EXTRA_CITY = "city";

    private ProgressBar progressBar;
    private TextView cityView;
    private TextView dateTimeView;
    private TextView temperatureView;
    private TextView conditionView;
    private TextView humidityView;
    private TextView windView;
    private TextView errorView;
    private Button insightsButton;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private String cityFromIntent = "";
    private String lastSuccessJson = "";

    /**
     * Initializes the layout, enables the action bar up affordance, reads the city
     * from the launching intent, and begins asynchronous weather retrieval.
     *
     * @param savedInstanceState previously saved instance state, or {@code null}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.weather_screen_title);
        }

        String themeJson = getIntent().getStringExtra(ThemeContract.EXTRA_THEME_JSON);
        if (themeJson != null && !themeJson.isEmpty()) {
            ThemeManager.applyTheme(this, ThemeParser.parseThemeJsonOrDefault(themeJson));
        }

        cityFromIntent = safeExtraCity(getIntent().getStringExtra(EXTRA_CITY));
        bindViews();
        wireInsightsButton();
        startWeatherFetch();
    }

    /**
     * Obtains references to all views required to render loading state, weather
     * fields, errors, and the insights action.
     */
    private void bindViews() {
        progressBar = findViewById(R.id.weatherProgress);
        cityView = findViewById(R.id.weatherCityName);
        dateTimeView = findViewById(R.id.weatherDateTime);
        temperatureView = findViewById(R.id.weatherTemperature);
        conditionView = findViewById(R.id.weatherCondition);
        humidityView = findViewById(R.id.weatherHumidity);
        windView = findViewById(R.id.weatherWind);
        errorView = findViewById(R.id.weatherError);
        insightsButton = findViewById(R.id.weatherInsightsButton);
    }

    /**
     * Registers the insights button listener and leaves the control disabled until
     * a successful response supplies non-empty JSON for the downstream screen.
     */
    private void wireInsightsButton() {
        insightsButton.setEnabled(false);
        insightsButton.setOnClickListener(v -> openWeatherInsights());
    }

    /**
     * Starts {@link WeatherInsightsActivity} with the city name and complete weather
     * JSON required by that activity's intent contract.
     */
    private void openWeatherInsights() {
        if (lastSuccessJson.isEmpty()) {
            Toast.makeText(this, R.string.weather_insights_missing_context, Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(this, WeatherInsightsActivity.class);
        intent.putExtra(WeatherInsightsActivity.EXTRA_CITY_NAME, cityFromIntent);
        intent.putExtra(WeatherInsightsActivity.EXTRA_WEATHER_JSON, lastSuccessJson);
        intent.putExtra(ThemeContract.EXTRA_THEME_JSON, getIntent().getStringExtra(ThemeContract.EXTRA_THEME_JSON));
        startActivity(intent);
    }

    /**
     * Executes {@link WeatherRepository#fetchCurrentWeather(String, String)} off the
     * UI thread and posts the resulting {@link WeatherResult} back to the main thread.
     */
    private void startWeatherFetch() {
        setLoadingUi(true);
        clearDetails();
        insightsButton.setEnabled(false);
        lastSuccessJson = "";

        executor.execute(() -> {
            WeatherResult result = WeatherRepository.fetchCurrentWeather(
                    cityFromIntent, BuildConfig.WEATHER_API_KEY);
            runOnUiThread(() -> applyWeatherResult(result));
        });
    }

    /**
     * Binds weather text fields from a successful result, or presents an error state
     * and notification when the repository reports failure. Must run on the main thread.
     *
     * @param result outcome of the background fetch operation
     */
    private void applyWeatherResult(WeatherResult result) {
        setLoadingUi(false);
        if (result.isOk() && result.getData() != null) {
            WeatherData d = result.getData();
            errorView.setVisibility(View.GONE);
            cityView.setText(d.getCityDisplayName());
            dateTimeView.setText(d.getDateTimeLine());
            temperatureView.setText(getString(R.string.weather_label_temperature, d.getTemperatureLine()));
            conditionView.setText(getString(R.string.weather_label_condition, d.getConditionLabel()));
            humidityView.setText(getString(R.string.weather_label_humidity, d.getHumidityLine()));
            windView.setText(getString(R.string.weather_label_wind, d.getWindLine()));
            lastSuccessJson = d.getRawApiJson();
            insightsButton.setEnabled(true);
        } else {
            String msg = result.getErrorMessage() != null ? result.getErrorMessage()
                    : getString(R.string.weather_generic_error);
            showErrorState(msg);
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Resets detail text fields prior to a new request so obsolete measurements are not
     * shown together with the progress indicator. Retains a provisional city label when known.
     */
    private void clearDetails() {
        cityView.setText(cityFromIntent.isEmpty() ? getString(R.string.weather_unknown_city) : cityFromIntent);
        dateTimeView.setText("");
        temperatureView.setText("");
        conditionView.setText("");
        humidityView.setText("");
        windView.setText("");
        errorView.setVisibility(View.GONE);
        errorView.setText("");
    }

    /**
     * Displays the error text region, disables the insights control, and clears any
     * cached JSON when the service request does not yield displayable weather data.
     *
     * @param message user-facing description of the failure
     */
    private void showErrorState(String message) {
        errorView.setVisibility(View.VISIBLE);
        errorView.setText(message);
        insightsButton.setEnabled(false);
        lastSuccessJson = "";
    }

    /**
     * Shows or hides the indeterminate progress indicator while network I/O is pending.
     *
     * @param loading {@code true} to show progress, {@code false} to hide it
     */
    private void setLoadingUi(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    /**
     * Returns a trimmed city string, or an empty string when the intent extra is absent.
     *
     * @param raw value read from the intent, possibly {@code null}
     * @return non-null trimmed city text
     */
    private static String safeExtraCity(String raw) {
        return raw == null ? "" : raw.trim();
    }

    /**
     * Handles navigation via the action bar up affordance by finishing this activity.
     *
     * @return {@code true} because the event is consumed
     */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /**
     * Handles the home-as-up menu item consistently with {@link #onSupportNavigateUp()}.
     *
     * @param item selected menu item
     * @return {@code true} when the home item is handled; otherwise delegates to the superclass
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Releases the background executor when the activity is destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}