package com.tomanagement.auth.controller;

import com.tomanagement.auth.dto.ChangeEstablishmentRequest;
import com.tomanagement.auth.dto.ChangePasswordRequest;
import com.tomanagement.auth.dto.LoginRequest;
import com.tomanagement.auth.dto.LoginResponse;
import com.tomanagement.auth.dto.UserSummary;
import com.tomanagement.auth.security.AuthenticatedUser;
import com.tomanagement.auth.service.AuthService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserSummary me(@AuthenticationPrincipal AuthenticatedUser user) {
        return authService.currentUser(user);
    }

    @PostMapping("/establishment")
    public LoginResponse changeEstablishment(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody ChangeEstablishmentRequest request) {
        return authService.changeEstablishment(user, request.establishmentId());
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(user.id(), request);
    }
}
