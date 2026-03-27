package edu.uiuc.cs427app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private String currentUsername;

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
            @Override
            public void onClick(View v) {
                navigateToLogin();
            }
        });
    }

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

    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
