package com.example.trafficeventsapp;


import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoLocation;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
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
    private boolean menu_visibility = false;
    private int minBlckAddMaker = 1;
    private double distance = 5000;
    private ImageButton menuButton;
    private GoogleMap gmCP;

    public interface OnMarkerInfoCallback {
        void onSuccess(com.example.trafficeventsapp.Marker markerInfo);

        void onFailure(String error);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        findViews();
        checkMyPermission();

        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(MapActivity.this, menuButton);
                popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());

                if (gmCP.isTrafficEnabled()) {
                    popup.getMenu().getItem(0).setTitle(getResources().getString(R.string.hide_traffic));
                } else {
                    popup.getMenu().getItem(0).setTitle(getResources().getString(R.string.show_traffic));
                }

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.menu_item_show_traffic:
                                if (gmCP != null && !gmCP.isTrafficEnabled()) {
                                    gmCP.setTrafficEnabled(true);
                                } else {
                                    gmCP.setTrafficEnabled(false);
                                }
                                return true;
                            case R.id.menu_item_logout:
                                new AlertDialog.Builder(MapActivity.this)
                                        .setTitle("Wylogowanie")
                                        .setMessage("Czy na pewno chcesz się wylogować?")
                                        .setPositiveButton("Tak", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                                                mAuth.signOut();
                                                startActivity(new Intent(MapActivity.this, LoginActivity.class));
                                            }
                                        })
                                        .setNegativeButton("Nie", null)
                                        .show();
                                return true;
                        }
                        return true;
                    }
                });
                popup.show();
            }
        });

        mapFragment.getMapAsync(this);
    }

    private void findViews() {
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.maps);
        lin_lay_menu = (LinearLayout) findViewById(R.id.linear_layout_menu);
        bottomSheet = findViewById(R.id.bottom_sheet);
        menuButton = findViewById(R.id.menu_button);
        btnAddMaker = (ImageButton) findViewById(R.id.buttonAddMaker);
        btn_speed_control_maker = (ImageButton) findViewById(R.id.btn_speed_control_maker);
        btn_traffic_accident_maker = (ImageButton) findViewById(R.id.btn_traffic_accident_maker);
        btn_police_voiture_maker = (ImageButton) findViewById(R.id.btn_police_voituer_maker);
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
            String eventId = "";
            switch (id) {
                case R.id.btn_speed_control_maker:
                    imageBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_map_speed);
                    resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, imageBitmap.getWidth() / ic_red, imageBitmap.getHeight() / ic_red, false);
                    eventId = "speedcntrl";
                    break;
                case R.id.btn_traffic_accident_maker:
                    imageBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_map_crash);
                    resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, imageBitmap.getWidth() / ic_red, imageBitmap.getHeight() / ic_red, false);
                    eventId = "accidnt";
                    break;
                case R.id.btn_police_voituer_maker:
                    imageBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_map_vouiter_police);
                    resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, imageBitmap.getWidth() / ic_red, imageBitmap.getHeight() / ic_red, false);
                    eventId = "polivoit";
                    break;
                default:
                    break;
            }

            DatabaseClass databaseClass = new DatabaseClass(this);
            GeoLocation geoLocation = new GeoLocation(latitude, longitude);
            markerOptions = new MarkerOptions().position(myLocation).icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap));
            String idd = UUID.randomUUID().toString();
            markerOptions.title(idd);

            databaseClass.checkMarkersExist(markerOptions, mGoogleMap, geoLocation, eventId, new DatabaseClass.OnMarkersExistListener() {
                @Override
                public void onMarkerExist(boolean exist) {
                    if (exist) {
                        Log.e(TAG, "Istnieje");
                        // marker istnieje
                    } else {
                        Log.e(TAG, "Nie istnieje");
                        // marker nie istnieje
                    }
                }
            });
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
        gmCP = googleMap;

        try {
            boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_dark));
            if (!success) {
                Log.e(TAG, "Nie udało się ustawić stylu mapy.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Nie udało się wczytać stylu mapy. Błąd: ", e);
        }

        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                BottomSheetDialog dialog = new BottomSheetDialog(MapActivity.this);
                View view = getLayoutInflater().inflate(R.layout.marker_info, null);
                dialog.setContentView(view);

                String markerId = marker.getTitle();
                DatabaseClass dbClass = new DatabaseClass(MapActivity.this);
                //  Map<String, Object> markerInfo = dbClass.getMarkerInfo(markerId);

                dbClass.getMarkerInfo(markerId, new OnMarkerInfoCallback() {
                    @Override
                    public void onSuccess(com.example.trafficeventsapp.Marker markerInfo) {
                        TextView eventName = view.findViewById(R.id.eventName);
                        TextView creatorName = view.findViewById(R.id.creatorName);
                        TextView creationTime = view.findViewById(R.id.creationTime);
                        TextView expireTime = view.findViewById(R.id.expireTime);
                        TextView refreshCount = view.findViewById(R.id.refreshCount);

                        String eventFullName = "";

                        switch (markerInfo.getEventID()) {
                            case "speedcntrl":
                                eventFullName = getResources().getString(R.string.speed_cntrl);
                                break;
                            case "accidnt":
                                eventFullName = getResources().getString(R.string.traff_acc);
                                break;
                            case "polivoit":
                                eventFullName = getResources().getString(R.string.polic_car);
                                break;
                            default:
                                break;
                        }

                        eventName.setText(eventFullName);
                        creatorName.setText(markerInfo.getCreatorName());
                        Date creatrion_date = new Date(markerInfo.getCreationTime());
                        DateFormat cr_df = new SimpleDateFormat("HH:mm:ss");
                        creationTime.setText(cr_df.format(creatrion_date));

                        long endTime = markerInfo.getExpirationTime();

                        Handler handler = new Handler();
                        Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                long currentTime = System.currentTimeMillis();
                                long remainingTime = (endTime - currentTime) / 1000;

                                if (remainingTime <= 0) {
                                    expireTime.setText("Czas minął");
                                } else {
                                    long hours = remainingTime / 3600;
                                    long minutes = (remainingTime % 3600) / 60;
                                    long seconds = remainingTime % 60;
                                    expireTime.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));
                                    handler.postDelayed(this, 1000);
                                }
                            }
                        };
                        handler.post(runnable);
                        refreshCount.setText(String.valueOf(markerInfo.getRefreshCount()));
                        dialog.show();
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "Error getting marker info: " + error);
                    }
                });
                return true;
            }
        });

        displayCurrentLocation();
        startLocationUpdates();

    }

    private void displayCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnownLocation == null) {
                    LocationListener locationListener = new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                            mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(15));

                            locationManager.removeUpdates(this);
                        }

                        @Override
                        public void onProviderEnabled(String provider) {
                        }

                        @Override
                        public void onProviderDisabled(String provider) {
                        }

                        @Override
                        public void onStatusChanged(String provider, int status, Bundle extras) {
                        }
                    };
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                } else {
                    LatLng latLng = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                    mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(15));
                }
            } else {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                LocationListener locationListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                        mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(15));
                        DatabaseClass dbClass = new DatabaseClass(getApplicationContext());
                        dbClass.updateGeoQuery(location, mGoogleMap);
                    }

                    @Override
                    public void onProviderEnabled(String provider) {
                    }

                    @Override
                    public void onProviderDisabled(String provider) {
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                    }
                };
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            } else {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }


    @SuppressLint("MissingPermission")
    private void enableMyLocation() {
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