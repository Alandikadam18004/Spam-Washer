package com.spam.myapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class login extends AppCompatActivity {

    EditText email, pass;
    Button login_btn, loginback_btn;
    TextView registerText, forgotPasswordText;
    FirebaseAuth auth;
    DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        email = findViewById(R.id.login_email);
        pass = findViewById(R.id.login_pass);
        registerText = findViewById(R.id.register_text);
        forgotPasswordText = findViewById(R.id.resetpass);
        login_btn = findViewById(R.id.login_btn);
        loginback_btn = findViewById(R.id.loginback_btn);

        auth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        // Register Redirect
        registerText.setOnClickListener(view ->
                startActivity(new Intent(login.this, register.class))
        );

        // Forgot Password Feature
        forgotPasswordText.setOnClickListener(view -> {
            String emailInput = email.getText().toString().trim();
            if (TextUtils.isEmpty(emailInput)) {
                Toast.makeText(login.this, "Enter your email to reset password", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.sendPasswordResetEmail(emailInput)
                    .addOnSuccessListener(unused ->
                            Toast.makeText(login.this, "Reset link sent to your email", Toast.LENGTH_LONG).show()
                    )
                    .addOnFailureListener(e ->
                            Toast.makeText(login.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
        });

        // Back Button
        loginback_btn.setOnClickListener(view -> {
            startActivity(new Intent(login.this, MainActivity.class));
            finish();
        });

        // Login Button
        login_btn.setOnClickListener(view -> {
            String emailInput = email.getText().toString().trim();
            String passInput = pass.getText().toString().trim();

            if (TextUtils.isEmpty(emailInput) || TextUtils.isEmpty(passInput)) {
                Toast.makeText(login.this, "Email and Password are required.", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.signInWithEmailAndPassword(emailInput, passInput)
                    .addOnSuccessListener(authResult -> {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            String userId = user.getUid();
                            usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (snapshot.exists()) {
                                        Boolean isActive = snapshot.child("isActive").getValue(Boolean.class);
                                        String username = snapshot.child("name").getValue(String.class);  // Fetch name
                                        String userEmail = snapshot.child("email").getValue(String.class); // Fetch email

                                        if (Boolean.TRUE.equals(isActive)) {
                                            // Save user data in SharedPreferences
                                            SharedPreferences prefs = getSharedPreferences("UserData", MODE_PRIVATE);
                                            SharedPreferences.Editor editor = prefs.edit();
                                            editor.putString("username", username);
                                            editor.putString("email", userEmail);
                                            editor.apply();

                                            Toast.makeText(login.this, "Login Successful", Toast.LENGTH_SHORT).show();

                                            // Pass data to DrawerActivity
                                            Intent intent = new Intent(login.this, home.class);
                                            intent.putExtra("username", username);
                                            intent.putExtra("email", userEmail);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            Toast.makeText(login.this, "Your account is inactive. Contact support.", Toast.LENGTH_LONG).show();
                                            auth.signOut();
                                        }
                                    } else {
                                        Toast.makeText(login.this, "User record not found.", Toast.LENGTH_LONG).show();
                                        auth.signOut();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Toast.makeText(login.this, "Failed to fetch user data: " + error.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(login.this, "Login Failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
        });
    }
}
