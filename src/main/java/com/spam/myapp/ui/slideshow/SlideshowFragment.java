package com.spam.myapp.ui.slideshow;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
  import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import android.graphics.Color;
import java.util.ArrayList;
import java.util.List;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.spam.myapp.databinding.FragmentSlideshowBinding;

public class SlideshowFragment extends Fragment {

    private FragmentSlideshowBinding binding;
    private DatabaseReference databaseReference;
     String userId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSlideshowBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(userId);

        fetchSpamStats();

        return root;
    }


    private void fetchSpamStats() {
        DatabaseReference emailsRef = databaseReference.child("Emails");
        DatabaseReference spamRef = databaseReference.child("SpamEmails");

        emailsRef.get().addOnSuccessListener(hamSnapshot -> {
            int hamCount = (int) hamSnapshot.getChildrenCount();

            spamRef.get().addOnSuccessListener(spamSnapshot -> {
                int spamCount = (int) spamSnapshot.getChildrenCount();
                int totalCount = hamCount + spamCount;

                if (binding != null) {
                    binding.textSlideshow.setText("Total Emails Scanned: " + totalCount);
                    binding.spamEmails.setText("Spam Emails Detected: " + spamCount);
                    binding.hamEmails.setText("Non-Spam Emails: " + hamCount);

                    setupPieChart(spamCount, hamCount);
                }
            });
        });
    }

    private void setupPieChart(int spamCount, int hamCount) {
        PieChart pieChart = binding.pieChart;

        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(spamCount, "Spam"));
        entries.add(new PieEntry(hamCount, "Ham"));

        PieDataSet dataSet = new PieDataSet(entries, "Email Stats");
        dataSet.setColors(Color.RED, Color.GREEN);
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData pieData = new PieData(dataSet);

        pieChart.setData(pieData);
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setEntryLabelTextSize(12f);

        Legend legend = pieChart.getLegend();
        legend.setEnabled(true);
        legend.setTextColor(Color.DKGRAY);
        legend.setTextSize(12f);

        pieChart.invalidate(); // Refresh chart
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
