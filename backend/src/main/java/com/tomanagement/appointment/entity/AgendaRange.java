package com.tomanagement.appointment.entity;

/**
 * Limits of an agenda listing.
 */
public enum AgendaRange {
    MAX_DAYS(42);

    private final int days;

    AgendaRange(int days) {
        this.days = days;
    }

    public int getDays() {
        return days;
    }
}
