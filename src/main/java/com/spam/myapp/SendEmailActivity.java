package com.spam.myapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SendEmailActivity extends AppCompatActivity {

    EditText toEditText, subjectEditText, bodyEditText;
    Button sendButton;

    String senderEmail;
    String appPassword;

    private SpamEmailDetector spamEmailDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_email);

        // Get sender credentials from Intent
        Intent intent = getIntent();
        if (intent != null) {
            senderEmail = intent.getStringExtra("sender_email");
            appPassword = intent.getStringExtra("app_password");
        }

        // Initialize views
        toEditText = findViewById(R.id.toEditText);
        subjectEditText = findViewById(R.id.subjectEditText);
        bodyEditText = findViewById(R.id.bodyEditText);
        sendButton = findViewById(R.id.sendButton);

        // Initialize spam detector and train it
        spamEmailDetector = new SpamEmailDetector();
        new Thread(() -> {
            try {
                spamEmailDetector.train(getApplicationContext());
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Failed to train spam detector", Toast.LENGTH_LONG).show());
            }
        }).start();

        // Handle send button
        sendButton.setOnClickListener(v -> {
            String to = toEditText.getText().toString().trim();
            String subject = subjectEditText.getText().toString().trim();
            String message = bodyEditText.getText().toString().trim();

            if (to.isEmpty() || subject.isEmpty() || message.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (senderEmail == null || appPassword == null) {
                Toast.makeText(this, "Missing sender credentials", Toast.LENGTH_LONG).show();
                return;
            }

            // Classify the email using the detector
            if (!spamEmailDetector.isTrained()) {
                Toast.makeText(this, "Spam detector is still training. Please wait.", Toast.LENGTH_LONG).show();
                return;
            }

            String classification = spamEmailDetector.classify(message, senderEmail, subject);
            if ("spam".equals(classification)) {
                Toast.makeText(this, "Sending blocked: The message looks like spam.", Toast.LENGTH_LONG).show();
                return;
            }

            // Proceed with sending email
            new Thread(() -> {
                try {
                    GmailSender.sendEmail(senderEmail, appPassword, to, subject, message);
                    runOnUiThread(() -> Toast.makeText(this, "Email sent!", Toast.LENGTH_SHORT).show());
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            }).start();
        });
    }
}
