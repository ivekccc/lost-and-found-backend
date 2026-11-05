package com.example.demo.user.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Locale;

@Schema(name = "AppLanguage", enumAsRef = true)
public enum AppLanguage {
    SR_LATN("sr-Latn"),
    EN("en");

    private final String tag;

    AppLanguage(String tag) {
        this.tag = tag;
    }

    public Locale toLocale() {
        return Locale.forLanguageTag(tag);
    }

    public static AppLanguage fromLocale(Locale locale) {
        return locale != null && "sr".equals(locale.getLanguage()) ? SR_LATN : EN;
    }
}
