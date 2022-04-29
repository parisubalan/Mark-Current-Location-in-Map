package com.parisubalan.googlemap;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;

import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener {

    private GoogleMap mapView;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationSettingsRequest locationSettingsRequest;
    private LocationManager locationManager;
    private LocationRequest locationRequest;
    private SettingsClient settingsClient;
    private LocationCallback locationCallback;
    Button locateBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialization();
    }

    public void initialization() {
        locateBtn = findViewById(R.id.locateBtn);
        locateBtn.setOnClickListener(this);
        locationInitialize();
        mapLoading();
        locationSettingRequest();
        locationCallBack();
    }

    public void locationInitialize() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        settingsClient = LocationServices.getSettingsClient(this);

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10 * 1000);
        locationRequest.setFastestInterval(2 * 1000);
    }

    public void mapLoading() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
    }

    public void locationSettingRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        locationSettingsRequest = builder.build();
        builder.setAlwaysShow(true);
    }

    public void locationCallBack() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        setMarkerOnCurrentLocation(location);
                    }
                }
            }
        };
    }

    private void setMarkerOnCurrentLocation(Location location) {
        LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
        mapView.addMarker(new MarkerOptions().position(current).title("Current Location"));
        mapView.animateCamera(CameraUpdateFactory.newLatLngZoom(current, 20.0f));
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mapView = googleMap;
        mapView.isIndoorEnabled();
        mapView.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        userInterfaceSetting();
        mapGestures();
    }

    public void userInterfaceSetting() {
        UiSettings uiSettings = mapView.getUiSettings();
        uiSettings.setIndoorLevelPickerEnabled(true);
        uiSettings.setMyLocationButtonEnabled(true);
        uiSettings.setMapToolbarEnabled(true);
        uiSettings.setCompassEnabled(true);
        uiSettings.setZoomControlsEnabled(true);
    }

    public void mapGestures() {
        UiSettings gestures = mapView.getUiSettings();
        gestures.setRotateGesturesEnabled(true);
        gestures.setScrollGesturesEnabled(true);
        gestures.setTiltGesturesEnabled(true);
        gestures.setZoomGesturesEnabled(true);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.locateBtn) {
            getLocation();
        }
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    11);
        } else {
            getGpsService();
        }
    }

    private void getGpsService() {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            getCurrentLatLon();
        } else {
            settingsClient.checkLocationSettings(locationSettingsRequest)
                    .addOnSuccessListener(this, locationSettingsResponse -> getCurrentLatLon())
                    .addOnFailureListener(this, e -> {
                        int statusCode = ((ApiException) e).getStatusCode();
                        if (statusCode == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                            try {
                                ResolvableApiException rae = (ResolvableApiException) e;
                                rae.startResolutionForResult(this, 11);
                            } catch (IntentSender.SendIntentException sie) {
                                Log.i(TAG, "PendingIntent unable to execute request.");
                            }
                        }
                    });
        }
    }

    @SuppressLint("MissingPermission")
    public void getCurrentLatLon() {
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                setMarkerOnCurrentLocation(location);
            } else {
                fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
            }
        });
    }

    public void settings() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Please Enable Permission").setCancelable(false)
                .setPositiveButton("Yes", (dialog, which) -> startActivity(new Intent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS)))
                .setNegativeButton("No", (dialog, which) -> dialog.cancel());
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 11) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            } else if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)
                    && ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_COARSE_LOCATION)) {
                Toast.makeText(this, "Permission was Denied", Toast.LENGTH_SHORT).show();
            }
            else{
                settings();
                Toast.makeText(this, "Please Turn On Location", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 11) {
                getCurrentLatLon();
            }
        }
    }

    public void locationClear()
    {
        if (fusedLocationProviderClient != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        initialization();
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationClear();
    }

    @Override
    protected void onStop() {
        super.onStop();
        locationClear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationClear();
    }
}