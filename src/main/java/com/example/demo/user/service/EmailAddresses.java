package com.example.demo.user.service;

import java.util.Locale;

public final class EmailAddresses {

    private EmailAddresses() {
    }

    public static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
