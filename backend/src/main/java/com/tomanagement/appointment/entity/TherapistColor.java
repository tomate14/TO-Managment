package com.tomanagement.appointment.entity;

/**
 * One of the four README colors, assigned to a user in a stable way.
 */
public enum TherapistColor {
    COLOR_1,
    COLOR_2,
    COLOR_3,
    COLOR_4;

    public static TherapistColor forUser(Long userId) {
        return values()[(int) (userId % values().length)];
    }

    public int getIndex() {
        return ordinal() + 1;
    }
}
