package com.tomanagement.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SafeThrowableDescriberTest {

    @Test
    void keepsTheTypeChainButNoMessages() {
        IllegalStateException cause = new IllegalStateException("Key (dni)=(30123456) already exists");
        RuntimeException failure = new RuntimeException("could not execute statement: Lucía Fernández", cause);

        String description = SafeThrowableDescriber.describe(failure);

        assertThat(description)
                .startsWith("java.lang.RuntimeException <- java.lang.IllegalStateException")
                .doesNotContain("30123456")
                .doesNotContain("Lucía")
                .contains(" at com.tomanagement.common.logging.SafeThrowableDescriberTest");
    }

    @Test
    void survivesCauseCycles() {
        RuntimeException first = new RuntimeException("a");
        RuntimeException second = new RuntimeException("b", first);
        first.initCause(second);

        assertThat(SafeThrowableDescriber.describe(second)).isNotBlank();
    }
}
