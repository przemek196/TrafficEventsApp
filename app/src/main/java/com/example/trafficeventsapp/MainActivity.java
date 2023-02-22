package com.example.trafficeventsapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    private ImageView imVlogo;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imVlogo = (ImageView) findViewById(R.id.logo_icon_startup);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        user = FirebaseAuth.getInstance().getCurrentUser();
        Animation anim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in_animation);
        imVlogo.setAnimation(anim);
        Intent homeIntent = new Intent(MainActivity.this, LoginActivity.class);
        Intent mapIntent = new Intent(MainActivity.this, MapActivity.class);
        Intent intent;

        if (user != null) {
            intent = mapIntent;
        } else {
            intent = homeIntent;
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(intent);
                finish();
            }
        }, 2700);
    }
}