package com.example.trafficeventsapp;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

public class LocationClass {
    private final Context context;
    private FusedLocationProviderClient fusedLocationClient;

    public LocationClass(Context context) {
        this.context = context;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    public interface OnLocationReceivedListener {
        void onLocationReceived(Location location);

        void onLocationError(Exception e);
    }

    public void getCurrentUserLocation(OnLocationReceivedListener listener) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            listener.onLocationError(new SecurityException("Location permission not granted"));
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            listener.onLocationReceived(location);
                        } else {
                            listener.onLocationError(new Exception("Location is null"));
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        listener.onLocationError(e);
                    }
                });
    }




    /*private void updateMap(Location location) {
        // Usunięcie poprzedniego markera
        if (mMarker != null) {
            mMarker.remove();
        }

        // Dodanie markera dla aktualnej lokalizacji użytkownika
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title("Moja lokalizacja");
        mMarker = mGoogleMap.addMarker(markerOptions);

        // Pobranie pinezek z bazy danych i wyświetlenie tych, które znajdują się w odległości 5 km od aktualnej lokalizacji użytkownika
        mValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot markerSnapshot : dataSnapshot.getChildren()) {
                    String markerId = markerSnapshot.getKey();
                    double latitude = markerSnapshot.child("localization").child("latitude").getValue(Double.class);
                    double longitude = markerSnapshot.child("localization").child("longitude").getValue(Double.class);
                    LatLng markerLatLng = new LatLng(latitude, longitude);
                    float distance = location.distanceTo(markerLatLng) / 1000; // Odległość w kilometrach
                    if (distance <= 5) {
                        MarkerOptions markerOptions = new MarkerOptions()
                                .position(markerLatLng)
                                .title(markerId);
                        mGoogleMap.addMarker(markerOptions);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        };
        mDatabase.addListenerForSingleValueEvent(mValueEventListener);
    }*/

}
