package com.example.trafficeventsapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private String eventFullName;

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);

        // Tutaj możesz zapisać token do bazy danych
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance("https://traffic-events-app-15a65-default-rtdb.europe-west1.firebasedatabase.app/").getReference("users").child(userId);
            userRef.child("registrationToken").setValue(token);
        }

        Log.d("TAG", "New Token: " + token);
    }


    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);



        String eventId = remoteMessage.getData().get("eventId");

        switch (eventId) {
            case "speedcntrl":
                eventFullName = "Kontrola prędkości";
                break;
            case "accidnt":
                eventFullName = "Wypadek drogowy";
                break;
            case "polivoit":
                eventFullName = "Radiowóz policyjny";
                break;
            default:
                break;
        }


        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Context context = getApplicationContext();
                LayoutInflater inflater = LayoutInflater.from(context);
                View view = inflater.inflate(R.layout.custom_toast_notification, null);
                TextView textView = view.findViewById(R.id.tv_notification);
                textView.setText("Twoje zdarzenie "+eventFullName+" zostało potwierdzone");

                Toast toast = new Toast(context);
                toast.setGravity(Gravity.TOP, 0, 20);
                toast.setDuration(Toast.LENGTH_LONG);
                toast.setView(view);
                toast.show();
            }
        });

    }

    // Inne metody obsługi powiadomień
}
