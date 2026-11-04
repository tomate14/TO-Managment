package com.tomanagement.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import com.tomanagement.common.exception.ApplicationException;
import com.tomanagement.common.exception.BusinessRuleException;
import com.tomanagement.common.exception.DuplicateResourceException;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.common.exception.ResourceNotFoundException;
import com.tomanagement.user.dto.CreateUserRequest;
import com.tomanagement.user.dto.UpdateUserRequest;
import com.tomanagement.user.dto.UserResponse;
import com.tomanagement.user.entity.Role;
import com.tomanagement.user.entity.User;
import com.tomanagement.user.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class UserManagementServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-11-04T12:00:00Z");
    private static final Long ADMIN_ID = 1L;
    private static final Long THERAPIST_ID = 2L;

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UserManagementServiceImpl service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new UserManagementServiceImpl(userRepository, passwordEncoder, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void listsUsersOrderedByTheRepository() {
        when(userRepository.findAllByOrderByFullNameAsc())
                .thenReturn(List.of(user(ADMIN_ID, Role.ADMIN, true), user(THERAPIST_ID, Role.TERAPEUTA, false)));

        assertThat(service.list()).extracting(UserResponse::id, UserResponse::active)
                .containsExactly(tuple(ADMIN_ID, true), tuple(THERAPIST_ID, false));
    }

    @Test
    void createsAnActiveUserWithHashedPasswordAndNormalizedEmail() {
        when(userRepository.findByEmail("carla@consultorio.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("clave-inicial")).thenReturn("bcrypt-hash");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse created = service.create(
                new CreateUserRequest(" Carla@Consultorio.com ", " Carla Ríos ", "clave-inicial", Role.TERAPEUTA));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue()).satisfies(user -> {
            assertThat(user.getEmail()).isEqualTo("carla@consultorio.com");
            assertThat(user.getFullName()).isEqualTo("Carla Ríos");
            assertThat(user.getPasswordHash()).isEqualTo("bcrypt-hash");
            assertThat(user.getRole()).isEqualTo(Role.TERAPEUTA);
            assertThat(user.isActive()).isTrue();
            assertThat(user.getCreatedAt()).isEqualTo(NOW);
        });
        assertThat(created.active()).isTrue();
    }

    @Test
    void rejectsADuplicateEmail() {
        when(userRepository.findByEmail("carla@consultorio.com"))
                .thenReturn(Optional.of(user(THERAPIST_ID, Role.TERAPEUTA, true)));

        assertThatThrownBy(() -> service.create(
                new CreateUserRequest("carla@consultorio.com", "Carla", "clave-inicial", Role.TERAPEUTA)))
                .isInstanceOf(DuplicateResourceException.class)
                .extracting(error -> ((ApplicationException) error).getErrorCode())
                .isEqualTo(ErrorCode.USER_EMAIL_TAKEN);
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void translatesAUniqueIndexRaceIntoADuplicateEmail() {
        when(userRepository.findByEmail("carla@consultorio.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("clave-inicial")).thenReturn("bcrypt-hash");
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("dup"));

        assertThatThrownBy(() -> service.create(
                new CreateUserRequest("carla@consultorio.com", "Carla", "clave-inicial", Role.TERAPEUTA)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void updatesNameAndRoleOfAnotherUser() {
        User therapist = user(THERAPIST_ID, Role.TERAPEUTA, true);
        when(userRepository.findById(THERAPIST_ID)).thenReturn(Optional.of(therapist));

        UserResponse updated = service.update(THERAPIST_ID, new UpdateUserRequest("Carla R. Ríos", Role.ADMIN), ADMIN_ID);

        assertThat(updated.fullName()).isEqualTo("Carla R. Ríos");
        assertThat(updated.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void anAdminCanRenameHerselfButNotChangeHerOwnRole() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(user(ADMIN_ID, Role.ADMIN, true)));

        assertThat(service.update(ADMIN_ID, new UpdateUserRequest("Mariana G.", Role.ADMIN), ADMIN_ID).fullName())
                .isEqualTo("Mariana G.");
        assertThatThrownBy(() -> service.update(ADMIN_ID, new UpdateUserRequest("Mariana G.", Role.TERAPEUTA), ADMIN_ID))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(error -> ((ApplicationException) error).getErrorCode())
                .isEqualTo(ErrorCode.SELF_ROLE_CHANGE_FORBIDDEN);
    }

    @Test
    void deactivatesAndReactivatesAnotherUser() {
        User therapist = user(THERAPIST_ID, Role.TERAPEUTA, true);
        when(userRepository.findById(THERAPIST_ID)).thenReturn(Optional.of(therapist));

        assertThat(service.changeActive(THERAPIST_ID, false, ADMIN_ID).active()).isFalse();
        assertThat(service.changeActive(THERAPIST_ID, true, ADMIN_ID).active()).isTrue();
    }

    @Test
    void anAdminCannotDeactivateHerself() {
        User admin = user(ADMIN_ID, Role.ADMIN, true);
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.changeActive(ADMIN_ID, false, ADMIN_ID))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(error -> ((ApplicationException) error).getErrorCode())
                .isEqualTo(ErrorCode.SELF_DEACTIVATION_FORBIDDEN);
        assertThat(admin.isActive()).isTrue();
    }

    @Test
    void unknownUsersAreNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting(error -> ((ApplicationException) error).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void createRequestNeverPrintsThePassword() {
        assertThat(new CreateUserRequest("a@b.com", "Ana", "clave-inicial", Role.TERAPEUTA).toString())
                .doesNotContain("clave-inicial");
    }

    private static User user(Long id, Role role, boolean active) {
        User user = User.builder()
                .email("persona" + id + "@consultorio.com")
                .passwordHash("hash")
                .fullName("Persona " + id)
                .role(role)
                .active(active)
                .createdAt(NOW)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
