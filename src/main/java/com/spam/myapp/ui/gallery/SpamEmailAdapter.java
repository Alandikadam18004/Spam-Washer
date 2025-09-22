package com.spam.myapp.ui.gallery;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.spam.myapp.R;
import com.spam.myapp.ui.home.EmailItem;

import java.util.List;

public class SpamEmailAdapter extends RecyclerView.Adapter<SpamEmailAdapter.ViewHolder> {

    private final List<EmailItem> spamEmailList;

    public SpamEmailAdapter(List<EmailItem> spamEmailList) {
        this.spamEmailList = spamEmailList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_spam_email, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EmailItem email = spamEmailList.get(position);
        holder.senderTextView.setText(email.getSender());
        holder.subjectTextView.setText(email.getSubject());
        holder.emailTextView.setText(email.getBody()); // Ensure this matches EmailItem

    }

    @Override
    public int getItemCount() {
        return spamEmailList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView senderTextView, subjectTextView,emailTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            senderTextView = itemView.findViewById(R.id.senderTextView);
            subjectTextView = itemView.findViewById(R.id.subjectTextView);
            emailTextView = itemView.findViewById(R.id.emailTextView); //
        }
    }
}
