package com.spam.myapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.Manifest;
import android.os.Build;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.annotation.NonNull;


public class MainActivity extends AppCompatActivity {

    Button loginBtn, createAccountBtn;
    FirebaseAuth auth;
    private static final int NOTIFICATION_PERMISSION_CODE = 100;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseAuth.getInstance().signOut();
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        createNotificationChannel(); // Create notification channel

        loginBtn = findViewById(R.id.loginacc_btn);
        createAccountBtn = findViewById(R.id.createacc_btn);
        auth = FirebaseAuth.getInstance();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }

        // If user is already logged in, move to home page
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            startActivity(new Intent(MainActivity.this, home.class));
            finish();
        }

        // Navigate to Register Page
        createAccountBtn.setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, register.class));
        });

        // Navigate to Login Page
        loginBtn.setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, login.class));
        });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification Permission Granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification Permission Denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }
    public void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "SPAM_ALERT_CHANNEL",
                    "Spam Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifies when a new spam email is detected");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }}
