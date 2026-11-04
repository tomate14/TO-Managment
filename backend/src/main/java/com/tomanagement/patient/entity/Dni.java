package com.tomanagement.patient.entity;

/**
 * Single rule for DNIs: only digits are kept, so "30.123.456" and "30123456" are the same patient.
 */
public final class Dni {

    private Dni() {
    }

    /**
     * Removes spaces, dots and dashes (validation of the result happens in the request DTO).
     */
    public static String normalize(String raw) {
        return raw == null ? null : raw.replaceAll("[\\s.\\-]", "");
    }
}
