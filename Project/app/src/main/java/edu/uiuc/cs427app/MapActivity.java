package edu.uiuc.cs427app;

import android.os.Bundle;
import android.webkit.WebView;
import android.widget.TextView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

public class MapActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        String city = getIntent().getStringExtra("city");

        TextView cityName = findViewById(R.id.cityName);
        TextView latLong = findViewById(R.id.latLong);
        WebView webView = findViewById(R.id.mapView);

        cityName.setText(city);

        // TEMP TEST VALUES (ill fix this later)
        double latitude = 40.1164;
        double longitude = -88.2434;

        latLong.setText("Lat: " + latitude + " , Lon: " + longitude);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());

        String url = "https://maps.google.com/maps?q=" + latitude + "," + longitude + "&t=&z=15&ie=UTF8&iwloc=&output=embed";

        webView.loadUrl(url);
    }
}