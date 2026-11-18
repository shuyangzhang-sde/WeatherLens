package edu.uiuc.weatherlens;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import edu.uiuc.weatherlens.forecast.ForecastData;
import edu.uiuc.weatherlens.forecast.ForecastRepository;
import edu.uiuc.weatherlens.theme.ThemeContract;
import edu.uiuc.weatherlens.theme.ThemeDefaults;
import edu.uiuc.weatherlens.theme.ThemeManager;
import edu.uiuc.weatherlens.theme.ThemeParser;
import edu.uiuc.weatherlens.theme.ThemePromptBuilder;
import edu.uiuc.weatherlens.theme.ThemeSpec;
import edu.uiuc.weatherlens.weatherinsights.GeminiApiClient;
import org.json.JSONObject;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** One shared city selection and user theme across Today, Forecast, and Cities. */
public class MainActivity extends AppCompatActivity {
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private Future<?> weatherTask;
    private Future<?> forecastTask;
    private DatabaseHelper database;
    private SharedPreferences preferences;
    private String username;
    private String selectedCity = "";
    private String themeJson = ThemeDefaults.defaultThemeJson();
    private ThemeSpec theme = ThemeDefaults.defaultTheme();
    private ArrayList<String> savedCities = new ArrayList<>();
    private View todayPage;
    private View forecastPage;
    private View citiesPage;
    private Spinner citySelector;
    private View citySelectorContainer;
    private BottomNavigationView navigation;
    private int selectedTab = R.id.navToday;
    private int generation;
    private boolean weatherLoading;
    private boolean forecastLoading;
    private WeatherData weather;
    private ForecastData forecast;
    private String weatherError = "";
    private String forecastError = "";
    private long fetchedAt;
    private final java.util.Map<String, WeatherResult> citySnapshots = new java.util.HashMap<>();
    private final java.util.Map<String, Long> citySnapshotTimes = new java.util.HashMap<>();
    private final java.util.Set<String> loadingCities = new java.util.HashSet<>();
    private boolean started;
    private boolean destroyed;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        username = getIntent().getStringExtra("username");
        if (username == null || username.trim().isEmpty()) { logout(); return; }
        setContentView(R.layout.activity_main);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        database = new DatabaseHelper(this);
        preferences = getSharedPreferences("weatherlens", MODE_PRIVATE);
        selectedCity = state == null ? preferences.getString("city:" + username, "") : state.getString("city", "");
        if (state != null) {
            selectedTab = state.getInt("tab", R.id.navToday);
        }
        FrameLayout host = findViewById(R.id.homeContent);
        todayPage = getLayoutInflater().inflate(R.layout.activity_weather, host, false);
        // Embedded pages share the shell's user-level background palette.
        todayPage.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        forecastPage = getLayoutInflater().inflate(R.layout.view_forecast, host, false);
        citiesPage = getLayoutInflater().inflate(R.layout.view_cities, host, false);
        host.addView(todayPage);
        host.addView(forecastPage);
        host.addView(citiesPage);
        citiesPage.findViewById(R.id.buttonAddLocation).setOnClickListener(v -> addCity());
        todayPage.findViewById(R.id.todayRetry).setOnClickListener(v -> loadCurrent());
        forecastPage.findViewById(R.id.forecastRetry).setOnClickListener(v -> {
            if (weather == null) loadCurrent(); else loadForecast();
        });
        forecastPage.findViewById(R.id.forecastMap).setOnClickListener(v -> openMap(selectedCity));
        forecastPage.findViewById(R.id.forecastWeather).setOnClickListener(v -> {
            if (selectedCity.isEmpty()) return;
            Intent intent = new Intent(this, WeatherActivity.class);
            intent.putExtra(WeatherActivity.EXTRA_CITY, selectedCity);
            intent.putExtra(ThemeContract.EXTRA_THEME_JSON, themeJson);
            startActivity(intent);
        });
        todayPage.findViewById(R.id.weatherInsightsButton).setOnClickListener(v -> openInsights());
        todayPage.findViewById(R.id.todayHeading).setVisibility(View.VISIBLE);
        todayPage.findViewById(R.id.weatherCityName).setVisibility(View.GONE);
        FrameLayout selectorSlot = todayPage.findViewById(R.id.todayCitySelectorSlot);
        citySelectorContainer = getLayoutInflater().inflate(R.layout.view_city_selector, selectorSlot, false);
        selectorSlot.addView(citySelectorContainer);
        citySelector = citySelectorContainer.findViewById(R.id.citySelector);
        citySelectorContainer.setOnClickListener(v -> citySelector.performClick());
        citySelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String city = (String) parent.getItemAtPosition(position);
                if (!city.equals(selectedCity)) selectCity(city);
                citySelector.post(MainActivity.this::resizeCitySelector);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        navigation = findViewById(R.id.homeNavigation);
        navigation.setOnNavigationItemSelectedListener(item -> { showTab(item.getItemId()); return true; });
        findViewById(R.id.homeMenu).setOnClickListener(this::showMenu);
        applyUserTheme(state);
    }

    @Override protected void onResume() {
        super.onResume();
        if (database == null) return;
        refreshCities();
        if (!selectedCity.isEmpty() && !weatherLoading
                && (!started || System.currentTimeMillis() - fetchedAt > 10 * 60 * 1000L)) loadCurrent();
        started = true;
        showTab(selectedTab);
    }

    private void refreshCities() {
        ArrayList<String> updated = database.getCitiesForUser(username);
        boolean changed = !updated.equals(savedCities);
        savedCities = updated;
        if (!savedCities.contains(selectedCity)) {
            selectedCity = savedCities.isEmpty() ? "" : savedCities.get(0);
            if (started) loadCurrent();
        }
        if (changed || citySelector.getAdapter() == null) {
            CitySelectorAdapter adapter = new CitySelectorAdapter(this, savedCities,
                    () -> theme, () -> selectedCity);
            citySelector.setAdapter(adapter);
        }
        if (!selectedCity.isEmpty()) citySelector.setSelection(savedCities.indexOf(selectedCity));
        positionCitySelector(selectedTab);
        preferences.edit().putString("city:" + username, selectedCity).apply();
        renderCities();
        if (savedCities.isEmpty()) selectedTab = R.id.navCities;
    }

    private void selectCity(String city) {
        if (!savedCities.contains(city)) return;
        selectedCity = city;
        preferences.edit().putString("city:" + username, city).apply();
        citySelector.setSelection(savedCities.indexOf(city));
        resizeCitySelector();
        loadCurrent();
        renderCities();
    }

    private void showTab(int tab) {
        selectedTab = tab;
        todayPage.setVisibility(tab == R.id.navToday ? View.VISIBLE : View.GONE);
        forecastPage.setVisibility(tab == R.id.navForecast ? View.VISIBLE : View.GONE);
        citiesPage.setVisibility(tab == R.id.navCities ? View.VISIBLE : View.GONE);
        navigation.getMenu().findItem(tab).setChecked(true);
        positionCitySelector(tab);
        if (tab == R.id.navCities) renderCities();
        if (tab == R.id.navToday) renderToday();
        if (tab == R.id.navForecast) {
            if (forecast == null && !forecastLoading && forecastError.isEmpty() && weather != null) loadForecast();
            renderForecast();
        }
        applyTheme();
    }

    /** One selector and one city state shared by both page headings. */
    private void positionCitySelector(int tab) {
        FrameLayout target = tab == R.id.navForecast
                ? forecastPage.findViewById(R.id.forecastCitySelectorSlot)
                : todayPage.findViewById(R.id.todayCitySelectorSlot);
        if (citySelectorContainer.getParent() != target) {
            ((android.view.ViewGroup) citySelectorContainer.getParent()).removeView(citySelectorContainer);
            target.addView(citySelectorContainer);
        }
        citySelectorContainer.setVisibility(savedCities.isEmpty() || tab == R.id.navCities ? View.GONE : View.VISIBLE);
        resizeCitySelector();
        target.post(this::resizeCitySelector);
    }

    private void resizeCitySelector() {
        android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(16 * getResources().getDisplayMetrics().scaledDensity);
        paint.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        View selectedView = citySelector.getSelectedView();
        int labelPadding = dp(6);
        if (selectedView instanceof TextView) {
            paint = ((TextView) selectedView).getPaint();
            labelPadding = selectedView.getPaddingLeft() + selectedView.getPaddingRight();
        }
        int desiredWidth = (int) Math.ceil(paint.measureText(selectedCity)) + labelPadding
                + citySelector.getPaddingLeft() + citySelector.getPaddingRight()
                + citySelectorContainer.getPaddingLeft() + citySelectorContainer.getPaddingRight()
                + dp(26);
        View slot = (View) citySelectorContainer.getParent();
        int availableWidth = slot.getWidth();
        android.view.ViewGroup.LayoutParams params = citySelectorContainer.getLayoutParams();
        int width = availableWidth > 0 ? Math.min(desiredWidth, availableWidth) : desiredWidth;
        if (params.width != width) {
            params.width = width;
            citySelectorContainer.setLayoutParams(params);
        }
    }

    private void loadCurrent() {
        final int request = ++generation;
        if (weatherTask != null) weatherTask.cancel(true);
        if (forecastTask != null) forecastTask.cancel(true);
        weather = null;
        forecast = null;
        weatherError = "";
        forecastError = "";
        forecastLoading = false;
        weatherLoading = !selectedCity.isEmpty();
        renderToday();
        renderForecast();
        if (selectedCity.isEmpty()) return;
        final String city = selectedCity;
        weatherTask = executor.submit(() -> {
            WeatherResult result = WeatherRepository.fetchCurrentWeather(city, BuildConfig.WEATHER_API_KEY);
            runOnUiThread(() -> {
                if (!active(request)) return;
                weatherLoading = false;
                fetchedAt = System.currentTimeMillis();
                if (result.isOk() && result.getData() != null) weather = result.getData();
                else weatherError = result.getErrorMessage() == null ? "Current weather is unavailable." : result.getErrorMessage();
                citySnapshots.put(city, result);
                citySnapshotTimes.put(city, System.currentTimeMillis());
                renderCities();
                renderToday();
                if (selectedTab == R.id.navForecast && weather != null) loadForecast(); else renderForecast();
            });
        });
    }

    private void renderToday() {
        ((TextView) todayPage.findViewById(R.id.weatherCityName)).setText(selectedCity.isEmpty() ? "Your weather" : selectedCity);
        ((TextView) todayPage.findViewById(R.id.weatherDateTime)).setText(weather == null ? "" : weather.getDateTimeLine());
        ((TextView) todayPage.findViewById(R.id.weatherTemperature)).setText(weather == null ? "—" : weather.getTemperatureLine());
        ((TextView) todayPage.findViewById(R.id.weatherCondition)).setText(weather == null
                ? (weatherLoading ? "Loading current weather…" : "") : weather.getConditionLabel());
        ((TextView) todayPage.findViewById(R.id.weatherHumidity)).setText(weather == null ? "—" : weather.getHumidityLine());
        ((TextView) todayPage.findViewById(R.id.weatherWind)).setText(weather == null ? "—" : weather.getWindLine());
        ((ImageView) todayPage.findViewById(R.id.weatherIllustration)).setImageResource(
                weather != null && sunny(weather.getConditionLabel()) ? R.drawable.lens_mascot : R.drawable.lens_cloud);
        todayPage.findViewById(R.id.weatherProgress).setVisibility(weatherLoading ? View.VISIBLE : View.GONE);
        TextView error = todayPage.findViewById(R.id.weatherError);
        error.setText(selectedCity.isEmpty() ? "Add a city in Cities to see its weather." : weatherError);
        error.setVisibility(selectedCity.isEmpty() || !weatherError.isEmpty() ? View.VISIBLE : View.GONE);
        todayPage.findViewById(R.id.todayRetry).setVisibility(!weatherError.isEmpty() ? View.VISIBLE : View.GONE);
        todayPage.findViewById(R.id.weatherInsightsButton).setEnabled(weather != null);
        applyTheme();
    }

    private void loadForecast() {
        if (weather == null || forecastLoading) return;
        final int request = generation;
        final double latitude;
        final double longitude;
        try {
            JSONObject coordinates = new JSONObject(weather.getRawApiJson()).getJSONObject("coord");
            latitude = coordinates.getDouble("lat");
            longitude = coordinates.getDouble("lon");
        } catch (Exception e) {
            forecastError = "The weather response did not include this city's coordinates.";
            renderForecast();
            return;
        }
        forecastLoading = true;
        forecastError = "";
        renderForecast();
        forecastTask = executor.submit(() -> {
            ForecastData result = null;
            String error = "";
            try { result = ForecastRepository.fetch(latitude, longitude); }
            catch (Exception e) { error = e instanceof java.io.IOException
                    ? "Could not reach the forecast service. Check your connection and retry."
                    : e.getMessage() == null ? "Could not read the forecast." : e.getMessage(); }
            final ForecastData data = result;
            final String message = error;
            runOnUiThread(() -> {
                if (!active(request)) return;
                forecastLoading = false;
                forecast = data;
                forecastError = message;
                renderForecast();
            });
        });
    }

    private void renderForecast() {
        forecastPage.findViewById(R.id.forecastMap).setEnabled(!selectedCity.isEmpty());
        forecastPage.findViewById(R.id.forecastWeather).setEnabled(!selectedCity.isEmpty());
        LinearLayout content = forecastPage.findViewById(R.id.forecastPeriods);
        content.removeAllViews();
        TextView status = forecastPage.findViewById(R.id.forecastStatus);
        boolean loading = forecastLoading || weatherLoading;
        forecastPage.findViewById(R.id.forecastProgress).setVisibility(loading ? View.VISIBLE : View.GONE);
        forecastPage.findViewById(R.id.forecastRetry).setVisibility(!forecastError.isEmpty() || !weatherError.isEmpty() ? View.VISIBLE : View.GONE);
        if (forecast == null) {
            status.setVisibility(View.VISIBLE);
            status.setText(selectedCity.isEmpty() ? "Add a city in Cities to explore its forecast."
                    : loading ? "Loading the forecast for " + selectedCity + "…"
                    : !forecastError.isEmpty() ? forecastError
                    : !weatherError.isEmpty() ? weatherError : "Select Forecast to load the outlook.");
            applyTheme();
            return;
        }
        status.setText("");
        status.setVisibility(View.GONE);
        java.time.LocalDateTime localNow = java.time.LocalDateTime.now(forecast.timezone);
        LocalDate today = localNow.toLocalDate();
        LinearLayout hourlyCard = card();
        android.widget.HorizontalScrollView hourlyScroll = new android.widget.HorizontalScrollView(this);
        hourlyScroll.setId(R.id.forecastHourlyScroll);
        hourlyScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout hours = new LinearLayout(this);
        hours.setId(R.id.forecastHours);
        hours.setOrientation(LinearLayout.HORIZONTAL);
        hours.setBaselineAligned(false);
        hours.setGravity(android.view.Gravity.TOP);
        int hourCount = 0;
        for (ForecastData.Slot slot : forecast.hourly) {
            if (!slot.localTime.toLocalDate().equals(today) || slot.localTime.getHour() < localNow.getHour()) continue;
            LinearLayout hour = new LinearLayout(this);
            hour.setOrientation(LinearLayout.VERTICAL);
            hour.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
            hour.setPadding(dp(4), dp(8), dp(4), dp(8));
            String time = slot.localTime.format(DateTimeFormatter.ofPattern("h a", Locale.US));
            hour.addView(alignedForecastLabel(time, 12, 28));
            hour.addView(forecastIcon(slot.condition, slot.isDay), new LinearLayout.LayoutParams(dp(32), dp(32)));
            hour.addView(alignedForecastLabel(String.format(Locale.US, "%.0f°", slot.temperature), 20, 36));
            if (hourCount++ == 0) {
                android.graphics.drawable.GradientDrawable pill = new android.graphics.drawable.GradientDrawable();
                pill.setColor(androidx.core.graphics.ColorUtils.blendARGB(
                        getColor(R.color.lens_lavender), android.graphics.Color.WHITE, 0.65f));
                pill.setCornerRadius(dp(20));
                hour.setBackground(pill);
            }
            hour.setScreenReaderFocusable(true);
            hour.setContentDescription(time + ", " + slot.condition + ", " + slot.temperature + " degrees Celsius");
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(64), -2);
            params.setMarginEnd(dp(6));
            hours.addView(hour, params);
        }
        if (hourCount == 0) hours.addView(text("Today's hourly forecast is unavailable.", 14));
        hourlyScroll.addView(hours);
        hourlyCard.addView(hourlyScroll);
        content.addView(hourlyCard);

        LinearLayout dailyCard = card();
        LinearLayout dailyHeading = new LinearLayout(this);
        dailyHeading.setGravity(android.view.Gravity.CENTER_VERTICAL);
        dailyHeading.addView(text("Daily forecast", 18), new LinearLayout.LayoutParams(0, -2, 1));
        TextView probabilityLegend = text("Precip. chance (%)", 11);
        probabilityLegend.setTooltipText("Highest hourly precipitation probability for each day");
        dailyHeading.addView(probabilityLegend);
        dailyCard.addView(dailyHeading);
        LinearLayout dailyRows = new LinearLayout(this);
        dailyRows.setId(R.id.forecastDailyRows);
        dailyRows.setOrientation(LinearLayout.VERTICAL);
        double weekLow = Double.POSITIVE_INFINITY;
        double weekHigh = Double.NEGATIVE_INFINITY;
        for (ForecastData.Day day : forecast.daily) {
            weekLow = Math.min(weekLow, day.low);
            weekHigh = Math.max(weekHigh, day.high);
        }
        for (ForecastData.Day day : forecast.daily) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setBaselineAligned(false);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(10), 0, dp(10));
            String label = day.date.equals(today) ? "Today"
                    : day.date.format(DateTimeFormatter.ofPattern("EEE", Locale.US));
            TextView date = text(label, 14);
            date.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
            row.addView(date, new LinearLayout.LayoutParams(dp(52), -2));
            LinearLayout weatherCell = new LinearLayout(this);
            weatherCell.setOrientation(LinearLayout.VERTICAL);
            weatherCell.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
            weatherCell.addView(forecastIcon(day.condition, true), new LinearLayout.LayoutParams(dp(32), dp(28)));
            TextView rain = alignedForecastLabel(Double.isNaN(day.rainProbability) ? "—"
                    : String.format(Locale.US, "%.0f%%", day.rainProbability * 100), 10, 20);
            weatherCell.addView(rain);
            row.addView(weatherCell, new LinearLayout.LayoutParams(dp(42), -2));
            TextView low = text(String.format(Locale.US, "%.0f°", day.low), 14);
            low.setGravity(android.view.Gravity.END);
            row.addView(low, new LinearLayout.LayoutParams(dp(38), -2));
            View range = new edu.uiuc.weatherlens.forecast.TemperatureRangeView(this, day.low, day.high, weekLow, weekHigh);
            LinearLayout.LayoutParams rangeParams = new LinearLayout.LayoutParams(0, dp(16), 1);
            rangeParams.setMargins(dp(10), 0, dp(10), 0);
            row.addView(range, rangeParams);
            TextView high = text(String.format(Locale.US, "%.0f°", day.high), 14);
            high.setGravity(android.view.Gravity.END);
            row.addView(high, new LinearLayout.LayoutParams(dp(38), -2));
            row.setScreenReaderFocusable(true);
            row.setContentDescription(day.date + ", " + day.condition + ", low " + day.low
                    + ", high " + day.high + " degrees Celsius, "
                    + (Double.isNaN(day.rainProbability) ? "precipitation chance unavailable"
                    : String.format(Locale.US, "maximum precipitation chance %.0f percent", day.rainProbability * 100)));
            if (dailyRows.getChildCount() > 0) {
                View divider = new View(this);
                divider.setBackgroundColor(androidx.core.graphics.ColorUtils.setAlphaComponent(
                        android.graphics.Color.parseColor(theme.getTextColor()), 24));
                dailyRows.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
            }
            dailyRows.addView(row);
        }
        dailyCard.addView(dailyRows);
        content.addView(dailyCard);
        TextView attribution = text("Forecast by Open-Meteo", 12);
        attribution.setGravity(android.view.Gravity.CENTER);
        attribution.setPadding(0, dp(16), 0, dp(8));
        attribution.setText(android.text.Html.fromHtml("Forecast by <a href=\"https://open-meteo.com/\">Open-Meteo</a>", android.text.Html.FROM_HTML_MODE_LEGACY));
        attribution.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
        content.addView(attribution);
        applyTheme();
    }

    private TextView alignedForecastLabel(String value, int size, int minHeightDp) {
        TextView label = text(value, size);
        label.setGravity(android.view.Gravity.CENTER);
        label.setIncludeFontPadding(false);
        label.setSingleLine(true);
        int height = Math.max(dp(minHeightDp),
                (int) Math.ceil(size * getResources().getDisplayMetrics().scaledDensity * 1.4));
        label.setLayoutParams(new LinearLayout.LayoutParams(-1, height));
        return label;
    }

    private ImageView forecastIcon(String condition, boolean isDay) {
        ImageView icon = new ImageView(this);
        int drawable = sunny(condition)
                ? (isDay ? R.drawable.lens_mascot : R.drawable.lens_moon) : R.drawable.lens_cloud;
        String lower = condition.toLowerCase(Locale.ROOT);
        if (lower.contains("rain") || lower.contains("drizzle") || lower.contains("thunder")) drawable = R.drawable.lens_rain;
        if (lower.contains("snow")) drawable = R.drawable.lens_snow;
        icon.setImageResource(drawable);
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return icon;
    }

    private void renderCities() {
        LinearLayout container = citiesPage.findViewById(R.id.cityContainer);
        container.removeAllViews();
        ((TextView) citiesPage.findViewById(R.id.textView14)).setText(savedCities.isEmpty()
                ? "Your next destination starts here. Add a city to see today's weather and the forecast." : "");
        for (String city : savedCities) {
            LinearLayout row = card();
            LinearLayout heading = new LinearLayout(this);
            heading.setGravity(android.view.Gravity.CENTER_VERTICAL);
            TextView name = text(city, 24);
            name.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
            heading.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
            Button remove = new Button(this);
            remove.setText("Remove");
            remove.setTextSize(11);
            remove.setMinWidth(0);
            remove.setMinimumWidth(0);
            remove.setMinHeight(0);
            remove.setMinimumHeight(0);
            remove.setPadding(dp(8), 0, dp(8), 0);
            remove.setElevation(0);
            remove.setStateListAnimator(null);
            remove.setTag("secondary");
            remove.setContentDescription("Remove " + city);
            remove.setOnClickListener(v -> removeCity(city));
            heading.setMinimumHeight(dp(48));
            heading.addView(remove, new LinearLayout.LayoutParams(dp(68), dp(32)));
            // Keep the small pill easy to tap without making it visually larger.
            heading.post(() -> {
                android.graphics.Rect target = new android.graphics.Rect();
                remove.getHitRect(target);
                target.inset(-dp(8), -dp(8));
                heading.setTouchDelegate(new android.view.TouchDelegate(target, remove));
            });
            row.addView(heading);

            WeatherResult snapshot = citySnapshots.get(city);
            WeatherData data = snapshot != null && snapshot.isOk() ? snapshot.getData() : null;
            LinearLayout overview = new LinearLayout(this);
            overview.setGravity(android.view.Gravity.CENTER_VERTICAL);
            LinearLayout labels = new LinearLayout(this);
            labels.setOrientation(LinearLayout.VERTICAL);
            labels.addView(text("Today · °C", 13));
            labels.addView(text(data != null ? data.getConditionLabel()
                    : snapshot != null ? "Weather unavailable" : "Loading temperature…", 14));
            overview.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
            if (data != null) {
                ImageView icon = new ImageView(this);
                icon.setImageResource(sunny(data.getConditionLabel()) ? R.drawable.lens_mascot : R.drawable.lens_cloud);
                icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                overview.addView(icon, new LinearLayout.LayoutParams(dp(40), dp(48)));
            }
            TextView temperature = text(cityTemperature(data), 46);
            temperature.setPadding(dp(8), dp(8), 0, dp(8));
            temperature.setContentDescription(data == null ? "Temperature unavailable"
                    : cityTemperature(data) + " Celsius");
            overview.addView(temperature);
            row.addView(overview);
            container.addView(row);
        }
        applyTheme();
        if (selectedTab == R.id.navCities) loadCityTemperatures();
    }

    private String cityTemperature(WeatherData data) {
        if (data == null) return "—";
        try {
            double temperature = new JSONObject(data.getRawApiJson()).getJSONObject("main").getDouble("temp");
            return String.format(Locale.US, "%.0f°", temperature);
        } catch (Exception ignored) {
            return data.getTemperatureLine();
        }
    }

    private void loadCityTemperatures() {
        for (String city : savedCities) {
            Long timestamp = citySnapshotTimes.get(city);
            if (loadingCities.contains(city)
                    || (city.equals(selectedCity) && weatherLoading)
                    || (timestamp != null && System.currentTimeMillis() - timestamp < 10 * 60 * 1000L)) continue;
            loadingCities.add(city);
            executor.submit(() -> {
                WeatherResult result = WeatherRepository.fetchCurrentWeather(city, BuildConfig.WEATHER_API_KEY);
                runOnUiThread(() -> {
                    loadingCities.remove(city);
                    if (destroyed || isFinishing() || !savedCities.contains(city)) return;
                    citySnapshots.put(city, result);
                    citySnapshotTimes.put(city, System.currentTimeMillis());
                    renderCities();
                });
            });
        }
    }

    private void removeCity(String city) {
        database.removeCityForUser(username, city);
        refreshCities();
        showTab(selectedTab);
        Snackbar.make(findViewById(R.id.homeContent), "City removed", Snackbar.LENGTH_LONG)
                .setAction("Undo", v -> {
                    if (!database.cityExistsForUser(username, city)) database.addCityForUser(username, city);
                    refreshCities();
                    showTab(selectedTab);
                }).show();
    }

    private void addCity() {
        Intent intent = new Intent(this, AddCityActivity.class);
        intent.putExtra("username", username);
        intent.putExtra(ThemeContract.EXTRA_THEME_JSON, themeJson);
        if (getIntent().getBooleanExtra("TEST_BYPASS_GEOCODER", false)) intent.putExtra("TEST_BYPASS_GEOCODER", true);
        startActivity(intent);
    }

    private void openMap(String city) {
        if (!configured(BuildConfig.MAPS_API_KEY)) {
            new AlertDialog.Builder(this).setTitle("Map setup required")
                    .setMessage("Google Maps needs a separate Maps API key. Your weather and forecasts are available without it.")
                    .setPositiveButton("OK", null).show();
            return;
        }
        Intent intent = new Intent(this, MapActivity.class);
        intent.putExtra("city", city);
        intent.putExtra(ThemeContract.EXTRA_THEME_JSON, themeJson);
        startActivity(intent);
    }

    private void openInsights() {
        if (weather == null) return;
        Intent intent = new Intent(this, WeatherInsightsActivity.class);
        intent.putExtra(WeatherInsightsActivity.EXTRA_CITY_NAME, selectedCity);
        intent.putExtra(WeatherInsightsActivity.EXTRA_WEATHER_JSON, weather.getRawApiJson());
        intent.putExtra(ThemeContract.EXTRA_THEME_JSON, themeJson);
        startActivity(intent);
    }

    private void showMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add("Refresh weather").setEnabled(!selectedCity.isEmpty());
        menu.getMenu().add("City map").setEnabled(!selectedCity.isEmpty());
        menu.getMenu().add("Log out");
        menu.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals("Log out")) logout();
            else if (item.getTitle().equals("City map")) openMap(selectedCity);
            else loadCurrent();
            return true;
        });
        menu.show();
    }

    private void applyUserTheme(Bundle state) {
        String prompt = getIntent().getStringExtra(ThemeContract.EXTRA_THEME_PROMPT);
        String cached = state != null ? state.getString("theme") : getIntent().getStringExtra(ThemeContract.EXTRA_THEME_JSON);
        // A new prompt intentionally arrives without cached JSON; do not override it with an old theme.
        if ((cached == null || cached.isEmpty()) && (prompt == null || prompt.trim().isEmpty())) {
            cached = database.getThemeJson(username);
        }
        ThemeSpec parsed = ThemeParser.parseThemeJson(cached);
        if (parsed != null) { theme = parsed; themeJson = parsed.toJsonString(); applyTheme(); return; }
        applyTheme();
        if (!configured(BuildConfig.GEMINI_KEY) || prompt == null || prompt.trim().isEmpty()) return;
        executor.submit(() -> {
            try {
                JSONObject config = new JSONObject().put("responseMimeType", "application/json")
                        .put("responseJsonSchema", ThemePromptBuilder.buildJsonSchema());
                String raw = new GeminiApiClient().generateContent(ThemePromptBuilder.buildUserPrompt(prompt), config);
                ThemeSpec generated = ThemeParser.parseThemeJson(raw);
                if (generated == null) return;
                runOnUiThread(() -> {
                    if (destroyed || isFinishing()) return;
                    theme = generated;
                    themeJson = generated.toJsonString();
                    database.updateThemeJson(username, themeJson);
                    applyTheme();
                });
            } catch (Exception ignored) { /* Keep the immediately usable default palette. */ }
        });
    }

    private void applyTheme() {
        if (navigation != null) ThemeManager.applyTheme(this, theme);
        if (citySelector != null) {
            android.graphics.drawable.GradientDrawable popup = new android.graphics.drawable.GradientDrawable();
            popup.setColor(android.graphics.Color.parseColor(theme.getBackgroundColor()));
            popup.setCornerRadius(dp(20));
            popup.setStroke(dp(1), androidx.core.graphics.ColorUtils.setAlphaComponent(
                    android.graphics.Color.parseColor(theme.getBorderColor()), 45));
            popup.setPadding(dp(8), dp(8), dp(8), dp(8));
            citySelector.setPopupBackgroundDrawable(popup);
            android.graphics.Paint paint = new android.graphics.Paint();
            paint.setTextSize(16 * getResources().getDisplayMetrics().scaledDensity);
            paint.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
            int width = dp(180);
            for (String city : savedCities) width = Math.max(width, (int) Math.ceil(paint.measureText(city)) + dp(76));
            citySelector.setDropDownWidth(Math.min(width, getResources().getDisplayMetrics().widthPixels - dp(40)));
        }
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setTag("lens_card");
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(12);
        card.setLayoutParams(params);
        return card;
    }

    private TextView text(String value, int size) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setPadding(0, dp(3), 0, dp(3));
        return text;
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density); }
    private boolean active(int request) { return !destroyed && !isFinishing() && request == generation; }
    private static boolean sunny(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.contains("clear") || lower.contains("sun");
    }
    private static boolean configured(String value) {
        return value != null && !value.trim().isEmpty() && !value.contains("{");
    }

    private void logout() {
        // Temporary UI-test accounts must not sign out the remembered real user.
        LoginSession.clearIfCurrentUser(this, username);
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra(LoginSession.SHOW_SIGN_IN, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override public void onSaveInstanceState(Bundle state) {
        state.putString("city", selectedCity);
        state.putInt("tab", selectedTab);
        state.putString("theme", themeJson);
        super.onSaveInstanceState(state);
    }

    @Override public void onBackPressed() {
        if (selectedTab != R.id.navToday && !selectedCity.isEmpty()) showTab(R.id.navToday);
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        destroyed = true;
        ++generation;
        executor.shutdownNow();
        if (database != null) database.close();
        super.onDestroy();
    }
}
