package com.tomanagement.appointment.validation;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.tomanagement.appointment.entity.AgendaSchedule;
import com.tomanagement.appointment.entity.AgendaSlot;
import com.tomanagement.common.exception.BusinessRuleException;
import com.tomanagement.common.exception.ErrorCode;

import org.springframework.stereotype.Component;

/**
 * Chain step: open day, slot-aligned start, inside the opening hours and not in the past.
 */
@Component
public class ScheduleStep extends AppointmentValidationStep {

    private final Clock clock;

    public ScheduleStep(Clock clock) {
        this.clock = clock;
    }

    @Override
    protected void check(AppointmentValidationContext context) {
        AppointmentDraft draft = context.draft();
        LocalDateTime start = draft.startAt();
        if (!isInsideAgenda(start, draft.endAt())) {
            throw new BusinessRuleException(ErrorCode.APPOINTMENT_OUTSIDE_HOURS);
        }
        if (start.isBefore(LocalDateTime.now(clock))) {
            throw new BusinessRuleException(ErrorCode.APPOINTMENT_IN_PAST);
        }
    }

    private boolean isInsideAgenda(LocalDateTime start, LocalDateTime end) {
        LocalTime startTime = start.toLocalTime();
        boolean alignedToSlot = startTime.getMinute() % AgendaSlot.STEP.getMinutes() == 0
                && startTime.getSecond() == 0 && startTime.getNano() == 0;
        return AgendaSchedule.isOpenDay(start.getDayOfWeek())
                && alignedToSlot
                && !startTime.isBefore(AgendaSchedule.OPENING.getTime())
                && end.toLocalDate().equals(start.toLocalDate())
                && !end.toLocalTime().isAfter(AgendaSchedule.CLOSING.getTime());
    }
}
