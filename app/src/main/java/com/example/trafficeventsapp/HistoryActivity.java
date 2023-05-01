package com.example.trafficeventsapp;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class HistoryActivity extends AppCompatActivity implements OnItemClickListener {

    RecyclerView recyclerView;
    LinearLayoutManager layoutManager;
    List<EventModel> events_list;
    EventAdapter eventAdapter;
    Button btn_back;
    FirebaseDatabase database = FirebaseDatabase.getInstance("https://traffic-events-app-15a65-default-rtdb.europe-west1.firebasedatabase.app/");
    DatabaseReference markersRef = database.getReference("markers");
    DatabaseReference usersRef = database.getReference("users");
    DatabaseReference ref = database.getReference("geofire");
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    String user_uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        user_uid = mAuth.getCurrentUser().getUid();
        btn_back = (Button) findViewById(R.id.btn_back_to_map);
        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        initData();
        initRecyclerView();
    }

    private void initData() {
        events_list = new ArrayList<>();
        DatabaseReference historyRef = database.getReference("users").child(user_uid).child("history");

        historyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {


                for (DataSnapshot dsnap : snapshot.getChildren()) {
                    String folderName = dsnap.getKey();
                    DatabaseReference dbFolderRef = historyRef.child(folderName);
                    dbFolderRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {

                            String eventID = snapshot.child("eventID").getValue(String.class);

                            long milliseconds = snapshot.child("creationTime").getValue(long.class);
                            Date date = new Date(milliseconds);
                            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
                            String dateString = sdf.format(date);
                            int conf_count = snapshot.child("refreshCount").getValue(int.class);
                            double latitude = snapshot.child("latitude").getValue(double.class);
                            double longlatitude = snapshot.child("longitude").getValue(double.class);

                            switch (eventID) {
                                case "speedcntrl":
                                    events_list.add(new EventModel(R.drawable.ic_map_speed, "Kontrola drogowa", dateString, "Potwierdzenia: "+ String.valueOf(conf_count), latitude, longlatitude,eventID));
                                    break;
                                case "accidnt":
                                    events_list.add(new EventModel(R.drawable.ic_car_accident, "Zdarzenie drogowe", dateString, "Potwierdzenia: "+ String.valueOf(conf_count), latitude, longlatitude,eventID));
                                    break;
                                case "polivoit":
                                    events_list.add(new EventModel(R.drawable.ic_voiture_the_police, "Radiowóz policyjny", dateString, "Potwierdzenia: "+ String.valueOf(conf_count), latitude, longlatitude,eventID));
                                    break;
                                default:
                                    break;
                            }
                            eventAdapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                        }
                    });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

    }

    private void initRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView);
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        eventAdapter = new EventAdapter(events_list, this);
        recyclerView.setAdapter(eventAdapter);
    }

    @Override
    public void onItemClick(int position) {
        double latitude = events_list.get(position).getLatitude();
        double longitude = events_list.get(position).getLonglatitude();
        String ev_type = events_list.get(position).getHist_key();

        Intent resultIntent = new Intent();
        resultIntent.putExtra("latitude", latitude);
        resultIntent.putExtra("longitude", longitude);
        resultIntent.putExtra("ev_type", ev_type);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    @Override
    public void onDeleteClick(int position) {
        DatabaseReference historyRef = database.getReference("users").child(user_uid).child("history");
        String folderName = events_list.get(position).getHist_key();
        DatabaseReference dbFolderRef = historyRef.child(folderName);
        dbFolderRef.removeValue();
        events_list.remove(position);
        eventAdapter.notifyItemRemoved(position);
    }
}