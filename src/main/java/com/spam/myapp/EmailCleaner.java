package com.spam.myapp;

import android.os.Build;
import android.text.Html;

import org.jsoup.Jsoup;

public class EmailCleaner {

    public static String cleanEmailBody(String body) {
        if (body == null || body.isEmpty()) {
            return "No content available.";
        }

        // Decode basic HTML entities first
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            body = Html.fromHtml(body, Html.FROM_HTML_MODE_LEGACY).toString();
        } else {
            body = Html.fromHtml(body).toString();
        }

        // Use Jsoup to parse and clean HTML completely
        body = Jsoup.parse(body).text();

        // Remove CSS-style garbage if any left
        body = body.replaceAll("(?s)@media[^\\{]*\\{[^\\}]*\\}", " ");
        body = body.replaceAll("(?s)\\.[\\w\\-]+\\s*\\{[^\\}]*\\}", " ");

        // Remove tracking URLs and LinkedIn junk
        body = body.replaceAll("https://www\\.linkedin\\.com/comm/jobs/view/\\d+[^\\s]*", "");
        body = body.replaceAll("https://www\\.linkedin\\.com/[^\\s]+", "");
        body = body.replaceAll("(trk|trkEmail|otpToken|midSig|refId|lipi|eid)[=\\w\\-_.%]+", "");

        // Remove unwanted characters, allow only meaningful punctuation
        body = body.replaceAll("[^\\p{L}\\p{N}.,!?@'\"\\s:/\\-_]", " ");

        // Normalize spacing
        body = body.replaceAll("\\s+", " ").trim();

        return body;
    }

    public static String cleanSubject(String subject) {
        if (subject == null || subject.isEmpty()) {
            return "No Subject";
        }

        return subject.replaceAll("[^a-zA-Z0-9.,!?@'\\s]", " ").trim();
    }
}
