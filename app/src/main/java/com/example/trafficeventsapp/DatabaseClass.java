package com.example.trafficeventsapp;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;

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

import java.util.Calendar;

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


    public interface OnMarkersExistListener {
        void onMarkerExist(boolean exist);
    }

    public DatabaseClass() {
    }

    public DatabaseClass(Context context) {
        mContext = context;
        mLocationClass = new LocationClass(mContext);
    }

    public void addMakerToDatabase(MarkerOptions markerOptions, com.google.android.gms.maps.model.Marker marker, String eventId) {

        GeoFire geoFire = new GeoFire(ref);

        long currentTimestamp = System.currentTimeMillis();
        long minInMls = 20 * 60 * 1000; //20 minutes
        if(eventId.equals("polivoit"))
        {
            minInMls = 5 * 60 * 1000; //20 minutes
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
        geoQuery = geoFire.queryAtLocation(new GeoLocation(location.getLatitude(), location.getLongitude()), radiusInM);

        // Zarejestruj nasłuchiwacza zdarzeń GeoQuery.
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {

                DatabaseReference markerRef = markersRef.child(key);
                markerRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            // Pobierz wartość pola "eventI" z DataSnapshot
                            String eventID = dataSnapshot.child("eventID").getValue(String.class);
                            Log.d(TAG, "Wartość pola eventI dla markera " + dataSnapshot.getValue() + " wynosi: " + eventID);

                            LatLng latLng = new LatLng(location.latitude, location.longitude);

                            //wstaw pinezke
                            Bitmap imageBitmap = null, resizedBitmap = null;
                            int ic_red = 5;
                            //add makers on map
                            switch (eventID) {
                                case "speedcntrl":
                                    int a = 5;
                                    imageBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_map_speed);
                                    resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, imageBitmap.getWidth() / ic_red, imageBitmap.getHeight() / ic_red, false);
                                    break;
                                case "accidnt":
                                    imageBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_map_crash);
                                    resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, imageBitmap.getWidth() / ic_red, imageBitmap.getHeight() / ic_red, false);
                                    break;
                                case "polivoit":
                                    imageBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_map_vouiter_police);
                                    resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, imageBitmap.getWidth() / ic_red, imageBitmap.getHeight() / ic_red, false);
                                    break;
                                default:
                                    break;
                            }


                            //add refresh count icon
                            int refreshCount = dataSnapshot.child("refreshCount").getValue(int.class);
                            MarkerOptions markerOptions = new MarkerOptions().title(dataSnapshot.getKey()).position(latLng).icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap)).
                                    snippet(String.valueOf(refreshCount));
                            com.google.android.gms.maps.model.Marker mMarker = mGoogleMap.addMarker(markerOptions);


                        } else {
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
                // Usuń punkt o kluczu "key" z mapy.
                // ...
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                // Zaktualizuj położenie punktu o kluczu "key" na mapie.
                // ...
            }

            @Override
            public void onGeoQueryReady() {
                // Ta metoda jest wywoływana, gdy GeoQuery został w pełni wczytany i subskrybowany.
                // ...
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                // Ta metoda jest wywoływana, gdy wystąpi błąd podczas subskrybowania GeoQuery.
                // ...
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
            boolean isDifferenTypeOfMarker = false;
            boolean isPlusAdded = false;

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


                               if(eventId.equals("speedcntrl") || eventId.equals("accidnt") )
                               {
                                   cal.add(Calendar.MINUTE, 20);
                               }else
                               {
                                   cal.add(Calendar.MINUTE, 5);
                               }

                                long expirationTime = cal.getTimeInMillis();
                                ref.child("expirationTime").setValue(expirationTime);

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
                    com.google.android.gms.maps.model.Marker marker = mGoogleMap.addMarker(markerOptions);
                    addMakerToDatabase(markerOptions, marker, eventId);
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                Log.e(TAG, "Failed to check markers exist: " + error.getMessage());
            }
        });
    }




  /*  public void checkIfMarkersExist(GeoLocation geoLocation, String eventId, MarkerOptions markerOptions, GoogleMap mGoogleMap, final OnCheckMarkersExistCallback callback) {

        DatabaseReference geofireRef = database.getReference("geofire");
        GeoFire geoFire = new GeoFire(geofireRef);
        GeoQuery geoQuery = geoFire.queryAtLocation(geoLocation, 1.0);

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                DatabaseReference markerRef = database.getReference("markers").child(key);
                markerRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            String exEventId = dataSnapshot.child("eventID").getValue(String.class);
                            if (eventId.equals(exEventId)) {
                                Log.d("TAG", "Takie same");
                                dataSnapshot.getRef().child("refreshCount").setValue(ServerValue.increment(1));
                                callback.onMarkerExists();
                                return;
                            }
                        }
                        callback.onMarkerNotExist();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        callback.onCheckMarkersExistError(databaseError);
                    }
                });
            }

            @Override
            public void onKeyExited(String key) {
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
            }

            @Override
            public void onGeoQueryReady() {
                // Jeżeli nie znaleziono markerów, wywołaj callback z odpowiednią flagą
                callback.onMarkerNotExist();
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                callback.onCheckMarkersExistError(error);
            }
        });



    }

    public interface OnCheckMarkersExistCallback {
        void onMarkerExists();
        void onMarkerNotExist();
        void onCheckMarkersExistError(DatabaseError error);
    }*/

  /*  public void checkIfMarkersExist(GeoLocation geoLocation, String eventId, MarkerOptions markerOptions, GoogleMap mGoogleMap) {

        DatabaseReference geofireRef = database.getReference("geofire");
        GeoFire geoFire = new GeoFire(geofireRef);
        GeoQuery geoQuery = geoFire.queryAtLocation(geoLocation, 1.0);
        markerExist = false;

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                DatabaseReference markerRef = database.getReference("markers").child(key);
                markerRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            String exEventId = dataSnapshot.child("eventID").getValue(String.class);
                            if (eventId.equals(exEventId)) {
                                dataSnapshot.getRef().child("refreshCount").setValue(ServerValue.increment(1));
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e("TAG", "GeoQuery error: " );
                    }
                });
            }

            @Override
            public void onKeyExited(String key) {
                // usunięcie pinezki z listy znalezionych w okolicy
                Log.e("TAG", "GeoQuery error: " );
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                // przesunięcie pinezki
                Log.e("TAG", "GeoQuery error: " );
            }

            @Override
            public void onGeoQueryReady() {
                Log.e("TAG", "GeoQuery error: " );
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                Log.e("TAG", "GeoQuery error: " + error.getMessage());

            }
        });

    }
*/


    public boolean isMarkerExist() {
        return markerExist;
    }

    public void setMarkerExist(boolean markerExist) {
        this.markerExist = markerExist;
    }

}
