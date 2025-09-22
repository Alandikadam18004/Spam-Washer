package com.spam.myapp;

import android.app.NotificationManager;
import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

public class SpamEmailDetector {
    private final List<String> emails = new ArrayList<>();
    private final List<String> labels = new ArrayList<>();
    private final Set<String> vocabulary = new HashSet<>();
    private double[][] tfidf;
    private boolean trained = false;
    private static int spamCount = 0;
    private static final int SPAM_THRESHOLD = 10;
     Map<String, Integer> vocabIndex = new HashMap<>();
     Map<String, Integer> documentFrequency = new HashMap<>();

    public boolean isTrained() {
        return trained;
    }

    public void train(Context context) throws IOException {
        if (trained) {
            Log.d("SpamEmailDetector", "Model already trained. Skipping training.");
            return;
        }

        AssetManager assetManager = context.getAssets();
        String[] languages = {
                "spamkk.csv", "spam_spanish.csv", "spam_marathi.csv",
                "spam_hindi.csv", "spam_french.csv", "spam_arabic.csv"
        };

        for (String language : languages) {
            try (InputStream inputStream = assetManager.open(language)) {
                readCSV(inputStream);
            } catch (IOException e) {
                Log.e("SpamEmailDetector", "Error reading CSV file: " + language, e);
            }
        }

        createVocabulary();
        calculateTfIdf();
        trained = true;
        Log.d("SpamEmailDetector", "Training completed successfully.");
    }

    private void readCSV(InputStream csvFileInputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csvFileInputStream, StandardCharsets.UTF_8))) {
            String line;
            reader.readLine(); // Skip header
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", 2);
                if (parts.length == 2) {
                    labels.add(parts[0].trim().toLowerCase());
                    emails.add(preprocessText(parts[1]));
                }
            }
        }
    }

    private String preprocessText(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{Punct}", " ")
                .replaceAll("<[^>]+>", " ") // Remove HTML tags
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();
    }

    private void createVocabulary() {
        for (String email : emails) {
            vocabulary.addAll(Arrays.asList(email.split("\\s+")));
        }
        tfidf = new double[emails.size()][vocabulary.size()];
    }

    private void calculateTfIdf() {
        List<String> vocabList = new ArrayList<>(vocabulary);
        vocabIndex.clear();
        for (int i = 0; i < vocabList.size(); i++) {
            vocabIndex.put(vocabList.get(i), i);
        }

        documentFrequency.clear();

        // Compute DF
        for (String email : emails) {
            Set<String> uniqueWords = new HashSet<>(Arrays.asList(email.split("\\s+")));
            for (String word : uniqueWords) {
                int df = documentFrequency.containsKey(word) ? documentFrequency.get(word) : 0;
                documentFrequency.put(word, df + 1);
            }
        }

        // Compute TF-IDF
        for (int i = 0; i < emails.size(); i++) {
            String[] words = emails.get(i).split("\\s+");
            Map<String, Integer> wordCounts = new HashMap<>();

            for (String word : words) {
                int count = wordCounts.containsKey(word) ? wordCounts.get(word) : 0;
                wordCounts.put(word, count + 1);
            }

            for (String word : wordCounts.keySet()) {
                if (vocabIndex.containsKey(word)) {
                    int wordIndex = vocabIndex.get(word);
                    int tf = wordCounts.get(word);
                    int df = documentFrequency.containsKey(word) ? documentFrequency.get(word) : 1; // avoid div by 0
                    double idf = Math.log((double) emails.size() / (1 + df)) + 1;
                    tfidf[i][wordIndex] = tf * idf;
                }
            }
        }
    }
    public void saveUserFeedback(String emailId, String correctLabel, String previousLabel, String sender, String subject) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference feedbackRef = database.getReference("UserFeedback");

        String feedbackId = feedbackRef.push().getKey();
        if (feedbackId == null) return;

        Map<String, Object> feedback = new HashMap<>();
        feedback.put("emailId", emailId);
        feedback.put("correctLabel", correctLabel);
        feedback.put("previousLabel", previousLabel);
        feedback.put("sender", sender);
        feedback.put("subject", subject);
        feedback.put("timestamp", System.currentTimeMillis());

        feedbackRef.child(feedbackId).setValue(feedback);
    }

    public String classify(String email, String sender, String subject) {
        email = preprocessText(email);
        String[] words = email.split("\\s+");

        double[] emailTfidf = new double[vocabIndex.size()];
        for (String word : words) {
            Integer wordIndex = vocabIndex.get(word);
            if (wordIndex != null && wordIndex < emailTfidf.length) {
                emailTfidf[wordIndex] += 1;
            }
        }

        if (tfidf == null || tfidf.length == 0) {
            Log.e("SpamEmailDetector", "TF-IDF not initialized. Train the model first.");
            return "ham";
        }

        double spamScore = 0, hamScore = 0;
        for (int i = 0; i < tfidf.length; i++) {
            double similarity = calculateSimilarity(emailTfidf, tfidf[i]);
            if (labels.get(i).equals("spam")) {
                spamScore += similarity;
            } else {
                hamScore += similarity;
            }
        }

        // BOOST SCORE BASED ON FEATURES
        if (email.contains("http://") || email.contains("https://")) {
            spamScore += 1.5;
        }
        if (email.contains(".pdf") || email.contains(".doc") || email.contains("attachment")) {
            spamScore += 1.0;
        }

        String[] spamWords = {"winner", "congratulations", "claim", "lottery", "selected", "prize", "urgent"};
        for (String word : spamWords) {
            if (email.contains(word)) {
                spamScore += 0.5;
            }
        }

        if (email.length() > 1000) {
            spamScore += 0.5;
        }

        String result = spamScore > hamScore * 1.2 ? "spam" : "ham";

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            saveEmailToFirebase(email, result, sender, subject);
        }

        if (result.equals("spam")) {
            spamCount++;
            if (spamCount % SPAM_THRESHOLD == 0) {
                sendSpamNotification();
            }
        }

        Log.d("SpamDetection", "SPAM SCORE: " + spamScore + ", HAM SCORE: " + hamScore);
        return result;
    }

    private void sendSpamNotification() {
        Context context = FirebaseAuth.getInstance().getApp().getApplicationContext();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "SPAM_ALERT_CHANNEL")
                .setSmallIcon(R.drawable.logo)
                .setContentTitle("Spam Email Detected!")
                .setContentText("Multiple spam emails detected.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(1, builder.build());
        }
    }

    private void saveEmailToFirebase(String emailText, String label, String sender, String subject) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // User is logged out, skip saving
            return; // <-- Added null check to avoid crash
        }

        String userId = currentUser.getUid();
        DatabaseReference emailRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);
        DatabaseReference targetRef = "spam".equals(label) ? emailRef.child("SpamEmails") : emailRef.child("Emails");

        targetRef.orderByChild("subject").equalTo(subject).get().addOnCompleteListener(task -> {
            boolean isDuplicate = false;

            if (task.isSuccessful() && task.getResult().exists()) {
                for (DataSnapshot snapshot : task.getResult().getChildren()) {
                    String existingSender = snapshot.child("sender").getValue(String.class);
                    if (existingSender != null &&
                            existingSender.trim().equalsIgnoreCase(sender.trim())) {
                        Log.d("SpamDetection", "Duplicate email detected, skipping storage.");
                        isDuplicate = true;
                        break;
                    }
                }
            }

            if (!isDuplicate) {
                String emailId = targetRef.push().getKey();
                if (emailId != null) {
                    Map<String, Object> emailData = new HashMap<>();
                    emailData.put("sender", sender);
                    emailData.put("subject", subject);
                    emailData.put("body", emailText);
                    emailData.put("classification", label);
                    targetRef.child(emailId).setValue(emailData);
                }
            }
        });
    }

    private double calculateSimilarity(double[] vector1, double[] vector2) {
        double dotProduct = 0, magnitude1 = 0, magnitude2 = 0;
        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            magnitude1 += Math.pow(vector1[i], 2);
            magnitude2 += Math.pow(vector2[i], 2);
        }
        magnitude1 = Math.sqrt(magnitude1);
        magnitude2 = Math.sqrt(magnitude2);
        return magnitude1 == 0 || magnitude2 == 0 ? 0 : dotProduct / (magnitude1 * magnitude2);
    }
}
