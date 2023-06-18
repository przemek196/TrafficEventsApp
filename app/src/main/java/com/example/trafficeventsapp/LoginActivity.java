package com.example.trafficeventsapp;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;





public class LoginActivity extends AppCompatActivity {

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference myRef = FirebaseDatabase.getInstance("https://traffic-events-app-15a65-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog dialog;
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private FirebaseUser firebaseUser;
    private TextView textViewRemindPassword, textViewLoginError;
    private EditText editTextEmail, editTextPassword;
    private String emailVerification = "";
    private ProgressBar progressbar;
    private Button buttonRegister;
    private Button buttonLogIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        progressbar = (ProgressBar) findViewById(R.id.progressBar);
        editTextEmail = (EditText) findViewById(R.id.editTextEmail);
        editTextPassword = (EditText) findViewById(R.id.editTextPassword);
        textViewRemindPassword = (TextView) findViewById(R.id.textViewRemindPassword);
        textViewLoginError = (TextView) findViewById(R.id.textViewLoginError);
        buttonLogIn = (Button) findViewById(R.id.buttonLogIn);
        buttonLogIn.setOnClickListener(v -> logIn());

        buttonRegister = (Button) findViewById(R.id.buttonRegisterUser);
        buttonRegister.setOnClickListener(v -> goToRegisterActivity());  //lambda expression
        textViewRemindPassword.setOnClickListener(v -> remindPassword());
        textViewLoginError.setVisibility(View.GONE);
        textViewLoginError.setOnClickListener(v -> sendAgainVeryficationEmail());
    }

    private void sendAgainVeryficationEmail() {
        firebaseUser.sendEmailVerification();
        Toast.makeText(getApplicationContext(), getResources().getString(R.string.checkEmail), Toast.LENGTH_LONG).show();
        textViewLoginError.setVisibility(View.GONE);

    }

    private void logIn() {
        progressbar.setVisibility(View.VISIBLE);
        String email = editTextEmail.getText().toString();
        String password = editTextPassword.getText().toString();

        FirebaseAuth.getInstance().signOut();
        if (validationEmailPassword(email, password))
            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        if (mAuth.getCurrentUser().isEmailVerified()) {
                            Toast.makeText(getApplicationContext(), getResources().getString(R.string.loginSucccesful), Toast.LENGTH_LONG).show();
                            progressbar.setVisibility(View.GONE);

                            //get FCM token
                            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
                                @Override
                                public void onComplete(@NonNull Task<String> task) {
                                    if (task.isSuccessful()) {
                                        String token = task.getResult();
                                        FirebaseUser currentUser = mAuth.getCurrentUser();
                                        if (currentUser != null) {
                                            String userId = currentUser.getUid();
                                            DatabaseReference userRef = myRef.child("users").child(userId);
                                            userRef.child("registrationToken").setValue(token);
                                        }
                                    } else {
                                        Log.e("TAG", "Failed to get FCM token: " + task.getException().getMessage());
                                    }

                                    // Continue with your code
                                    Intent intent = new Intent(LoginActivity.this, MapActivity.class);
                                    startActivity(intent);
                                }
                            });



                        } else {
                            emailVerification = editTextEmail.getText().toString();
                            firebaseUser = mAuth.getCurrentUser();
                            textViewLoginError.setText((getResources().getString(R.string.sendAgainEmailVeryfication)) + "\n" + emailVerification + "?");
                            textViewLoginError.setVisibility(View.VISIBLE);
                            progressbar.setVisibility(View.GONE);
                            return;
                        }
                    } else {
                        try {
                            throw task.getException();
                        } catch (FirebaseAuthInvalidCredentialsException e) {
                            Log.e("TAG", "Weak password.");
                            Toast.makeText(getApplicationContext(), getResources().getString(R.string.credentialsInvalid), Toast.LENGTH_LONG).show();
                        } catch (FirebaseAuthUserCollisionException e) {
                            Log.e("TAG", "User is already registered.");
                            Toast.makeText(getApplicationContext(), getResources().getString(R.string.invalidEmail), Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Log.e(TAG, e.getMessage());
                            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.loginFailed), Toast.LENGTH_LONG).show();
                        progressbar.setVisibility(View.GONE);
                    }
                }
            });
    }

    private void goToRegisterActivity() {
        startActivity(new Intent(this, RegisterActivity.class));
    }

    private boolean validationEmailPassword(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.pleaseEnterEmail), Toast.LENGTH_LONG).show();
            progressbar.setVisibility(View.GONE);
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.pleaseEnterPassword), Toast.LENGTH_LONG).show();
            progressbar.setVisibility(View.GONE);
            return false;
        }
        return true;
    }

    private void remindPassword() {
        dialogBuilder = new AlertDialog.Builder(this);
        final View popup_view = getLayoutInflater().inflate(R.layout.popup_remindpass, null);


        EditText editText = popup_view.findViewById(R.id.editTextTextEmailAddress);
        Button button = popup_view.findViewById(R.id.buttonRemindPassword);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String emailAddress = editText.getText().toString();
                if (emailAddress.isEmpty()) {
                    editText.setError("Wpisz adres e-mail");
                    return;
                }

                mAuth.getInstance().sendPasswordResetEmail(emailAddress)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(LoginActivity.this, "Wysłano e-mail umożliwiający resetowanie hasła", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                } else {
                                    Toast.makeText(LoginActivity.this, "Nie znaleziono konta powiązanego z tym adresem e-mail", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });

        dialogBuilder.setView(popup_view);
        dialog = dialogBuilder.create();
        dialog.show();
    }
}