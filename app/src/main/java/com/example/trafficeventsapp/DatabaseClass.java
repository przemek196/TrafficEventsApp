package com.example.trafficeventsapp;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseClass {

    FirebaseDatabase database = FirebaseDatabase.getInstance("https://traffic-events-app-15a65-default-rtdb.europe-west1.firebasedatabase.app/");
    DatabaseReference markersRef = database.getReference("markers");
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private Context mContext;
    private LocationClass mLocationClass;


    public DatabaseClass(Context context) {
        mContext = context;
        mLocationClass = new LocationClass(mContext);
    }

    public void addMakerToDatabase(MarkerOptions markerOptions) {
        Map<String, Object> values = new HashMap<>();
        values.put("position", new GeoLocation(markerOptions.getPosition().latitude, markerOptions.getPosition().longitude));
        values.put("event_id", markerOptions.getTitle());
        values.put("who_added", mAuth.getCurrentUser().getUid());
        values.put("time_added", ServerValue.TIMESTAMP);
        markersRef.push().setValue(values);
    }

    public void getActiveMarkersFromDataase() {
        LocationClass locationClass = this.mLocationClass;
        locationClass.getCurrentUserLocation(new LocationClass.OnLocationReceivedListener() {
            @Override
            public void onLocationReceived(Location location) {
                // Get user's current location
                LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                GeoLocation geoLocation = new GeoLocation(currentLocation.latitude, currentLocation.longitude);
                GeoFire geoFire = new GeoFire(markersRef);

                GeoLocation myLocation = new GeoLocation(currentLocation.latitude, currentLocation.longitude);
                double radius = 5.0; // km

                GeoQuery geoQuery = geoFire.queryAtLocation(myLocation, radius);
                geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                    @Override
                    public void onKeyEntered(String key, GeoLocation location) {
                        // For each marker within the query radius, add it to the map
                        DatabaseReference positionRef = markersRef.child(key).child("location");
                        positionRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                // Check the distance between user's location and the marker's location
                                double distance = calculateDistance(myLocation, dataSnapshot.getValue(GeoLocation.class));
                                if (distance <= radius) {
                                    // Add the marker to the map
                                    // ...
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                // Obsługa błędów
                            }
                        });
                    }

                    @Override
                    public void onKeyExited(String key) {
int a=5;
                    }

                    @Override
                    public void onKeyMoved(String key, GeoLocation location) {

                    }

                    @Override
                    public void onGeoQueryReady() {

                    }

                    @Override
                    public void onGeoQueryError(DatabaseError error) {
int a=5;
                    }
                });




//ten kod dziala
               /* ChildEventListener childEventListener = new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                        // odczytaj dane z podkatalogu position
                        String id = dataSnapshot.getKey();
                        String lat = dataSnapshot.child("position").child("latitude").getValue().toString();
                        String lon = dataSnapshot.child("position").child("longitude").getValue().toString();

                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                        // obsłuż zmiany danych
                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {
                        // obsłuż usunięcie danych
                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                        // obsłuż zmianę kolejności danych
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e(TAG, "Failed to read value.", databaseError.toException());
                    }
                };


                markersRef.addChildEventListener(childEventListener);
*/


            }

            @Override
            public void onLocationError(Exception e) {
            }
        });
    }

    public static double calculateDistance(GeoLocation location1, GeoLocation location2) {
        // Średnia promień Ziemi w kilometrach
        double earthRadius = 6371;

        // Współrzędne punktów
        double lat1 = location1.latitude;
        double lng1 = location1.longitude;
        double lat2 = location2.latitude;
        double lng2 = location2.longitude;

        // Różnice między współrzędnymi
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        // Obliczenie odległości między punktami przy użyciu wzoru Haversine
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = earthRadius * c;

        return distance;
    }


}
