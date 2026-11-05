package com.tomanagement.appointment.entity;

/**
 * Appointments start every 30 minutes.
 */
public enum AgendaSlot {
    STEP(30);

    private final int minutes;

    AgendaSlot(int minutes) {
        this.minutes = minutes;
    }

    public int getMinutes() {
        return minutes;
    }
}
