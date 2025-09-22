package com.spam.myapp;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.spam.myapp.ui.home.EmailItem;


public class EmailDetailActivity extends AppCompatActivity {

    private TextView senderTextView, dateTextView, bodyTextView;
     Toolbar toolbar;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_detail);

        toolbar = findViewById(R.id.toolBar);
        senderTextView = findViewById(R.id.senderTextView);
        dateTextView = findViewById(R.id.dateTextView);
        bodyTextView = findViewById(R.id.bodyTextView);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Get the email data passed from HomeFragment
        EmailItem email = getIntent().getParcelableExtra("email_data");

        if (email != null) {
            displayEmailDetails(email);

        } else {
            bodyTextView.setText("Error: No email data available.");
        }

        // Enable long press to copy email body
        bodyTextView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                copyToClipboard(bodyTextView.getText().toString());
                return true; // Consume the event
            }
        });
    }

    private void displayEmailDetails(EmailItem email) {
        senderTextView.setText("From: " + email.getSender());
        dateTextView.setText("Subject: " + email.getSubject());

        // Clean the email body before displaying
        String cleanedBody = EmailCleaner.cleanEmailBody(email.getBody());
        bodyTextView.setText(cleanedBody);
        bodyTextView.setMovementMethod(new ScrollingMovementMethod()); // Allow scrolling for long text
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Email Content", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Email copied!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
