package com.example.finalassignment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.DecodeCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.zxing.Result;

import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {
    private static final long RESET_DELAY_MS = 7000;
    private CodeScanner mCodeScanner;
    private TextView scannedTextView;
    private Button scanButton;

    private FusedLocationProviderClient fusedLocationClient;

    private TextView distanceTextView;

    private boolean isScanning = false;

    private double latitude, longitude;

    private static final int CAMERA_PERMISSION_REQUEST = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        distanceTextView = (TextView) findViewById(R.id.distanceText);


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        CodeScannerView scannerView = findViewById(R.id.scanner_view);
        mCodeScanner = new CodeScanner(this, scannerView);
        scanButton = findViewById(R.id.scan_button);

        scannedTextView = findViewById(R.id.scannedText);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
           ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        } else {

            scanButton.setOnClickListener(view -> {
                if(!isScanning) {
                    scannedTextView.setText("Searching QR code..");
                    initializeCodeScanner();
                }
                isScanning = false;
            });
        }
    }

    protected void onStart() {
        super.onStart();

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            System.out.println("Coarse and Fine location permission not granted!");

            String[] permissions = {
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
            };
            ActivityCompat.requestPermissions(this,permissions, 42);

            return;
        } else {
            System.out.println("Permission check succeeded!");
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Logic to handle location object
                            latitude = Double.parseDouble(Double.toString(location.getLatitude()));
                            longitude = Double.parseDouble(Double.toString(location.getLongitude()));
                            System.out.println("current location \n" + "lat:" + latitude
                                    + "long: " + longitude);
//
                        }
                    }
                });
    }

    // here to request the missing permissions, and then overriding

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0; i < permissions.length; i++) {
            System.out.println(permissions[i] + " : " + Integer.toString(grantResults[i]));
        }
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeCodeScanner();
            } else {
                System.out.println("Access Camera permission Denied!");
            }
        }
    }

    private double calculateDistance(double currentLatitude, double currentLongitude, double targetLatitude, double targetLongitude) {
        final int R = 6371; // Radius of the Earth

        double latDistance = Math.toRadians(targetLatitude - currentLatitude);
        double lonDistance = Math.toRadians(targetLongitude - currentLongitude);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(currentLatitude)) * Math.cos(Math.toRadians(targetLatitude))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    private void showLocation(double latitude, double longitude) {
        String geoUri = "geo:" + latitude + "," + longitude;
        Uri gmmIntentUri = Uri.parse(geoUri);

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            // If Google Maps is not installed, you can open a web browser with the location
            String mapsUrl = "https://www.google.com/maps/search/?api=1&query=" + latitude + "," + longitude;
            Uri webUri = Uri.parse(mapsUrl);
            Intent webIntent = new Intent(Intent.ACTION_VIEW, webUri);
            startActivity(webIntent);
        }
    }

    private void showLocationDistance(double latitude, double longitude) {
        double distance = calculateDistance(this.latitude, this.longitude, latitude, longitude);
        DecimalFormat df = new DecimalFormat("#.##");
        String formattedDistance = df.format(distance);
        distanceTextView.setText("Location: " + formattedDistance + " km");
    }
    private void initializeCodeScanner() {
        mCodeScanner.startPreview();

        mCodeScanner.setDecodeCallback(result -> runOnUiThread(() -> {
            String scannedText = result.getText();
            scannedTextView.setText(scannedText);
            if (scannedText != null) {
                if (scannedText.startsWith("https://maps")) {
                    Uri uri = Uri.parse(scannedText);
                    String query = uri.getQuery();

                    if (query != null && query.contains("q=")) {
                        String[] queryParts = query.split("q=");
                        if (queryParts.length > 1) {
                            String locationInfo = queryParts[1];
                            String[] latLng = locationInfo.split(",");
                            if (latLng.length == 2) {
                                try {
                                    double newLatitude = Double.parseDouble(latLng[0]);
                                    double newLongitude = Double.parseDouble(latLng[1]);

                                    showLocation(newLatitude, newLongitude);

                                    showLocationDistance(newLatitude, newLongitude);
                                } catch (NumberFormatException e) {
                                    // Handle invalid latitude or longitude
                                }
                            }
                        }
                    }
                }
            }
        }));
    }



    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        mCodeScanner.releaseResources();
        super.onPause();
    }
}
