package com.spam.myapp.ui.home;

import android.os.Parcel;
import android.os.Parcelable;

public class EmailItem implements Parcelable {
    String sender;
    String subject;
    String body;
    String classification;
     String emailId; // Unique ID for each email
// Added classification field

    // Updated constructor with classification
    public EmailItem(String sender, String subject, String body, String classification) {
        this.sender = sender;
        this.subject = subject;
        this.body = body;
        this.classification = classification;
    }

    // Original constructor for compatibility
    public EmailItem(String sender, String subject, String body) {
        this.sender = sender;
        this.subject = subject;
        this.body = body;
        this.classification = "unknown"; // Default value
    }

    protected EmailItem(Parcel in) {
        emailId = in.readString();
        sender = in.readString();
        subject = in.readString();
        body = in.readString();
        classification = in.readString();
    }

    public static final Creator<EmailItem> CREATOR = new Creator<EmailItem>() {
        @Override
        public EmailItem createFromParcel(Parcel in) {
            return new EmailItem(in);
        }

        @Override
        public EmailItem[] newArray(int size) {
            return new EmailItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(emailId);
        dest.writeString(sender);
        dest.writeString(subject);
        dest.writeString(body);
        dest.writeString(classification);
    }

    public String getSender() {
        return sender;
    }
    public String getEmailId() {
        return emailId;
    }
    public void setEmailId(String emailId) {
        this.emailId = emailId;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public String getClassification() {
        return classification;
    }
}
