package com.tomanagement.appointment.dto;

/**
 * Why a start time cannot be booked.
 */
public enum SlotUnavailableReason {
    PAST,
    THERAPIST_BUSY,
    PATIENT_BUSY
}
