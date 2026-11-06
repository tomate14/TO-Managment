package com.tomanagement.user.service;

import org.slf4j.event.Level;

/**
 * Result of the startup admin check, with the (data-free) log line it produces.
 */
public enum InitialAdminOutcome {
    CREATED(Level.INFO, "No había ningún superadministrador: se creó el superadmin inicial con los datos del .env."),
    ALREADY_PRESENT(Level.INFO, "Ya existe un superadministrador: no se crea el superadmin inicial."),
    EMAIL_IN_USE(Level.WARN, "No hay superadministradores, pero el email del superadmin inicial ya lo usa otro usuario: no se creó el superadmin.");

    private final Level logLevel;
    private final String logMessage;

    InitialAdminOutcome(Level logLevel, String logMessage) {
        this.logLevel = logLevel;
        this.logMessage = logMessage;
    }

    public Level getLogLevel() {
        return logLevel;
    }

    public String getLogMessage() {
        return logMessage;
    }
}
