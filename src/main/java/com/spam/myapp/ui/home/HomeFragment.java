package com.spam.myapp.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.spam.myapp.EmailCleaner;
import com.spam.myapp.EmailDetailActivity;
import com.spam.myapp.R;
import com.spam.myapp.SpamEmailDetector;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;

public class HomeFragment extends Fragment {

    RecyclerView recyclerView;
    private EmailAdapter emailAdapter;
    List<EmailItem> emailList;
    private String userEmail;
    private String userPassword;
    private String getMailHost(String email) {
        if (email.endsWith("@gmail.com")) {
            return "imap.gmail.com";

        } else if (email.endsWith("@outlook.com") || email.endsWith("@hotmail.com")) {
            return "imap-mail.outlook.com";
        } else if (email.endsWith("@yahoo.com")) {
            return "imap.mail.yahoo.com";
        } else if (email.endsWith("@zoho.com")) {
            return "imap.zoho.com";
        } else {
            return "imap.gmail.com"; // default fallback
        }
    }
    private ProgressBar progressBar;
     boolean emailsFetched = false;
    private boolean isFetchingEmails = false;
    private int emailOffset = 0;

    FirebaseAuth auth;
    FirebaseUser user;
    SpamEmailDetector spamDetector;
    private HomeViewModel homeViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        progressBar = view.findViewById(R.id.progressBar);

        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        emailList = homeViewModel.getEmailList();
        emailAdapter = new EmailAdapter(emailList);
        recyclerView.setAdapter(emailAdapter);
        SearchView searchView = view.findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterEmails(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterEmails(newText);
                return false;
            }
        });

        emailAdapter.setOnItemClickListener(email -> {
            Intent intent = new Intent(getActivity(), EmailDetailActivity.class);
            intent.putExtra("email_data", email);
            startActivity(intent);
        });

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        spamDetector = new SpamEmailDetector();

        if (user != null) {
            String userId = user.getUid();
            DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);

            // Fetch email and app password in one call
            databaseRef.get().addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    userEmail = snapshot.child("email").getValue(String.class);
                    userPassword = snapshot.child("appPassword").getValue(String.class);

                    if (userEmail != null && userPassword != null) {
                        trainAndFetchEmails(); // Start training & fetching emails
                    } else {
                        Toast.makeText(getContext(), "Email or app password missing!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        return view;
    }
    private void filterEmails(String query) {
        List<EmailItem> filteredList = new ArrayList<>();
        for (EmailItem email : emailList) {
            if (email.getSender().toLowerCase().contains(query.toLowerCase()) ||
                    email.getSubject().toLowerCase().contains(query.toLowerCase()) ||
                    email.getBody().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(email);
            }
        }
        emailAdapter.filterList(filteredList);
    }
    private void trainAndFetchEmails() {
        new Thread(() -> {
            try {
                if (getActivity() == null) {
                    Log.e("HomeFragment", "Activity is null, cannot train spam detector.");
                    return;
                }

                if (!spamDetector.isTrained()) {  // Check if training is needed
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getActivity(), "Training Spam Detector...", Toast.LENGTH_SHORT).show()
                    );

                    spamDetector.train(getActivity());

                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getActivity(), "Spam Detector Training Complete ! Fethcing Emails ....", Toast.LENGTH_SHORT).show()
                    );
                }

                fetchEmails();  // Proceed to fetch emails

            } catch (IOException e) {
                Log.e("HomeFragment", "Error training spam detector", e);
            }
        }).start();
    }
    private Store store;  // Declare store at the class level
    private Folder inbox;
    private void fetchEmails() {
        if (isFetchingEmails || !isAdded()) return;

        isFetchingEmails = true;
        requireActivity().runOnUiThread(() -> progressBar.setVisibility(View.VISIBLE));

        new Thread(() -> {
            try {
                if (store == null || !store.isConnected()) {
                    Properties properties = new Properties();
                    properties.put("mail.store.protocol", "imaps");
                    Session session = Session.getDefaultInstance(properties, null);
                    store = session.getStore("imaps");
                    String mailHost = getMailHost(userEmail);
                    store.connect(mailHost, userEmail, userPassword);
                }

                if (inbox == null || !inbox.isOpen()) {
                    inbox = store.getFolder("INBOX");
                    inbox.open(Folder.READ_ONLY);
                }

                int batchSize = 10;
                Message[] messages = inbox.getMessages();
                int totalEmails = messages.length;

                if (emailOffset >= totalEmails) {
                    emailsFetched = true;
                    return;
                }

                int startIdx = Math.max(totalEmails - (emailOffset + batchSize), 0);
                int endIdx = Math.max(totalEmails - emailOffset, 0);

                if (startIdx >= endIdx) {
                    emailsFetched = true;
                    return;
                }

                List<EmailItem> tempList = new ArrayList<>();
                for (int i = startIdx; i < endIdx; i++) {
                    Message message = messages[i];
                    String sender = ((InternetAddress) message.getFrom()[0]).getAddress();
                    String subject = message.getSubject();
                    String body = getEmailBody(message);

                    String cleanedBody = EmailCleaner.cleanEmailBody(body);
                    String classification = spamDetector.classify(cleanedBody, sender, subject);

                    if ("ham".equals(classification)) {
                        tempList.add(new EmailItem(sender, subject, cleanedBody, classification));
                    }
                }

                if (!isAdded()) return;

                List<EmailItem> finalList = tempList;
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    homeViewModel.addEmails(finalList);
                    emailAdapter.notifyItemRangeInserted(emailOffset, finalList.size());
                    emailOffset += finalList.size();
                    isFetchingEmails = false;

                    // 🚀 Trigger next batch dynamically
                    if (emailOffset < totalEmails) {
                        new Handler(requireActivity().getMainLooper()).postDelayed(this::fetchEmails, 2000);
                    } else {
                        emailsFetched = true;
                        Log.d("HomeFragment", "✅ All emails fetched successfully!");
                        Toast.makeText(getContext(), "All emails fetched.", Toast.LENGTH_SHORT).show();

                    }
                });

            } catch (MessagingException e) {
                Log.e("HomeFragment", "Error fetching emails", e);
                isFetchingEmails = false;

                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getActivity(), "Error fetching emails. Please try again.", Toast.LENGTH_SHORT).show();
                });

            }
        }).start();
    }



    @Override
    public void onResume() {
        super.onResume();

        // 🔧 Do not reset emailsFetched – let the fetchEmails continue naturally
        if (userEmail != null && userPassword != null && !emailsFetched && !isFetchingEmails) {
            fetchEmails();  // Just continue fetching if not finished
        }
    }


    private String getEmailBody(Message message) {
        try {
            Object content = message.getContent();
            if (content instanceof String) {
                return (String) content;
            } else if (content instanceof Multipart) {
                Multipart multipart = (Multipart) content;
                StringBuilder emailBody = new StringBuilder();
                for (int i = 0; i < multipart.getCount(); i++) {
                    BodyPart part = multipart.getBodyPart(i);
                    if (part.isMimeType("text/plain")) {
                        emailBody.append(part.getContent().toString()).append("\n");
                    }
                }
                return emailBody.toString().trim();
            } else {
                return "Unsupported email format.";
            }
        } catch (MessagingException | IOException e) {
            return "Error retrieving email content.";
        }
    }
}
