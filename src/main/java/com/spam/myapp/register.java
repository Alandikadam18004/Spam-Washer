package com.spam.myapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;

public class register extends AppCompatActivity {

    EditText registerName, registerEmail, registerPass, registerPhone;
    Button createBtn, createBackBtn;
    FirebaseAuth auth;
    TextView reset;
    DatabaseReference databaseRef;  // Realtime Database Reference

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.register);

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();

        // UI Elements
        registerName = findViewById(R.id.register_name);
        registerEmail = findViewById(R.id.register_email);
        registerPass = findViewById(R.id.register_pass);
        registerPhone = findViewById(R.id.register_phone);
        reset=findViewById(R.id.reset);
        createBtn = findViewById(R.id.create_btn);
        createBackBtn = findViewById(R.id.createback_btn);

        // Go back to MainActivity
        createBackBtn.setOnClickListener(view -> {
            startActivity(new Intent(register.this, MainActivity.class));
        });
        reset.setOnClickListener(view -> {
            startActivity(new Intent(register.this, login.class));
        });

        // Create Account Button Click
        createBtn.setOnClickListener(view -> {
            String name = registerName.getText().toString();
            String email = registerEmail.getText().toString();
            String pass = registerPass.getText().toString();
            String phone = registerPhone.getText().toString();

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(pass) || TextUtils.isEmpty(phone)) {
                Toast.makeText(register.this, "All fields are required!", Toast.LENGTH_SHORT).show();
            } else if (!phone.matches("\\d{10}")) {  // Regex for exactly 10 digits
                Toast.makeText(register.this, "Phone number must be exactly 10 digits!", Toast.LENGTH_SHORT).show();
            } else{
                registerUser(name, email, pass, phone);
            }
        });
    }

    private void registerUser(String name, String email, String password, String phone) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(register.this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = auth.getCurrentUser();
                if (user != null) {
                    // Reference to Firebase Realtime Database
                    String userId = auth.getCurrentUser().getUid();
                    databaseRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);

                    // Storing user data in HashMap
                    HashMap<String, Object> userData = new HashMap<>();
                    userData.put("name", name);
                    userData.put("email", email);
                    userData.put("phone", phone);
                    userData.put("password", password);
                    userData.put("isActive", true); // Not recommended for security reasons

                    // Save data in Realtime Database
                    databaseRef.setValue(userData).addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()) {
                            Toast.makeText(register.this, "Successfully Registered!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(register.this, login.class));
                            finish();
                        } else {
                            Toast.makeText(register.this, "Failed to store d ata!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else {
                Toast.makeText(register.this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

