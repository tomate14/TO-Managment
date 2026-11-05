package com.tomanagement.appointment.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.tomanagement.appointment.dto.AppointmentResponse;
import com.tomanagement.appointment.dto.CreateAppointmentRequest;
import com.tomanagement.appointment.dto.RescheduleAppointmentRequest;
import com.tomanagement.appointment.dto.SlotResponse;
import com.tomanagement.appointment.entity.AppointmentDuration;
import com.tomanagement.appointment.service.AppointmentService;
import com.tomanagement.appointment.service.AvailabilityService;
import com.tomanagement.auth.security.AuthenticatedUser;
import com.tomanagement.common.exception.BusinessRuleException;
import com.tomanagement.common.exception.ErrorCode;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AvailabilityService availabilityService;

    public AppointmentController(AppointmentService appointmentService, AvailabilityService availabilityService) {
        this.appointmentService = appointmentService;
        this.availabilityService = availabilityService;
    }

    @GetMapping
    public List<AppointmentResponse> list(@AuthenticationPrincipal AuthenticatedUser actor,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Set<Long> therapistIds) {
        return appointmentService.list(actor, from, to, therapistIds);
    }

    @GetMapping("/availability")
    public List<SlotResponse> availability(@AuthenticationPrincipal AuthenticatedUser actor,
            @RequestParam Long therapistId, @RequestParam(required = false) Long patientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam int durationMinutes, @RequestParam(required = false) Long excludeAppointmentId) {
        AppointmentDuration duration = AppointmentDuration.fromMinutes(durationMinutes)
                .orElseThrow(() -> new BusinessRuleException(ErrorCode.APPOINTMENT_DURATION_INVALID));
        return availabilityService.slots(actor, therapistId, patientId, date, duration, excludeAppointmentId);
    }

    @GetMapping("/{id}")
    public AppointmentResponse get(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) {
        return appointmentService.get(actor, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse book(@AuthenticationPrincipal AuthenticatedUser actor,
            @Valid @RequestBody CreateAppointmentRequest request) {
        return appointmentService.book(actor, request);
    }

    @PutMapping("/{id}")
    public AppointmentResponse reschedule(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id,
            @Valid @RequestBody RescheduleAppointmentRequest request) {
        return appointmentService.reschedule(actor, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) {
        appointmentService.cancel(actor, id);
    }
}
