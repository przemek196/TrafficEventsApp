package com.example.trafficeventsapp;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;


import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.IOException;
import java.io.InputStream;


public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback, LocationListener, View.OnClickListener {

    private SupportMapFragment mapFragment;
    private GoogleMap mGoogleMap;
    private LocationManager locationManager;
    private LocationListener locationListener;
    View bottomSheet;
    private LinearLayout lin_lay_menu;
    private ImageButton btnAddMaker;
    // private ImageButton btn_speed_control_maker;
    //private ImageButton btn_traffic_accident_maker;
    //private ImageButton btn_police_voiture_maker;
    private boolean menu_visibility = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        findViews();
        checkMyPermission();

        mapFragment.getMapAsync(this);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(15));
            }
        };
    }

    private void findViews() {
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.maps);
        lin_lay_menu = (LinearLayout) findViewById(R.id.linear_layout_menu);
        bottomSheet = findViewById(R.id.bottom_sheet);

        btnAddMaker = (ImageButton) findViewById(R.id.buttonAddMaker);
        ImageButton btn_speed_control_maker = (ImageButton) findViewById(R.id.btn_speed_control_maker);
        ImageButton btn_traffic_accident_maker = (ImageButton) findViewById(R.id.btn_traffic_accident_maker);
        ImageButton btn_police_voiture_maker = (ImageButton) findViewById(R.id.btn_police_voituer_maker);
        btnAddMaker.setOnClickListener(this);
        btn_speed_control_maker.setOnClickListener(this);
        btn_traffic_accident_maker.setOnClickListener(this);
        btn_police_voiture_maker.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.buttonAddMaker:
                show_makers_menu();
                break;
            case R.id.btn_speed_control_maker:
                addMakerOnMap(v.getId());
                break;
            case R.id.btn_traffic_accident_maker:
                addMakerOnMap(v.getId());
                break;
            case R.id.btn_police_voituer_maker:
                addMakerOnMap(v.getId());
                break;
            default:
                break;
        }


    }

    private void show_makers_menu() {
        if (lin_lay_menu.getVisibility() == View.GONE) {
            Animation showAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in_animation);
            lin_lay_menu.startAnimation(showAnimation);
            lin_lay_menu.setVisibility(View.VISIBLE);

        } else {
            lin_lay_menu.setVisibility(View.GONE);
        }
    }

    private void addMakerOnMap(int id) {



      /*  LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String provider = locationManager.getBestProvider(criteria, true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location location = locationManager.getLastKnownLocation(provider);
        if (location != null) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            LatLng myLocation = new LatLng(latitude, longitude);
            mGoogleMap.addMarker(new MarkerOptions().position(myLocation).title("My Location"));
        }*/

    }

    private void checkMyPermission() {
        Dexter.withContext(this).withPermission(Manifest.permission.ACCESS_FINE_LOCATION).withListener(new PermissionListener() {
            @Override
            public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                Toast.makeText(MapActivity.this, "Uprawnienia nadane", Toast.LENGTH_SHORT);
            }

            @Override
            public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), "");
                intent.setData(uri);
                startActivity(intent);
            }

            @Override
            public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                permissionToken.continuePermissionRequest();
            }
        }).check();
    }


    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mGoogleMap = googleMap;
        enableMyLocation();
        UiSettings uiSettings = mGoogleMap.getUiSettings();
        uiSettings.setScrollGesturesEnabled(false);
        uiSettings.setZoomGesturesEnabled(false);
        uiSettings.setMyLocationButtonEnabled(false);

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        //lastKnownLocation null where function didn't find a last known location
        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        LatLng latLng = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(15));

    }

    @SuppressLint("MissingPermission")
    private void enableMyLocation() {

        //Check if permissions are granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mGoogleMap.setMyLocationEnabled(true);
            return;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, (long) 0L, (float) 0, (android.location.LocationListener) locationListener);
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnownLocation != null) {
                    LatLng latLng = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                    mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(15));
                }
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
    }

    private String loadJSONFromAsset(String fileName) {
        String json = null;
        try {
            InputStream is = getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {

        }
        return json;
    }
}