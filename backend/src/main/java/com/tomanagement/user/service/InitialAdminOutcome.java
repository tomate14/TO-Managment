package com.tomanagement.user.service;

import org.slf4j.event.Level;

/**
 * Result of the startup admin check, with the (data-free) log line it produces.
 */
public enum InitialAdminOutcome {
    CREATED(Level.INFO, "No había ningún administrador: se creó el admin inicial con los datos del .env."),
    ALREADY_PRESENT(Level.INFO, "Ya existe un administrador: no se crea el admin inicial."),
    EMAIL_IN_USE(Level.WARN, "No hay administradores, pero el email del admin inicial ya lo usa otro usuario: no se creó el admin.");

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
