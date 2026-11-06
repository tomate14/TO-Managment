package com.tomanagement.tenant;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Thread-local tenant override for tests and future system jobs. It is not used by the establishment seeder (which
 * writes through JDBC with an explicit establishment_id) and is only effective when it wraps the whole
 * transaction/session boundary, because the tenant is resolved when the session opens. The override is scoped to the
 * given work and the previous value is always restored, so it never leaks into later work on the same thread.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> OVERRIDE = new ThreadLocal<>();

    private TenantContext() {
        // Static utility.
    }

    public static void runAs(Long establishmentId, Runnable work) {
        Objects.requireNonNull(work, "work is required");
        callAs(establishmentId, () -> {
            work.run();
            return null;
        });
    }

    public static <T> T callAs(Long establishmentId, Supplier<T> work) {
        Objects.requireNonNull(establishmentId, "establishmentId is required");
        Objects.requireNonNull(work, "work is required");
        Long previous = OVERRIDE.get();
        OVERRIDE.set(establishmentId);
        try {
            return work.get();
        } finally {
            if (previous == null) {
                OVERRIDE.remove();
            } else {
                OVERRIDE.set(previous);
            }
        }
    }

    static Optional<Long> current() {
        return Optional.ofNullable(OVERRIDE.get());
    }
}
