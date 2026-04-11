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

public class WeatherActivity extends AppCompatActivity {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.weather_screen_title);
        }

        cityFromIntent = safeExtraCity(getIntent().getStringExtra(EXTRA_CITY));
        bindViews();
        wireInsightsButton();
        startWeatherFetch();
    }

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

    private void wireInsightsButton() {
        insightsButton.setEnabled(false);
        insightsButton.setOnClickListener(v -> openWeatherInsights());
    }

    private void openWeatherInsights() {
        if (lastSuccessJson.isEmpty()) {
            Toast.makeText(this, R.string.weather_insights_missing_context, Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(this, WeatherInsightsActivity.class);
        intent.putExtra(WeatherInsightsActivity.EXTRA_CITY_NAME, cityFromIntent);
        intent.putExtra(WeatherInsightsActivity.EXTRA_WEATHER_JSON, lastSuccessJson);
        startActivity(intent);
    }

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

    private void showErrorState(String message) {
        errorView.setVisibility(View.VISIBLE);
        errorView.setText(message);
        insightsButton.setEnabled(false);
        lastSuccessJson = "";
    }

    private void setLoadingUi(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private static String safeExtraCity(String raw) {
        return raw == null ? "" : raw.trim();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
