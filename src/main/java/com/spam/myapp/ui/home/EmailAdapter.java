package com.spam.myapp.ui.home;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.spam.myapp.EmailCleaner;
import com.spam.myapp.R;

import java.util.List;

public class EmailAdapter extends RecyclerView.Adapter<EmailAdapter.EmailViewHolder> {
    List<EmailItem> emailList;
    private OnItemClickListener listener;

    // Define an interface for item click handling
    public interface OnItemClickListener {
        void onItemClick(EmailItem email);
    }

    // Method to set the listener
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    // Constructor
    public EmailAdapter(List<EmailItem> emailList) {
        this.emailList = emailList;
    }

    @NonNull
    @Override
    public EmailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_email_item, parent, false);
        return new EmailViewHolder(view);
    }
    public void filterList(List<EmailItem> filteredList) {
        this.emailList = filteredList;
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull EmailViewHolder holder, int position) {
        EmailItem email = emailList.get(position);
        holder.senderTextView.setText(email.getSender());
        holder.subjectTextView.setText(email.getSubject());
        holder.emailBodyTextView.setOnLongClickListener(view -> {
            ClipboardManager clipboard = (ClipboardManager) view.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Spam Email", email.getBody()); // Copy email body
            clipboard.setPrimaryClip(clip);
            Toast.makeText(view.getContext(), "Email copied!", Toast.LENGTH_SHORT).show();
            return true; // Consume long click event
        });

        // Handle item click
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(email);
            }
        });
    }

    @Override
    public int getItemCount() {
        return emailList.size();
    }

    public static class EmailViewHolder extends RecyclerView.ViewHolder {
        TextView senderTextView, emailBodyTextView, subjectTextView;

        public EmailViewHolder(@NonNull View itemView) {
            super(itemView);
            senderTextView = itemView.findViewById(R.id.senderTextView);
            subjectTextView = itemView.findViewById(R.id.subjectTextView);
            emailBodyTextView = itemView.findViewById(R.id.emailBodyTextView); // Initialize it

        }
    }
}
