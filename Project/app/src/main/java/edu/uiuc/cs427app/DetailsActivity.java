package edu.uiuc.cs427app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class DetailsActivity extends AppCompatActivity implements View.OnClickListener {

    private String cityName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        cityName = getIntent().getStringExtra("city");
        if (cityName == null) {
            cityName = "";
        }

        String welcome = "Welcome to the " + cityName;
        String cityWeatherInfo = "Detailed information about the weather of " + cityName;

        TextView welcomeMessage = findViewById(R.id.welcomeText);
        TextView cityInfoMessage = findViewById(R.id.cityInfo);
        Button buttonMap = findViewById(R.id.mapButton);

        welcomeMessage.setText(welcome);
        cityInfoMessage.setText(cityWeatherInfo);

        buttonMap.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.mapButton) {
            if (cityName == null || cityName.trim().isEmpty()) {
                Toast.makeText(this, "City not found", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri mapUri = Uri.parse("geo:0,0?q=" + Uri.encode(cityName));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, mapUri);

            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                Toast.makeText(this, "No map application found", Toast.LENGTH_SHORT).show();
            }
        }
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
}