package com.tomanagement.registration.service;

import java.util.function.Predicate;

import com.tomanagement.establishment.entity.EstablishmentField;

/**
 * Picks a unique practice name: the therapist's name, or the first free "Name (n)" (n >= 2), within the column size.
 */
final class PracticeNameAllocator {

    private PracticeNameAllocator() {
    }

    static String allocate(String base, Predicate<String> taken) {
        if (!taken.test(base)) {
            return base;
        }
        int maxLength = EstablishmentField.NAME.maxLength();
        for (int n = 2; ; n++) {
            String suffix = " (" + n + ")";
            String candidate = base.substring(0, Math.min(base.length(), maxLength - suffix.length())) + suffix;
            if (!taken.test(candidate)) {
                return candidate;
            }
        }
    }
}
