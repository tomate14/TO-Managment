package com.tomanagement.appointment.dto;

import java.time.LocalTime;

/**
 * One candidate start time; {@code reason} is null when the slot is available.
 */
public record SlotResponse(LocalTime startTime, boolean available, SlotUnavailableReason reason) {
}
