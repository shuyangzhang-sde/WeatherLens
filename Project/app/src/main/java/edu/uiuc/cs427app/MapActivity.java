package edu.uiuc.cs427app;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    String city;
    double latitude;
    double longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        city = getIntent().getStringExtra("city");

        TextView cityName = findViewById(R.id.cityName);
        TextView latLong = findViewById(R.id.latLong);

        cityName.setText(city);

        if (city.equalsIgnoreCase("chicago")) {
            latitude = 41.8781;
            longitude = -87.6298;
        }
        else if (city.equalsIgnoreCase("new york")) {
            latitude = 40.7128;
            longitude = -74.0060;
        }
        else if (city.equalsIgnoreCase("los angeles")) {
            latitude = 34.0522;
            longitude = -118.2437;
        }
        else {
            latitude = 39.8283;
            longitude = -98.5795;
        }

        latLong.setText("Lat: " + latitude + ", Lon: " + longitude);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "Map is ready", Toast.LENGTH_SHORT).show();

        googleMap.getUiSettings().setZoomControlsEnabled(true);

        LatLng location = new LatLng(latitude, longitude);

        googleMap.addMarker(new MarkerOptions().position(location).title(city));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 12));
    }
}