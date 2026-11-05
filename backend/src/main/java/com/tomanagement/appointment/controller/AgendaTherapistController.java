package com.tomanagement.appointment.controller;

import java.util.List;

import com.tomanagement.appointment.dto.AgendaTherapistResponse;
import com.tomanagement.appointment.service.AgendaTherapistService;
import com.tomanagement.auth.security.AuthenticatedUser;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agenda/therapists")
public class AgendaTherapistController {

    private final AgendaTherapistService service;

    public AgendaTherapistController(AgendaTherapistService service) {
        this.service = service;
    }

    @GetMapping
    public List<AgendaTherapistResponse> list(@AuthenticationPrincipal AuthenticatedUser actor) {
        return service.list(actor);
    }
}
