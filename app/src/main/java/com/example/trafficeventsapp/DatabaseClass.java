package com.example.trafficeventsapp;

import android.content.Context;
import android.util.Log;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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

    public void addMakerToDatabase(MarkerOptions markerOptions, com.google.android.gms.maps.model.Marker marker, String eventId) {

        DatabaseReference ref = database.getReference("geofire");
        GeoFire geoFire = new GeoFire(ref);

        long currentTimestamp = System.currentTimeMillis();
        long twentyMinutesInMilliseconds = 20 * 60 * 1000; //20 minutes
        Marker pin = new Marker(eventId, mAuth.getCurrentUser().getUid(), currentTimestamp, currentTimestamp + twentyMinutesInMilliseconds, 1);

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



 /*       Map<String, Object> values = new HashMap<>();
        values.put("latitude", markerOptions.getPosition().latitude);
        values.put("longitude", markerOptions.getPosition().longitude);
        values.put("event_id", markerOptions.getTitle());
        values.put("creator", mAuth.getCurrentUser().getUid());
        values.put("creationTime", ServerValue.TIMESTAMP);
        long currentTimestamp = System.currentTimeMillis();
        long twentyMinutesInMilliseconds = 20 * 60 * 1000; //20 minutes
        values.put("expirationTime", currentTimestamp + twentyMinutesInMilliseconds);
        values.put("refreshCount", 1);
        markersRef.push().setValue(values);*/


    }
}
