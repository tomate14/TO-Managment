package com.tomanagement.appointment.entity;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Fixed agenda hours for everyone (spec section 2): Monday to Saturday, 08:00-20:00.
 */
public enum AgendaSchedule {
    OPENING(LocalTime.of(8, 0)),
    CLOSING(LocalTime.of(20, 0));

    private final LocalTime time;

    AgendaSchedule(LocalTime time) {
        this.time = time;
    }

    public LocalTime getTime() {
        return time;
    }

    public static boolean isOpenDay(DayOfWeek day) {
        return day != DayOfWeek.SUNDAY;
    }
}
