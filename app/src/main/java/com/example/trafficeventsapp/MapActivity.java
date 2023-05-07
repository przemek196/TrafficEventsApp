package com.example.trafficeventsapp;


import static android.content.ContentValues.TAG;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.Sensor;
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
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;


public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback, LocationListener {


    private SupportMapFragment mapFragment;
    private MapView mapView;
    private GoogleMap mGoogleMap;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private ImageButton btn_speed_control_maker;
    private ImageButton btn_displayCurrentDirection;
    private Button confirm_event_button;
    private boolean menu_visibility = false;
    private int minBlckAddMaker = 1;
    private double distance = 5000;
    private ImageButton menuButton;
    private GoogleMap gmCP;
    private boolean isDirectionButtonPressed = true;
    private Marker mCurrentLocationMarker;
    private Location mLastKnownLocation;
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
    private View layout_marker_window;
    private FrameLayout frameL;
    private Marker historyMarker;
    ExtendedFloatingActionButton fabAdd;
    FloatingActionButton fabSpeed, fabCrash, fabCar;
    TextView tvSp, tvCrash, tvCar;
    Animation fabOpen, fabClose;
    boolean isOpen = false;
    FirebaseDatabase database = FirebaseDatabase.getInstance("https://traffic-events-app-15a65-default-rtdb.europe-west1.firebasedatabase.app/");
    DatabaseReference markersRef = database.getReference("markers");
    DatabaseReference usersRef = database.getReference("users");
    DatabaseReference ref = database.getReference("geofire");
    private boolean isEventConf = false;
    private boolean isMarkersNearFinded;
    private MarkerOptions cpMarkerOptions;
    private String cpEventId;
private boolean isNorificationShow,isMarkersAded;
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

        initFloatActionBtn();

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
                    popup.getMenu().getItem(1).setTitle(getResources().getString(R.string.hide_traffic));
                } else {
                    popup.getMenu().getItem(1).setTitle(getResources().getString(R.string.show_traffic));
                }

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.incident_history:
                                Intent historyIntent = new Intent(getApplicationContext(), HistoryActivity.class);
                                if (historyMarker != null) {
                                    historyMarker.remove();
                                    historyMarker = null;
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

        mapView.getMapAsync(this);
    }


    private void initFloatActionBtn() {

        fabAdd = (ExtendedFloatingActionButton) findViewById(R.id.addFab);
        fabSpeed = (FloatingActionButton) findViewById(R.id.speedFab);
        fabCrash = (FloatingActionButton) findViewById(R.id.accidentFab);
        fabCar = (FloatingActionButton) findViewById(R.id.polCarFab);
        tvSp = (TextView) findViewById(R.id.speedFabText);
        tvCrash = (TextView) findViewById(R.id.accidentFabText);
        tvCar = (TextView) findViewById(R.id.polCarFabText);

        fabOpen = AnimationUtils.loadAnimation(this, R.anim.fab_open);
        fabClose = AnimationUtils.loadAnimation(this, R.anim.fab_close);

        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateFab();
            }
        });

        fabSpeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addMakerOnMap(v.getId());
            }
        });

        fabCrash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addMakerOnMap(v.getId());
            }
        });

        fabCar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addMakerOnMap(v.getId());
            }
        });


    }

    private void animateFab() {
        if (isOpen) {
            fabSpeed.startAnimation(fabClose);
            fabCrash.startAnimation(fabClose);
            fabCar.startAnimation(fabClose);
            tvSp.startAnimation(fabClose);
            tvCrash.startAnimation(fabClose);
            tvCar.startAnimation(fabClose);

            fabSpeed.setClickable(false);
            fabCrash.setClickable(false);
            fabCar.setClickable(false);
            isOpen = false;
        } else {
            fabSpeed.startAnimation(fabOpen);
            fabCrash.startAnimation(fabOpen);
            fabCar.startAnimation(fabOpen);
            tvSp.startAnimation(fabOpen);
            tvCrash.startAnimation(fabOpen);
            tvCar.startAnimation(fabOpen);
            fabSpeed.setClickable(true);
            fabCrash.setClickable(true);
            fabCar.setClickable(true);
            isOpen = true;
        }

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
      //  mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.maps);
       mapView = (MapView) findViewById(R.id.maps);
        confirm_event_button = (Button) findViewById(R.id.button_confirm_event);
        menuButton = findViewById(R.id.menu_button);
        btn_displayCurrentDirection = (ImageButton) findViewById(R.id.btnDisplayDirection);
        btn_displayCurrentDirection.setOnClickListener(view -> enableDirection());
    }

    private void enableDirection() {

        if (historyMarker != null) {
            historyMarker.remove();
            historyMarker = null;
        }

        btn_displayCurrentDirection.setVisibility(View.GONE);
        isDirectionButtonPressed = true;

    }

    public void block_events_buttons() {
        fabAdd.setEnabled(false); // zablokowanie przycisku
        fabAdd.setTextColor(Color.BLACK);
        isOpen = true;
        animateFab();
        new CountDownTimer(minBlckAddMaker * 60 * 1000, 1000) {
            public void onTick(long millisUntilFinished) {
                // ustawienie czasu do końca blokady na przycisku
                fabAdd.setText("Blokada przez:" + millisUntilFinished / 1000 + "s");
            }

            public void onFinish() {
                fabAdd.setEnabled(true); // odblokowanie przycisku
                fabAdd.setText("Dodaj zdarzenie"); // ustawienie tekstu przycisku na domyślną wartość
            }
        }.start();
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

            //add markers on map
            String eventId = "";
            switch (id) {
                case R.id.speedFab:
                    eventId = "speedcntrl";
                    break;
                case R.id.accidentFab:
                    eventId = "accidnt";
                    break;
                case R.id.polCarFab:
                    eventId = "polivoit";
                    break;
                default:
                    break;
            }

            MarkerOptions markerOptions = null;
            GeoLocation geoLocation = new GeoLocation(latitude, longitude);
            markerOptions = new MarkerOptions().position(myLocation);
            String idd = UUID.randomUUID().toString();
            markerOptions.title(idd);

            cpMarkerOptions = markerOptions;
            cpEventId = eventId;
            isMarkersNearFinded = false;
            isNorificationShow = false;

            GeoFire geoFire = new GeoFire(ref);
            GeoQuery geoQuery = geoFire.queryAtLocation(geoLocation, 1.0);

            geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                @Override
                public void onKeyEntered(String key, GeoLocation location) {
                    isMarkersNearFinded = true;

               if(!isNorificationShow && !isMarkersAded)
               {
                   LayoutInflater inflater = getLayoutInflater();
                   View layout = inflater.inflate(R.layout.custom_toast, (ViewGroup) findViewById(R.id.toast_layout));
                   TextView text = (TextView) layout.findViewById(R.id.toast_text);
                   text.setText(getResources().getString(R.string.otherevent));
                   Toast toast = new Toast(getApplicationContext());
                   toast.setGravity(Gravity.TOP, 0, 300);
                   toast.setDuration(Toast.LENGTH_SHORT);
                   toast.setView(layout);
                   toast.show();

                   isNorificationShow=true;
               }
                }

                @Override
                public void onKeyExited(String key) {

                }

                @Override
                public void onKeyMoved(String key, GeoLocation location) {

                }

                @Override
                public void onGeoQueryReady() {
                    if (!isMarkersNearFinded) {
                        databaseClass.addMakerToDatabase(cpMarkerOptions, cpEventId);

                        LayoutInflater inflater = getLayoutInflater();
                        View layout = inflater.inflate(R.layout.custom_toast, (ViewGroup) findViewById(R.id.toast_layout));
                        TextView text = (TextView) layout.findViewById(R.id.toast_text);
                        text.setText(getResources().getString(R.string.new_event_adaed));
                        Toast toast = new Toast(getApplicationContext());
                        toast.setGravity(Gravity.TOP, 0, 300);
                        toast.setDuration(Toast.LENGTH_SHORT);
                        toast.setView(layout);
                        toast.show();
                        block_events_buttons();

                        isMarkersAded=true;
                    }
                }

                @Override
                public void onGeoQueryError(DatabaseError error) {

                }
            });















         /*   databaseClass.checkMarkersExist(markerOptions, mGoogleMap, geoLocation, eventId, new DatabaseClass.OnMarkersExistListener() {
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
                }
            });
*/


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
            boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.light_style_map));
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

                            long currentTime = System.currentTimeMillis();
                            if (currentTime - lastUpdateTime > 20000) { // time litmit to check markers
                                databaseClass.updateGeoQuery(location, mGoogleMap);
                                lastUpdateTime = currentTime;
                            }

                            //check if other events near to 200m
                            showLayoutIfMarkerExist(location);


                        }
                }
            });
        }
    }

    public void showLayoutIfMarkerExist(Location location) {
        GeoFire geoFire = new GeoFire(ref);
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(location.getLatitude(), location.getLongitude()), 0.2); // 0.2 to promień w km, ustawiony na 200m

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {

                DatabaseReference markerRef = markersRef.child(key);
                markerRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // Pobierz dane zdarzenia
                            String creator = snapshot.child("creator").getValue(String.class);
                            String current_user = FirebaseAuth.getInstance().getUid();
                            int refCount = snapshot.child("refreshCount").getValue(int.class);
                            if (!creator.equals(current_user) && !isEventConf) {
                                confirm_event_button.setVisibility(View.VISIBLE);
                                confirm_event_button.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        int newCount = refCount + 1;
                                        isEventConf = true;
                                        markerRef.child("refreshCount").setValue(newCount);
                                        // Schowaj przycisk potwierdzenia
                                        confirm_event_button.setVisibility(View.GONE);

                                        //show confirm notification
                                        DatabaseReference userRef = database.getReference("users").child(creator);
                                        userRef.child("usere_name").addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                String username = dataSnapshot.getValue(String.class);

                                                LayoutInflater inflater = getLayoutInflater();
                                                View layout = inflater.inflate(R.layout.custom_toast, (ViewGroup) findViewById(R.id.toast_layout));
                                                TextView text = (TextView) layout.findViewById(R.id.toast_text);
                                                text.setText(getResources().getString(R.string.updateevent) + " " + username + "\n" + "Aktualna liczba zgłoszeń to: " + newCount);

                                                Toast toast = new Toast(getApplicationContext());
                                                toast.setGravity(Gravity.TOP, 0, 300);
                                                toast.setDuration(Toast.LENGTH_LONG);
                                                toast.setView(layout);
                                                toast.show();
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                                // obsługa błędu pobierania danych
                                                Log.e("TAG", "Failed to get username", databaseError.toException());
                                            }
                                        });


                                    }
                                });
                            }
                            // Wyświetl przycisk potwierdzenia
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
                //confirm_event_button.setVisibility(View.VISIBLE);
            }

            @Override
            public void onKeyExited(String key) {
                isEventConf = false;
                confirm_event_button.setVisibility(View.GONE);
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                // Ta metoda jest wywoływana, gdy zdarzenie się przesuwa
                // Możesz dodać tutaj odpowiedni kod, jeśli to potrzebne
            }

            @Override
            public void onGeoQueryReady() {
                // Ta metoda jest wywoływana, gdy GeoQuery zakończy szukanie zdarzeń
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                // Ta metoda jest wywoływana, gdy GeoQuery napotka błąd
            }
        });

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
    }


    @Override
    public void onLocationChanged(Location location) {
    }
}