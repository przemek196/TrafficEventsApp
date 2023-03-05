package com.example.trafficeventsapp;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;


import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;


public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback, LocationListener, View.OnClickListener {

    private SupportMapFragment mapFragment;
    private GoogleMap mGoogleMap;
    private LocationManager locationManager;
    private LocationListener locationListener;
    View bottomSheet;
    private LinearLayout lin_lay_menu;
    private ImageButton btnAddMaker;
    private ImageButton btn_speed_control_maker;
    private ImageButton btn_traffic_accident_maker;
    private ImageButton btn_police_voiture_maker;
    private Button logOut;
    private boolean menu_visibility = false;
    private int minBlckAddMaker = 1;
    private double distance = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        findViews();
        checkMyPermission();

        mapFragment.getMapAsync(this);
    }

    private void findViews() {
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.maps);
        lin_lay_menu = (LinearLayout) findViewById(R.id.linear_layout_menu);
        bottomSheet = findViewById(R.id.bottom_sheet);

        btnAddMaker = (ImageButton) findViewById(R.id.buttonAddMaker);
        btn_speed_control_maker = (ImageButton) findViewById(R.id.btn_speed_control_maker);
        btn_traffic_accident_maker = (ImageButton) findViewById(R.id.btn_traffic_accident_maker);
        btn_police_voiture_maker = (ImageButton) findViewById(R.id.btn_police_voituer_maker);


        logOut = (Button) findViewById(R.id.btnLogOut);
        logOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                mAuth.signOut();
                startActivity(new Intent(MapActivity.this, LoginActivity.class));

            }
        });

        btnAddMaker.setOnClickListener(this);
        btn_speed_control_maker.setOnClickListener(this);
        btn_traffic_accident_maker.setOnClickListener(this);
        btn_police_voiture_maker.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

        if (!v.isEnabled())
            Toast.makeText(getApplicationContext(), "Przycisk jest nieaktywny. Poczekaj 3 minuty", Toast.LENGTH_SHORT).show();

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

        if (v.getId() != R.id.buttonAddMaker) {
            new CountDownTimer(minBlckAddMaker * 10 * 1000, 1000) {
                public void onTick(long millisUntilFinished) {
                }

                public void onFinish() {
                    //called after minBlckAddMaker minutes
                    setButtonsEnabled(true);
                    lin_lay_menu.setVisibility(View.VISIBLE);
                }
            }.start();
            setButtonsEnabled(false);
            lin_lay_menu.setVisibility(View.GONE);

        }
    }

    private void setButtonsEnabled(Boolean b) {
        btn_speed_control_maker.setEnabled(b);
        btn_traffic_accident_maker.setEnabled(b);
        btn_police_voiture_maker.setEnabled(b);
        btnAddMaker.setEnabled(b);


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
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
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
            int ic_red = 5;

            Bitmap imageBitmap = null, resizedBitmap = null;
            MarkerOptions markerOptions = null;
            //add makers on map
            switch (id) {
                case R.id.btn_speed_control_maker:
                    imageBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_map_speed);
                    resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, imageBitmap.getWidth() / ic_red, imageBitmap.getHeight() / ic_red, false);
                    break;
                case R.id.btn_traffic_accident_maker:
                    imageBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_map_crash);
                    resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, imageBitmap.getWidth() / ic_red, imageBitmap.getHeight() / ic_red, false);
                    break;
                case R.id.btn_police_voituer_maker:
                    imageBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_map_vouiter_police);
                    resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, imageBitmap.getWidth() / ic_red, imageBitmap.getHeight() / ic_red, false);
                    break;
                default:
                    break;
            }
            markerOptions = new MarkerOptions().position(myLocation).icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap));
            String idd = UUID.randomUUID().toString();
            markerOptions.title(idd);
            Marker marker = mGoogleMap.addMarker(markerOptions);
            DatabaseClass databaseClass = new DatabaseClass(this);
            databaseClass.addMakerToDatabase(markerOptions, marker,String.valueOf(id));

        }
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

        displayCurrentLocation();
        startLocationUpdates();

    }

    private void displayCurrentLocation() {
        // Sprawdź, czy aplikacja ma uprawnienia do dostępu do lokalizacji.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Uzyskaj odwołanie do LocationManager.
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

            // Sprawdź, czy GPS jest włączony.
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                // Pobierz ostatnią znaną lokalizację z GPS.
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                // Jeśli lokalizacja nie jest znana, zarejestruj LocationListener, aby czekać na aktualizacje lokalizacji.
                if (lastKnownLocation == null) {
                    LocationListener locationListener = new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            // Zaktualizuj kamerę GoogleMap z nową lokalizacją.
                            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                            mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(15));

                            // Wyłącz LocationListener, gdy lokalizacja zostanie znaleziona.
                            locationManager.removeUpdates(this);
                        }

                        @Override
                        public void onProviderEnabled(String provider) {
                            // Kod do obsługi włączenia dostawcy lokalizacji.
                        }

                        @Override
                        public void onProviderDisabled(String provider) {
                            // Kod do obsługi wyłączenia dostawcy lokalizacji.
                        }

                        @Override
                        public void onStatusChanged(String provider, int status, Bundle extras) {
                            // Kod do obsługi zmiany statusu dostawcy lokalizacji.
                        }
                    };
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                }
                // Jeśli lokalizacja jest znana, zaktualizuj kamerę GoogleMap.
                else {
                    LatLng latLng = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                    mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(15));
                }
            } else {
                // Poproś użytkownika o włączenie GPS.
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            // Poproś użytkownika o uprawnienia do dostępu do lokalizacji.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    private void startLocationUpdates() {
        // Sprawdź, czy aplikacja ma uprawnienia do dostępu do lokalizacji.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Uzyskaj odwołanie do LocationManager.
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

            // Sprawdź, czy GPS jest włączony.
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                // Zarejestruj LocationListener do nasłuchiwania na aktualizacje lokalizacji.
                LocationListener locationListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        // Zaktualizuj kamerę GoogleMap z nową lokalizacją.
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                        mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(15));


                    }

                    @Override
                    public void onProviderEnabled(String provider) {
                        // Kod do obsługi włączenia dostawcy lokalizacji.
                    }

                    @Override
                    public void onProviderDisabled(String provider) {
                        // Kod do obsługi wyłączenia dostawcy lokalizacji.
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                        // Kod do obsługi zmiany statusu dostawcy lokalizacji.
                    }
                };
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            } else {
                // Poproś użytkownika o włączenie GPS.
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            // Poproś użytkownika o uprawnienia do dostępu do lokalizacji.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
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