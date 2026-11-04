package com.tomanagement.user.service;

/**
 * Guarantees there is an administrator to create the other accounts (H1.3).
 */
public interface InitialAdminService {

    /**
     * Creates the initial admin from the .env values only when no ADMIN exists yet.
     */
    InitialAdminOutcome ensureInitialAdmin();
}
