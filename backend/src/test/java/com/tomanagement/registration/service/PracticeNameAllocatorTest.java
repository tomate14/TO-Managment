package com.tomanagement.registration.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Test;

class PracticeNameAllocatorTest {

    @Test
    void usesTheNameWhenFree() {
        assertThat(PracticeNameAllocator.allocate("Ana Pérez", name -> false)).isEqualTo("Ana Pérez");
    }

    @Test
    void addsTheFirstFreeSuffix() {
        Set<String> taken = Set.of("ana pérez", "ana pérez (2)");
        assertThat(PracticeNameAllocator.allocate("Ana Pérez", n -> taken.contains(n.toLowerCase(Locale.ROOT))))
                .isEqualTo("Ana Pérez (3)");
    }

    @Test
    void dropsTrailingWhitespaceLeftByTheTruncation() {
        String base = "a".repeat(115) + " " + "b".repeat(14);
        String result = PracticeNameAllocator.allocate(base, n -> n.equals(base));
        assertThat(result).isEqualTo("a".repeat(115) + " (2)");
    }

    @Test
    void keepsTheSuffixWithin120Characters() {
        String base = "x".repeat(120);
        String result = PracticeNameAllocator.allocate(base, n -> n.equals(base));
        assertThat(result).hasSizeLessThanOrEqualTo(120).endsWith(" (2)");
    }
}
