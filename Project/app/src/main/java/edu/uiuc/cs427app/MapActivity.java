package edu.uiuc.cs427app;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import edu.uiuc.cs427app.theme.ThemeContract;
import edu.uiuc.cs427app.theme.ThemeManager;
import edu.uiuc.cs427app.theme.ThemeParser;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    String city;
    double latitude;
    double longitude;

    //gets city name and coords, and then sets up the map
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        String themeJson = getIntent().getStringExtra(ThemeContract.EXTRA_THEME_JSON);
        if (themeJson != null && !themeJson.isEmpty()) {
            ThemeManager.applyTheme(this, ThemeParser.parseThemeJsonOrDefault(themeJson));
        }

        city = getIntent().getStringExtra("city");

        TextView cityName = findViewById(R.id.cityName);
        TextView coords = findViewById(R.id.cityCoordinates);

        cityName.setText(city);

        //use geocoder like in add city
        android.location.Geocoder geocoder = new android.location.Geocoder(this);

        try {
            java.util.List<android.location.Address> list = geocoder.getFromLocationName(city, 1);

            if (list != null && list.size() > 0) {
                android.location.Address address = list.get(0);
                latitude = address.getLatitude();
                longitude = address.getLongitude();
            } else {
                latitude = 0;
                longitude = 0;
            }

        } catch (Exception e) {
            latitude = 0;
            longitude = 0;
        }

        coords.setText("Latitude: " + latitude + ", Longitude: " + longitude);

        SupportMapFragment mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        if (mapFrag != null) {
            mapFrag.getMapAsync(this);
        }
    }

    //once map loads: add marker, zoom in
    @Override
    public void onMapReady(GoogleMap googleMap) {
        //Toast.makeText(this, "map is ready", Toast.LENGTH_SHORT).show(); //toast for testing

        googleMap.getUiSettings().setZoomControlsEnabled(true);

        LatLng coord = new LatLng(latitude, longitude);

        googleMap.addMarker(new MarkerOptions().position(coord).title(city));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coord, 10));
    }

    //sends user back to city list when the back arrow is clicked
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}