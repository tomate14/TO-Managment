package com.tomanagement.user.controller;

import java.util.List;

import com.tomanagement.auth.security.AuthenticatedUser;
import com.tomanagement.user.dto.ChangeUserActiveRequest;
import com.tomanagement.user.dto.CreateUserRequest;
import com.tomanagement.user.dto.UpdateUserRequest;
import com.tomanagement.user.dto.UserResponse;
import com.tomanagement.user.service.UserManagementService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * User management, admins only.
 */
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserManagementService userManagementService;

    public UserController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    public List<UserResponse> list() {
        return userManagementService.list();
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable("id") Long id) {
        return userManagementService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userManagementService.create(request);
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable("id") Long id, @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        return userManagementService.update(id, request, actor.id());
    }

    @PatchMapping("/{id}/active")
    public UserResponse changeActive(@PathVariable("id") Long id,
            @Valid @RequestBody ChangeUserActiveRequest request, @AuthenticationPrincipal AuthenticatedUser actor) {
        return userManagementService.changeActive(id, request.active(), actor.id());
    }
}
