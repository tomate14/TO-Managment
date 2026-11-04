package com.tomanagement.common.logging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Describes a failure for the logs without any exception message: messages from the driver, Hibernate or
 * validation can contain patient data (e.g. "Key (dni)=(30123456) already exists"). Only exception types and the
 * first frame of application code are kept.
 */
public final class SafeThrowableDescriber {

    private static final String APPLICATION_PACKAGE = "com.tomanagement.";
    private static final String CAUSE_SEPARATOR = " <- ";

    private SafeThrowableDescriber() {
    }

    public static String describe(Throwable throwable) {
        List<String> types = new ArrayList<>();
        Set<Throwable> seen = new HashSet<>();
        Optional<StackTraceElement> applicationFrame = Optional.empty();
        for (Throwable current = throwable; current != null && seen.add(current); current = current.getCause()) {
            types.add(current.getClass().getName());
            if (applicationFrame.isEmpty()) {
                applicationFrame = Arrays.stream(current.getStackTrace())
                        .filter(frame -> frame.getClassName().startsWith(APPLICATION_PACKAGE))
                        .findFirst();
            }
        }
        String chain = String.join(CAUSE_SEPARATOR, types);
        return applicationFrame.map(frame -> chain + " at " + frame).orElse(chain);
    }
}
