package com.spam.myapp.ui.home;
import androidx.lifecycle.ViewModel;
import java.util.ArrayList;
import java.util.List;

public class HomeViewModel extends ViewModel {
    private List<EmailItem> emailList = new ArrayList<>();

    public List<EmailItem> getEmailList() {
        return emailList;
    }

    public void setEmailList(List<EmailItem> emails) {
        this.emailList = emails;
    }

    public void addEmails(List<EmailItem> emails) {
        emailList.addAll(emails);
    }
}
