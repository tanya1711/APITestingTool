package org.example.util;

import java.util.ArrayList;
import java.util.List;

public class DataPool {

    private static List<String> emails;

    public static List<String> generateEmails() {
        emails = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            String newEmail = "testEmail_" + System.currentTimeMillis() + "@example.com";
            emails.add(newEmail);

        }
        return emails;
    }


}
