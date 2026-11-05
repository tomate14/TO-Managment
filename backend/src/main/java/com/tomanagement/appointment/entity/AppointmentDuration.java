package com.tomanagement.appointment.entity;

import java.util.Arrays;
import java.util.Optional;

/**
 * The durations an appointment can have.
 */
public enum AppointmentDuration {
    MINUTES_30(30),
    MINUTES_45(45),
    MINUTES_60(60);

    private final int minutes;

    AppointmentDuration(int minutes) {
        this.minutes = minutes;
    }

    public int getMinutes() {
        return minutes;
    }

    public static Optional<AppointmentDuration> fromMinutes(int minutes) {
        return Arrays.stream(values()).filter(value -> value.minutes == minutes).findFirst();
    }
}
