package com.spam.myapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.spam.myapp.databinding.ActivityDrawerBinding;

import java.util.HashMap;
import java.util.Map;

public class DrawerActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
     ActivityDrawerBinding binding;
     FirebaseAuth auth;
     FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityDrawerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        auth = FirebaseAuth.getInstance();      // ✅ Initialize here
        user = auth.getCurrentUser();
        FloatingActionButton fab = findViewById(R.id.fab);

        if (user != null) {
            String userId = user.getUid();
            DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);

            databaseRef.get().addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    String userEmail = snapshot.child("email").getValue(String.class);
                    String appPassword = snapshot.child("appPassword").getValue(String.class);

                    fab.setOnClickListener(v -> {
                        Intent intent = new Intent(DrawerActivity.this, SendEmailActivity.class);
                        intent.putExtra("sender_email", userEmail);
                        intent.putExtra("app_password", appPassword);
                        startActivity(intent);
                    });
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to load user credentials", Toast.LENGTH_SHORT).show();
            });
        }

        setSupportActionBar(binding.appBarDrawer.toolbar);


        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        // Setting up navigation
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_drawer);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Fetch and display user info in Navigation Drawer Header
        updateNavHeader(navigationView);
    }

    private void updateNavHeader(NavigationView navigationView) {
        View headerView = navigationView.getHeaderView(0);  // Access the nav-header view

        TextView usernameTextView = headerView.findViewById(R.id.user); // Make sure this matches nav-header XML
        TextView emailTextView = headerView.findViewById(R.id.email);   // Ensure this exists in nav-header XML

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        if (user != null) {
            emailTextView.setText(user.getEmail());  // Display email

            // Fetch username from Firebase Realtime Database
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(user.getUid());
            userRef.get().addOnSuccessListener(dataSnapshot -> {
                if (dataSnapshot.exists()) {
                    String username = dataSnapshot.child("name").getValue(String.class); // Get the name
                    if (username != null && !username.isEmpty()) {
                        usernameTextView.setText(username);
                    } else {
                        usernameTextView.setText("No Username");
                    }
                } else {
                    usernameTextView.setText("No Username");
                }
            }).addOnFailureListener(e ->
                    Toast.makeText(DrawerActivity.this, "Failed to load username", Toast.LENGTH_SHORT).show()
            );
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.drawer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, AboutUs.class);
            startActivity(intent);
            return true;

        } else if (id == R.id.action_toggle_notifications) {
            showNotificationDialog(); // Toggle on/off
            return true;
        }
        else if (id == R.id.action_feedback) {
            showFeedbackDialog(); // Toggle on/off
            return true;
        }
        else if (id == R.id.action_logout) {
            performLogout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void performLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(DrawerActivity.this, MainActivity.class));
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showFeedbackDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.feedback, null);
        builder.setView(dialogView);
        builder.setTitle("Feedback");

        RatingBar ratingBar = dialogView.findViewById(R.id.dialogRatingBar);
        EditText feedbackText = dialogView.findViewById(R.id.dialogFeedbackText);

        builder.setPositiveButton("Submit", null); // Set null to override below

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Override the positive button after dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            float rating = ratingBar.getRating();
            String feedback = feedbackText.getText().toString().trim();

            if (rating == 0 || feedback.isEmpty()) {
                Toast.makeText(this, "Please provide rating and feedback.", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = user.getUid();

            Map<String, Object> feedbackMap = new HashMap<>();
            feedbackMap.put("rating", rating);
            feedbackMap.put("feedback", feedback);

            FirebaseDatabase.getInstance().getReference("Feedback")
                    .child(userId)
                    .setValue(feedbackMap)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Thanks for your feedback!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to submit feedback.", Toast.LENGTH_SHORT).show());
        });
    }


    private void showNotificationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Notification Settings");
        builder.setMessage("Do you want to enable or disable notifications?");
        builder.setPositiveButton("Enable", (dialog, which) -> {
            Toast.makeText(this, "Notifications Enabled", Toast.LENGTH_SHORT).show();
            // You can save this preference in SharedPreferences
        });
        builder.setNegativeButton("Disable", (dialog, which) -> {
            Toast.makeText(this, "Notifications Disabled", Toast.LENGTH_SHORT).show();
        });
        builder.setCancelable(true);
        builder.show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_drawer);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }
}
