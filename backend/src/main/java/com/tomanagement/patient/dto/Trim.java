package com.tomanagement.patient.dto;

/**
 * Null-safe trimming for request fields.
 */
final class Trim {

    private Trim() {
    }

    static String of(String value) {
        return value == null ? null : value.trim();
    }
}
