package com.example.trafficeventsapp;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DatabaseClass {

    FirebaseDatabase database = FirebaseDatabase.getInstance("https://traffic-events-app-15a65-default-rtdb.europe-west1.firebasedatabase.app/");
    DatabaseReference markersRef = database.getReference("markers");
    DatabaseReference usersRef = database.getReference("users");
    DatabaseReference ref = database.getReference("geofire");
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private GeoQuery geoQuery;
    private double radiusInKm = 5;
    private double radiusInM = radiusInKm * 1000;
    private Context mContext;
    private LocationClass mLocationClass;
    private boolean markerExist = false;
    private final int timeMarkerSpeedCntrl = 1;
    private ArrayList<com.google.android.gms.maps.model.Marker> markersList;


    public interface OnMarkersExistListener {
        void onMarkerExist(String callbackName, String useruid, int refreshCount);
    }

    public DatabaseClass() {
        // mDatabase = FirebaseDatabase.getInstance("https://traffic-events-app-15a65-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
    }


    public ArrayList<com.google.android.gms.maps.model.Marker> getMarkersList() {
        return markersList;
    }

    public void setMarkersList(ArrayList<com.google.android.gms.maps.model.Marker> markersList) {
        this.markersList = markersList;
    }

    public DatabaseClass(Context context) {
        mContext = context;
        mLocationClass = new LocationClass(mContext);
        markersList = new ArrayList<com.google.android.gms.maps.model.Marker>();
    }

    public void addMakerToDatabase(MarkerOptions markerOptions, String eventId) {

        GeoFire geoFire = new GeoFire(ref);
        long currentTimestamp = System.currentTimeMillis();
        long minInMls = timeMarkerSpeedCntrl * 60 * 1000; //marker spped control and accident time
        if (eventId.equals("polivoit")) {
            minInMls = 5 * 60 * 1000; //police car marker
        }
        Marker pin = new Marker(eventId, mAuth.getCurrentUser().getUid(), currentTimestamp, currentTimestamp + minInMls, 1);

// Zapisywanie obiektu w Geofire pod określonym kluczem (markerId) i z określoną lokalizacją (lat, lng)
        geoFire.setLocation(markerOptions.getTitle(), new GeoLocation(markerOptions.getPosition().latitude, markerOptions.getPosition().longitude), new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                if (error != null) {
                    Log.d("TAG", "Nie udało się zapisać lokalizacji w Geofire: " + error.getMessage());
                } else {
                    Log.d("TAG", "Lokalizacja została zapisana w Geofire pod kluczem: " + key);
                }
            }
        });

// Zapisywanie dodatkowych informacji o pinezce w Firebase Realtime Database
        DatabaseReference pinRef = database.getReference("markers").child(markerOptions.getTitle());
        pinRef.setValue(pin);
    }


    public void updateGeoQuery(Location location, GoogleMap mGoogleMap) {
        // Utwórz nową GeoQuery z nowymi parametrami lokalizacji i promienia.
        GeoFire geoFire = new GeoFire(ref);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(location.getLatitude(), location.getLongitude()), radiusInKm);


        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {

                List<DataSnapshot> dataSnapshotList = new ArrayList<>();
                DatabaseReference markerRef = markersRef.child(key);
                markerRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            // Pobierz wartość pola "eventId" z DataSnapshot
                            String eventID = dataSnapshot.child("eventID").getValue(String.class);
                            Log.d(TAG, "Wartość pola eventI dla markera " + dataSnapshot.getValue() + " wynosi: " + eventID);

                            LatLng latLng = new LatLng(location.latitude, location.longitude);

                            //wstaw pinezke
                            Bitmap imageBitmap = null, resizedBitmap = null;
                            int ic_red = 12;
                            //add makers on map
                            switch (eventID) {
                                case "speedcntrl":
                                    imageBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.speed_cntrl_ic);
                                    resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, imageBitmap.getWidth() / ic_red, imageBitmap.getHeight() / ic_red, false);
                                    break;
                                case "accidnt":
                                    imageBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.car_cc_ic);
                                    resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, imageBitmap.getWidth() / ic_red, imageBitmap.getHeight() / ic_red, false);
                                    break;
                                case "polivoit":
                                    imageBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.pol_car_ic);
                                    resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, imageBitmap.getWidth() / ic_red, imageBitmap.getHeight() / ic_red, false);
                                    break;
                                default:
                                    break;
                            }

                            //add refresh count icon
                            int refreshCount = dataSnapshot.child("refreshCount").getValue(int.class);

                            //tutaj usuwam pinezke której czas minął z mapy
                            long endTime = dataSnapshot.child("expirationTime").getValue(long.class);
                            long currentTime = System.currentTimeMillis();

                            long remainingTime = (endTime - currentTime) / 1000;
                            boolean markExistOnMap = false;


                            if (markersList.isEmpty()) {
                                MarkerOptions markerOptions = new MarkerOptions().title(dataSnapshot.getKey()).position(latLng).icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap)).
                                        snippet(String.valueOf(refreshCount));
                                com.google.android.gms.maps.model.Marker mMarker = mGoogleMap.addMarker(markerOptions);
                                markersList.add(mMarker);
                                int a = 5;
                            } else {

                                //sprawdzam czy mam taki marker na mapie
                                for (com.google.android.gms.maps.model.Marker m : markersList) {
                                    if (m.getTitle().equals(dataSnapshot.getKey())) { //jeżeli istnieje to znaczy, że taki marker już jest na mapie
                                        markExistOnMap = true;
                                        break;
                                    }
                                }
                                if (!markExistOnMap) {
                                    if (remainingTime >= 0L) {
                                        MarkerOptions markerOptions = new MarkerOptions().title(dataSnapshot.getKey()).position(latLng).icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap)).
                                                snippet(String.valueOf(refreshCount));
                                        com.google.android.gms.maps.model.Marker mMarker = mGoogleMap.addMarker(markerOptions);
                                        markersList.add(mMarker);
                                    }
                                }
                            }

                            boolean isExist = false;
                            int index = 0;
                            //usun stare markery
                            for (com.google.android.gms.maps.model.Marker m : markersList) {
                                if (m.getTitle().equals(dataSnapshot.getKey())) { //jeżeli istnieje to znaczy, że taki marker już jest na mapie
                                    isExist = true;
                                    break;
                                }
                            }

                            if (!isExist) {
                                com.google.android.gms.maps.model.Marker markerToRemove = markersList.get(index);
                                markerToRemove.remove();
                                markersList.remove(markerToRemove);
                            }
                        } else {
                            //if any marker didnt-exist
                            Log.d(TAG, "Nie znaleziono markera o ID " + key);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.d(TAG, "Błąd podczas pobierania danych z Firebase: " + databaseError.getMessage());
                    }
                });
            }

            @Override
            public void onKeyExited(String key) {
                for (com.google.android.gms.maps.model.Marker m : markersList) {
                    m.remove();
                    markersList.remove(m);
                }
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
            }

            @Override
            public void onGeoQueryReady() {
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
            }
        });
    }


    public void getMarkerInfo(String markerId, MapActivity.OnMarkerInfoCallback callback) {

        DatabaseReference markerRef = markersRef.child(markerId);
        markerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Marker marker = dataSnapshot.getValue(Marker.class);
                    String userId = marker.getCreator();
                    DatabaseReference userRef = usersRef.child(userId);

                    userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            marker.setCreatorName(snapshot.child("usere_name").getValue(String.class));
                            callback.onSuccess(marker);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                } else {
                    callback.onFailure("Marker not found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                callback.onFailure(databaseError.getMessage());
            }
        });
    }

    public void checkMarkersExist(MarkerOptions markerOptions, GoogleMap mGoogleMap, GeoLocation geoLocation, String eventId, OnMarkersExistListener listener) {

        //DatabaseReference geofire = database.getReference("geofire");
        GeoFire geoFire = new GeoFire(ref);
        GeoQuery geoQuery = geoFire.queryAtLocation(geoLocation, 1.0); // promień 1km

        // zmienna przechowująca informację o tym, czy już wystąpiła pinezka w pobliżu
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            boolean anyMarkerExist = false;
            boolean exist = false;


            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                DatabaseReference markerRef = markersRef.child(key);
                anyMarkerExist = true;

                markerRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            markerExist = true; // zmiana wartości zmiennej na true w przypadku wystąpienia pinezki

                            String evID = dataSnapshot.child("eventID").getValue(String.class);
                            if (evID.equals(eventId) && !markerOptions.getTitle().equals(key)) {
                                int refCount = dataSnapshot.child("refreshCount").getValue(int.class);
                                refCount++;
                                DatabaseReference ref = dataSnapshot.getRef();
                                ref.child("refreshCount").setValue(refCount);
                                Calendar cal = Calendar.getInstance();
                                if (eventId.equals("speedcntrl") || eventId.equals("accidnt")) {
                                    cal.add(Calendar.MINUTE, timeMarkerSpeedCntrl);
                                } else {
                                    cal.add(Calendar.MINUTE, 5);
                                }
                                long expirationTime = cal.getTimeInMillis();
                                ref.child("expirationTime").setValue(expirationTime);
                                listener.onMarkerExist("event2", dataSnapshot.child("creator").getValue(String.class),refCount);
                            } else {
                                //tutaj trzeba nie blokować przycisku i zwrócić informacje o tym, że inne zdarzenie jest w pobliżu
                                listener.onMarkerExist("event1", "",0);
                            }

                        } else {
                            Log.e(TAG, "Koniec rekordów");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Failed to check markers exist: " + databaseError.getMessage());
                    }
                });
            }

            @Override
            public void onKeyExited(String key) {
                // do nothing
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                // do nothing
            }

            @Override
            public void onGeoQueryReady() {
                if (!anyMarkerExist) {
                    addMakerToDatabase(markerOptions, eventId);
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                Log.e(TAG, "Failed to check markers exist: " + error.getMessage());
            }
        });
    }

}
