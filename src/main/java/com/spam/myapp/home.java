package com.spam.myapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import android.widget.ImageView;


public class home extends AppCompatActivity {

    TextView welcomeText;
    EditText emailInput, appPasswordInput;
    Button saveButton ;
    FirebaseAuth auth;
    DatabaseReference databaseRef;
    FirebaseUser user;
    ImageView infoIcon;  // FIX: Change Button to ImageView


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home); // Ensure XML file is correctly named

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        welcomeText = findViewById(R.id.inbox_display);
        emailInput = findViewById(R.id.email_input);
        appPasswordInput = findViewById(R.id.app_password_input);
        saveButton = findViewById(R.id.save_button);
        infoIcon = findViewById(R.id.info_icon); // FIX: Ensure infoIcon is an ImageView

        if (user != null) {
            String userId = user.getUid();
            databaseRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);

            welcomeText.setText("Welcome!");

            // Retrieve existing email if available
            databaseRef.child("email").get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.exists()) {
                            String email = snapshot.getValue(String.class);
                            emailInput.setText(email != null ? email.trim() : ""); // Avoid null values
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(home.this, "Error fetching email", Toast.LENGTH_SHORT).show());

            // Retrieve existing app password if available
            databaseRef.child("appPassword").get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.exists()) {
                            String appPassword = snapshot.getValue(String.class);
                            appPasswordInput.setText(appPassword != null ? appPassword.trim() : ""); // Avoid null values
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(home.this, "Error fetching password", Toast.LENGTH_SHORT).show());

            saveButton.setOnClickListener(view -> {
                String email = emailInput.getText().toString().trim();
                String appPassword = appPasswordInput.getText().toString().trim();

                if (TextUtils.isEmpty(email) || TextUtils.isEmpty(appPassword)) {
                    Toast.makeText(home.this, "Please enter email and app password", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Store email and app password in Firebase
                databaseRef.child("email").setValue(email);
                databaseRef.child("appPassword").setValue(appPassword)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(home.this, "Email saved successfully!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(home.this, DrawerActivity.class)); // Redirect to main activity
                            finish();
                        })
                        .addOnFailureListener(e -> Toast.makeText(home.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            });
            infoIcon.setOnClickListener(v -> showAppPasswordGuide());
        } else {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            finish(); // Close activity if no user is logged in
        }
    }
    private void showAppPasswordGuide() {
        new AlertDialog.Builder(this)
                .setTitle("How to Generate an App Password")
                .setMessage("To use this app, you need to generate an App Password from your Google account:\n\n"
                        + "1. Open your Google Account.\n"
                        + "2. Go to 'Security' > 'App Passwords'.\n"
                        + "3. Select 'Mail' as the app and 'Other' for the device.\n"
                        + "4. Click 'Generate' and copy the password.\n\n"
                        + "Use this password instead of your regular Gmail password.")
                .setPositiveButton("Open Google App Password Page", (dialog, which) -> {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://myaccount.google.com/apppasswords"));
                    startActivity(browserIntent);
                })
                .setNegativeButton("Close", null)
                .show();
    }
}
