package com.example.trafficeventsapp;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference myRef = database.getReference();
    private ProgressBar progressbar;
    private Button buttonBack, buttonRegister;
    private EditText editTextEmail, editTextPassword, editTextUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);


        editTextUserName = (EditText) findViewById(R.id.editTextUserName);
        editTextEmail = (EditText) findViewById(R.id.editTextEmail);
        editTextPassword = (EditText) findViewById(R.id.editTextPassword);
        progressbar = (ProgressBar) findViewById(R.id.progressBar2);
        buttonBack = (Button) findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> backToLogin());
        buttonRegister = (Button) findViewById(R.id.buttonRegisterUser);
        buttonRegister.setOnClickListener(v -> registerNewUser());
    }

    private void registerNewUser() {
        String email = editTextEmail.getText().toString();
        String password = editTextPassword.getText().toString();
        String userName = editTextUserName.getText().toString();
        // checkIsFieldsIsFill(email,password,userName);
        progressbar.setVisibility(View.VISIBLE);
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    mAuth.getCurrentUser().sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            finish();
                            Log.e("TAG", "E-mail verification was send.");

                            FirebaseDatabase database = FirebaseDatabase.getInstance("https://traffic-events-app-15a65-default-rtdb.europe-west1.firebasedatabase.app/");
                            DatabaseReference markersRef = database.getReference("users");

                            Map<String, Object> values = new HashMap<>();
                            values.put("email", mAuth.getCurrentUser().getEmail());
                            values.put("usere_name", userName);
                            markersRef.child(mAuth.getCurrentUser().getUid()).setValue(values);


                            Toast.makeText(getApplicationContext(), getResources().getString(R.string.verifyEmail), Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    progressbar.setVisibility(View.GONE);
                    try {
                        throw task.getException();
                    } catch (FirebaseAuthWeakPasswordException e) {
                        Log.w("TAG", "Weak password.");
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.weakPassword), Toast.LENGTH_LONG).show();
                    } catch (FirebaseAuthInvalidCredentialsException e) {
                        Log.e("TAG", "Invalid e-mail or in use.");
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.invalidEmail), Toast.LENGTH_LONG).show();
                    } catch (FirebaseAuthUserCollisionException e) {
                        Log.e("TAG", "User is already registered.");
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.invalidEmail), Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    private boolean checkIsFieldsIsFill(String email, String password, String userName) {
        return true;
    }

    private void backToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
    }


}