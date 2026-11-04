package com.tomanagement.user.entity;

import java.util.Locale;

/**
 * Single rule for comparing emails: trimmed and lower-cased, so lookups and uniqueness ignore case and spaces.
 */
public final class EmailNormalizer {

    private EmailNormalizer() {
    }

    public static String normalize(String raw) {
        return raw == null ? null : raw.trim().toLowerCase(Locale.ROOT);
    }
}
