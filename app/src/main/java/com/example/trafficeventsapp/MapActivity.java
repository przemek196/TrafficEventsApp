package com.example.trafficeventsapp;


import static android.content.ContentValues.TAG;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoLocation;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.SphericalUtil;
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
    private ImageButton btn_displayCurrentDirection;
    private boolean menu_visibility = false;
    private int minBlckAddMaker = 1;
    private double distance = 5000;
    private ImageButton menuButton;
    private GoogleMap gmCP;
    private boolean isDirectionButtonPressed = true;
    private Marker mCurrentLocationMarker;
    private Location mLastKnownLocation;
    private Bitmap mMarkerBitmap;
    private Marker mMarker;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private float[] mGravity = new float[3];
    private float[] mGeomagnetic = new float[3];
    private float mBearing = 0f;
    private Location lastLocation;
    private boolean firstUpdate = false;
    private long lastUpdateTime = 0;
    DatabaseClass databaseClass;
    //   private LayoutInflater inflater_marker_window;
    private View layout_marker_window;
    private static final int REQUEST_HISTORY = 1;
    private ProgressBar mProgressBar;
    private FrameLayout frameL;
    private Marker historyMarker;

    public interface OnMarkerInfoCallback {
        void onSuccess(com.example.trafficeventsapp.Marker markerInfo);

        void onFailure(String error);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        mLastKnownLocation = new Location("");
        findViews();
        checkMyPermission();
        databaseClass = new DatabaseClass(this);

        frameL = (FrameLayout) findViewById(R.id.loading_layout);

        // Inicjalizacja sensora ruchu
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);


        layout_marker_window = findViewById(R.id.marker_info_layout);
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
                            case R.id.incident_history:
                                Intent historyIntent = new Intent(getApplicationContext(), HistoryActivity.class);
                                if(historyMarker!=null)
                                {
                                    historyMarker.remove();
                                    historyMarker=null;
                                }
                                mStartForResult.launch(historyIntent);
                                // startActivityForResult(historyIntent, REQUEST_HISTORY);

                                return true;
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

    private ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        double latitude = data.getDoubleExtra("latitude", 0.0);
                        double longitude = data.getDoubleExtra("longitude", 0.0);
                        String event_id = data.getStringExtra("ev_type");
                        btn_displayCurrentDirection.setVisibility(View.VISIBLE);
                        isDirectionButtonPressed = false;

                        LatLng location = new LatLng(latitude, longitude);
                        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(location, 15f);
                        mGoogleMap.moveCamera(cameraUpdate);

                        Bitmap imageBitmap = null, resizedBitmap = null;
                        int ic_red = 12;
                        switch (event_id) {
                            case "speedcntrl":
                                imageBitmap = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.speed_cntrl_ic);
                                resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, imageBitmap.getWidth() / ic_red, imageBitmap.getHeight() / ic_red, false);
                                break;
                            case "accidnt":
                                imageBitmap = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.car_cc_ic);
                                resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, imageBitmap.getWidth() / ic_red, imageBitmap.getHeight() / ic_red, false);
                                break;
                            case "polivoit":
                                imageBitmap = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.pol_car_ic);
                                resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, imageBitmap.getWidth() / ic_red, imageBitmap.getHeight() / ic_red, false);
                                break;
                            default:
                                break;
                        }

                        MarkerOptions markerOptions = new MarkerOptions().title("Pin").position(location).icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap));

                        // Dodaj pinezkę na otrzymane współrzędne
                        historyMarker = mGoogleMap.addMarker(markerOptions);


                    }
                }
            });


    private void findViews() {
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.maps);
        lin_lay_menu = (LinearLayout) findViewById(R.id.linear_layout_menu);
        bottomSheet = findViewById(R.id.bottom_sheet);
        menuButton = findViewById(R.id.menu_button);
        btnAddMaker = (ImageButton) findViewById(R.id.buttonAddMaker);
        btn_speed_control_maker = (ImageButton) findViewById(R.id.btn_speed_control_maker);
        btn_traffic_accident_maker = (ImageButton) findViewById(R.id.btn_traffic_accident_maker);
        btn_police_voiture_maker = (ImageButton) findViewById(R.id.btn_police_voituer_maker);
        btn_displayCurrentDirection = (ImageButton) findViewById(R.id.btnDisplayDirection);
        btn_displayCurrentDirection.setOnClickListener(view -> enableDirection());

        btnAddMaker.setOnClickListener(this);
        btn_speed_control_maker.setOnClickListener(this);
        btn_traffic_accident_maker.setOnClickListener(this);
        btn_police_voiture_maker.setOnClickListener(this);
    }

    private void enableDirection() {

        if (historyMarker != null) {
            historyMarker.remove();
            historyMarker = null;
        }

        btn_displayCurrentDirection.setVisibility(View.GONE);
        isDirectionButtonPressed = true;

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
        /*if (v.getId() != R.id.buttonAddMaker) {
           block_events_buttons();
        }
   */
    }

    public void block_events_buttons() {
        new CountDownTimer(minBlckAddMaker * 10 * 1000, 1000) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                //called after minBlckAddMaker minutes
                setButtonsEnabled(true);
                btnAddMaker.setVisibility(View.VISIBLE);
                // lin_lay_menu.setVisibility(View.VISIBLE);
            }
        }.start();
        setButtonsEnabled(false);

        //tutaj ustawiam przycisk z minutami
        btnAddMaker.setVisibility(View.GONE);
        lin_lay_menu.setVisibility(View.GONE);

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
            MarkerOptions markerOptions = null;
            //add markers on map
            String eventId = "";
            switch (id) {
                case R.id.btn_speed_control_maker:
                    eventId = "speedcntrl";
                    break;
                case R.id.btn_traffic_accident_maker:
                    eventId = "accidnt";
                    break;
                case R.id.btn_police_voituer_maker:
                    eventId = "polivoit";
                    break;
                default:
                    break;
            }
            GeoLocation geoLocation = new GeoLocation(latitude, longitude);
            markerOptions = new MarkerOptions().position(myLocation);
            String idd = UUID.randomUUID().toString();
            markerOptions.title(idd);

            databaseClass.checkMarkersExist(markerOptions, mGoogleMap, geoLocation, eventId, new DatabaseClass.OnMarkersExistListener() {
                @Override
                public void onMarkerExist(String callbackName, String useruid, int refreshCount) {
                    //other event near to current localization
                    if (callbackName.equals("event1")) {
                        LayoutInflater inflater = getLayoutInflater();
                        View layout = inflater.inflate(R.layout.custom_toast, (ViewGroup) findViewById(R.id.toast_layout));
                        TextView text = (TextView) layout.findViewById(R.id.toast_text);
                        text.setText(getResources().getString(R.string.otherevent));
                        Toast toast = new Toast(getApplicationContext());
                        toast.setGravity(Gravity.TOP, 0, 300);
                        toast.setDuration(Toast.LENGTH_SHORT);
                        toast.setView(layout);
                        toast.show();
                    }
                    if (callbackName.equals("event2")) {
                        layout_marker_window.setVisibility(View.GONE);
                        FirebaseDatabase database = FirebaseDatabase.getInstance("https://traffic-events-app-15a65-default-rtdb.europe-west1.firebasedatabase.app/");
                        DatabaseReference userRef = database.getReference("users").child(useruid);

                        userRef.child("usere_name").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                String username = dataSnapshot.getValue(String.class);

                                LayoutInflater inflater = getLayoutInflater();
                                View layout = inflater.inflate(R.layout.custom_toast, (ViewGroup) findViewById(R.id.toast_layout));
                                TextView text = (TextView) layout.findViewById(R.id.toast_text);
                                text.setText(getResources().getString(R.string.updateevent) + " " + username + "\n" + "Aktualna liczba zgłoszeń to: " + refreshCount);

                                Toast toast = new Toast(getApplicationContext());
                                toast.setGravity(Gravity.TOP, 0, 300);
                                toast.setDuration(Toast.LENGTH_SHORT);
                                toast.setView(layout);
                                toast.show();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                // obsługa błędu pobierania danych
                                Log.e("TAG", "Failed to get username", databaseError.toException());
                            }
                        });
                        block_events_buttons();
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
        gmCP = googleMap;
        mGoogleMap.getUiSettings().setCompassEnabled(false);
        mGoogleMap.setPadding(0, 800, 0, 0);
        mGoogleMap.setMinZoomPreference(14.0f);
        btn_displayCurrentDirection.setVisibility(View.GONE);
        //  mNoLocationView.setVisibility(View.VISIBLE);
        enableMyLocation();
        UiSettings uiSettings = mGoogleMap.getUiSettings();
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);

        mGoogleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng latLng) {
                layout_marker_window.setVisibility(View.GONE);
            }
        });

        //set a map style
        try {
            boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_dark));
            if (!success) {
                Log.e(TAG, "Nie udało się ustawić stylu mapy.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Nie udało się wczytać stylu mapy. Błąd: ", e);
        }

        //jeżeli kamera się przesunie to nie aktualizuje pozycji
        mGoogleMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int reason) {
                // Jeśli przycisk kierunku jazdy jest wciśnięty, ustaw wartość na false po przesunięciu kamery
                if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                    btn_displayCurrentDirection.setVisibility(View.VISIBLE);
                    isDirectionButtonPressed = false;
                }
            }
        });

        //obsługa wciśnięcia markera na mapie
        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {

                String markerId = marker.getTitle();
                databaseClass.getMarkerInfo(markerId, new OnMarkerInfoCallback() {
                    @Override
                    public void onSuccess(com.example.trafficeventsapp.Marker markerInfo) {

                        TextView eventName = layout_marker_window.findViewById(R.id.event_desc);
                        TextView creationTime = layout_marker_window.findViewById(R.id.hour_desc);
                        TextView creatorName = layout_marker_window.findViewById(R.id.user_desc);
                        TextView refreshCount = layout_marker_window.findViewById(R.id.confirm_desc);


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
                        creatorName.setText("Zgłosił: " + markerInfo.getCreatorName());
                        Date creatrion_date = new Date(markerInfo.getCreationTime());
                        DateFormat cr_df = new SimpleDateFormat("HH:mm");
                        creationTime.setText("Godzina: " + cr_df.format(creatrion_date));

                        long endTime = markerInfo.getExpirationTime();

                        Handler handler = new Handler();
                        Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                long currentTime = System.currentTimeMillis();
                                long remainingTime = (endTime - currentTime) / 1000;

                                if (remainingTime <= 0) {
                                    //  expireTime.setText("Czas minął");
                                } else {
                                    long hours = remainingTime / 3600;
                                    long minutes = (remainingTime % 3600) / 60;
                                    long seconds = remainingTime % 60;
                                    //  expireTime.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));
                                    handler.postDelayed(this, 1000);
                                }
                            }
                        };

                        refreshCount.setText("Liczba potwierdzeń: " + String.valueOf(markerInfo.getRefreshCount()));
                        layout_marker_window.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "Error getting marker info: " + error);
                    }
                });
                return true;

            }
        });

        //     enableMyLocation();
    }


    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mGoogleMap.setMyLocationEnabled(true);
            mGoogleMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
                @Override
                public void onMyLocationChange(Location location) {

                    if (!firstUpdate) {
                        databaseClass.updateGeoQuery(location, mGoogleMap);
                        firstUpdate = true;
                    }

                    if (frameL.getVisibility() == View.VISIBLE)
                        frameL.setVisibility(View.GONE);

                    //animate camera
                    if (isDirectionButtonPressed)
                        if (location != null) {
                            if (lastLocation == null) {
                                lastLocation = location;
                                CameraPosition.Builder b = CameraPosition.builder().
                                        zoom(15.0F).
                                        target(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()));
                                CameraUpdate cu = CameraUpdateFactory.newCameraPosition(b.build());
                                mGoogleMap.animateCamera(cu);
                                return;
                            }

                            LatLng oldPos = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                            LatLng newPos = new LatLng(location.getLatitude(), location.getLongitude());

                            double d = SphericalUtil.computeDistanceBetween(oldPos, newPos);
                            if (d < 10) {
                                return;
                            }

                            double bearing = SphericalUtil.computeHeading(oldPos, newPos);
                            Projection p = mGoogleMap.getProjection();
                            Point bottomRightPoint = p.toScreenLocation(p.getVisibleRegion().nearRight);
                            Point center = new Point(bottomRightPoint.x / 2, bottomRightPoint.y / 2);
                            Point offset = new Point(center.x, (center.y + 300));

                            LatLng centerLoc = p.fromScreenLocation(center);
                            LatLng offsetNewLoc = p.fromScreenLocation(offset);

                            double offsetDistance = SphericalUtil.computeDistanceBetween(centerLoc, offsetNewLoc);

                            LatLng shadowTgt = SphericalUtil.computeOffset(newPos, offsetDistance, bearing);


                            CameraPosition.Builder b = CameraPosition.builder();
                            b.zoom(15.0F);
                            b.bearing((float) (bearing));
                            b.target(shadowTgt);
                            b.tilt(55);
                            CameraUpdate cu = CameraUpdateFactory.newCameraPosition(b.build());
                            mGoogleMap.animateCamera(cu);

                            lastLocation = location;

                            /*CameraPosition cameraPosition = new CameraPosition.Builder()
                                    .target(new LatLng(location.getLatitude(), location.getLongitude()))
                                    .bearing(location.getBearing())
                                    .tilt(55)
                                    .zoom(16)
                                    .build();
                            mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));*/


                            ///////////////////////////////////////
                            long currentTime = System.currentTimeMillis();
                            if (currentTime - lastUpdateTime > 20000) { // time litmit to check markers
                                databaseClass.updateGeoQuery(location, mGoogleMap);
                                lastUpdateTime = currentTime;
                            }
                        }
                }
            });
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
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        //   mSensorManager.unregisterListener(mSensorEventListener);
    }


    @Override
    public void onLocationChanged(Location location) {
    }
}