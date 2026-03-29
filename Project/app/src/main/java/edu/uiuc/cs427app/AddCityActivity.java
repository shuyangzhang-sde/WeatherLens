package edu.uiuc.cs427app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AddCityActivity extends AppCompatActivity {

    private EditText editTextCityName;
    private Button buttonSaveCity;
    private Button buttonCancel;
    private String currentUsername;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_city);

        currentUsername = getIntent().getStringExtra("username");
        databaseHelper = new DatabaseHelper(this);

        editTextCityName = findViewById(R.id.editTextCityName);
        buttonSaveCity = findViewById(R.id.buttonSaveCity);
        buttonCancel = findViewById(R.id.buttonCancel);

        buttonSaveCity.setOnClickListener(v -> saveCity());
        buttonCancel.setOnClickListener(v -> finish());
    }

    private void saveCity() {
        String cityName = editTextCityName.getText().toString().trim();

        if (cityName.isEmpty()) {
            Toast.makeText(this, "Please enter a city name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (databaseHelper.cityExistsForUser(currentUsername, cityName)) {
            Toast.makeText(this, "City already added", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean success = databaseHelper.addCityForUser(currentUsername, cityName);

        if (success) {
            Toast.makeText(this, "City added successfully", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to add city", Toast.LENGTH_SHORT).show();
        }
    }
}