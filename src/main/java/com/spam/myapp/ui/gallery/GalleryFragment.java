package com.spam.myapp.ui.gallery;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.spam.myapp.R;
import com.spam.myapp.ui.home.EmailAdapter;
import com.spam.myapp.ui.home.EmailItem;
import com.spam.myapp.EmailDetailActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GalleryFragment extends Fragment {

    private RecyclerView recyclerView;
    private EmailAdapter emailAdapter;
    private List<EmailItem> spamEmailList;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private DatabaseReference databaseRef;
    private TextView noSpamMessage;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gallery, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        noSpamMessage = view.findViewById(R.id.noSpamText);

        spamEmailList = new ArrayList<>();
        emailAdapter = new EmailAdapter(spamEmailList);

        emailAdapter.setOnItemClickListener(email -> {
            Intent intent = new Intent(getActivity(), EmailDetailActivity.class);
            intent.putExtra("email_data", email);
            startActivity(intent);
        });

        recyclerView.setAdapter(emailAdapter);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        if (user != null) {
            String userId = user.getUid();
            databaseRef = FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(userId)
                    .child("SpamEmails");

            fetchSpamEmails();
        } else {
            Toast.makeText(getActivity(), "User not authenticated", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    private void fetchSpamEmails() {
        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                spamEmailList.clear();
                Set<String> uniqueEmails = new HashSet<>(); // 🔹 Track unique emails by "sender+subject"

                for (DataSnapshot emailSnapshot : snapshot.getChildren()) {
                    String sender = emailSnapshot.child("sender").getValue(String.class);
                    String subject = emailSnapshot.child("subject").getValue(String.class);
                    String body = emailSnapshot.child("body").getValue(String.class);

                    if (sender != null && body != null) {
                        if (subject == null) subject = "(No Subject)";
                        String uniqueKey = sender + subject; // 🔹 Unique key to prevent duplicates
                        if (!uniqueEmails.contains(uniqueKey)) {
                            uniqueEmails.add(uniqueKey);
                            spamEmailList.add(new EmailItem(sender, subject, body));
                        }
                    }
                }

                if (spamEmailList.isEmpty()) {
                    noSpamMessage.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    noSpamMessage.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    emailAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getActivity(), "Failed to load spam emails", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
