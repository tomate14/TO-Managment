# É10 Trial Period Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Every establishment the superadmin approves or creates gets a 7-day trial (`TRIAL_DURATION_DAYS`); when it expires, its users are blocked at login and on every request until the superadmin extends or removes the expiry.

**Architecture:** One nullable column `establishment.trial_ends_at`. The `Establishment` entity owns the rules (start on first approval, change, expired check). The backend blocks expired establishments in `AuthServiceImpl.login` and in a new `TrialExpirationFilter` that runs after JWT authentication. The superadmin manages expiry with `PUT /api/establishments/{id}/trial`. The frontend adds a login banner, an interceptor rule, a "Prueba" column or line, and a trial dialog in Establecimientos.

**Tech Stack:** Java 21, Spring Boot 3.5, Spring Security, JPA/Flyway, Postgres (Supabase), JUnit 5 + Testcontainers. Angular 21 standalone components, signals, reactive forms, Vitest.

**Spec:** `docs/superpowers/specs/2026-11-07-trial-period-design.md`. Mockups: `docs/mockups/h10-prueba/` (README explains each PNG).

**Worktree / branch:** `C:\Users\Maxi\Desktop\TO Managment-trial`, branch `feat/trial-period` (from `feat/independent-therapists`).

## Global Constraints

- **Language:** UI copy in Spanish (exact strings below). Code, identifiers, commits and file names in English.
- **Backend design (CLAUDE.md):**
  - No `import *`, including `import static`.
  - Services are interface + `Impl`; inject by constructor.
  - Fixed domain values are enums (`ErrorCode`), never loose constants.
  - Throw only `ApplicationException` subclasses: `AccessDeniedApplicationException`, `BusinessRuleException`, `ResourceNotFoundException`.
- **Env variable:** `TRIAL_DURATION_DAYS` (value 7). In the same change, add it to:
  - `.env.example` (`TRIAL_DURATION_DAYS=7`);
  - `docker-compose.yml` (`${TRIAL_DURATION_DAYS:?TRIAL_DURATION_DAYS is required}`, next to `REGISTRATION_MAX_PER_HOUR`);
  - `application.yml` (`app.trial.duration-days`);
  - `application-test.yml` (`TRIAL_DURATION_DAYS: 7`);
  - `docs/cloud-run.md`.

  The real `.env` lives only in the main folder; the controller adds it there at merge time.
- **Schema:** new Flyway migration `V12__trial_period.sql`, mirrored idempotently in `supabase/init/01_schema.sql` in the same change. No backfill: existing rows keep `NULL`.
- **Exact copy:**
  - `ErrorCode.TRIAL_EXPIRED` → 403, "Tu período de prueba venció. Contactá al administrador de la plataforma."
  - `ErrorCode.TRIAL_END_IN_PAST` → 422, "La fecha de vencimiento no puede ser anterior a hoy."
  - Login banner: title "Tu período de prueba venció", message "Contactá al administrador de la plataforma."
  - Row texts:
    - mobile: "Venció el dd/mm/aaaa", "Prueba hasta el dd/mm/aaaa", "Sin vencimiento";
    - desktop "Prueba" column: badge "Vencida" + "el dd/mm/aaaa", "Hasta el dd/mm/aaaa", badge "Sin vencimiento".
  - Dialog:
    - title "Vencimiento de la prueba";
    - field "Último día de prueba";
    - hint "La cuenta se bloquea a la medianoche de ese día.";
    - buttons "Guardar", "Quitar vencimiento", "Cancelar".
- **Never log** names, emails or clinical data.
- **Verification:** compile-only by standing user instruction. Write the tests anyway; don't run test suites, except the runtime check in Task 6.
  - Backend compile (from `backend/`): `MSYS_NO_PATHCONV=1 docker run --rm -v "C:/Users/Maxi/Desktop/TO Managment-trial/backend:/app" -v to-management-m2:/root/.m2 -w /app maven:3.9-eclipse-temurin-21 ./mvnw -B -q test-compile`
  - Frontend compile (from `frontend/`): `npx ng build`, with no warnings, plus `npx tsc -p tsconfig.spec.json --noEmit`.
  - `frontend/node_modules` is a junction to the main folder: never `rm -rf` it.

## Review Focus

1. **Expiry boundary:** a request at exactly `trial_ends_at` is blocked, one second earlier is allowed. Pinned in Task 1 (`EstablishmentTest`).
2. **Superadmin inside an expired establishment** (token role SUPERADMIN, establishmentId set) must not be blocked by the filter. Pinned in Task 3 (`TrialExpirationTest`).
3. **Re-approving an approved establishment** (the idempotent POST) must not push the expiry forward, or an expired account could be revived by accident. Pinned in Task 1 and Task 2.
4. **The last day chosen is "today"** is valid and blocks at the next midnight in `APP_TIME_ZONE`. Yesterday is 422. Pinned in Task 2.
5. **A 403 from a different cause** (e.g. ACCESS_DENIED) must not log the user out. Only `TRIAL_EXPIRED` ends the session. Pinned in Task 4.

---

### Task 1: Column, entity rules and configuration

**Files:**
- Create: `backend/src/main/resources/db/migration/V12__trial_period.sql`
- Modify: `supabase/init/01_schema.sql` (after the `ck_establishment_kind` DO block, around line 38)
- Modify: `backend/src/main/java/com/tomanagement/establishment/entity/Establishment.java`
- Create: `backend/src/main/java/com/tomanagement/establishment/config/TrialProperties.java`
- Modify: `backend/src/main/resources/application.yml`, `backend/src/test/resources/application-test.yml`, `.env.example`, `docker-compose.yml`, `docs/cloud-run.md`
- Modify (callers of the old `approve(Instant)`): `backend/src/test/java/com/tomanagement/TestTenancy.java:34`, `backend/src/test/java/com/tomanagement/auth/controller/AuthControllerTest.java:86`, `backend/src/test/java/com/tomanagement/establishment/controller/EstablishmentControllerTest.java:267`, `EstablishmentCreatorImpl.java`, `EstablishmentServiceImpl.java` (the last two get a temporary `Duration.ofDays(7)`; Task 2 replaces it with `TrialProperties`)
- Test: `backend/src/test/java/com/tomanagement/establishment/entity/EstablishmentTest.java`

**Interfaces:**
- Produces:
  - `Establishment#approve(Instant when, Duration trial)`
  - `Establishment#changeTrialEnd(Instant endsAt)` (`endsAt` nullable)
  - `Establishment#isTrialExpired(Instant now)`
  - `Establishment#getTrialEndsAt()` (`Instant`, nullable)
  - `TrialProperties#trial()` (`Duration`), `TrialProperties#durationDays()` (`int`)

- [ ] **Step 1: Write the failing entity tests.** Replace the existing `approve(...)` calls in `EstablishmentTest`, and add the new tests:

```java
    private static final Duration TRIAL = Duration.ofDays(7);

    @Test
    void approvingSetsTheDateAndReactivates() {
        Establishment e = new Establishment("Ana Pérez", EstablishmentKind.INDIVIDUAL, NOW);
        e.changeActive(false);
        assertThat(e.isRejected()).isTrue();
        e.approve(NOW.plusSeconds(60), TRIAL);
        assertThat(e.getApprovedAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(e.isActive()).isTrue();
        assertThat(e.isPendingApproval()).isFalse();
    }

    @Test
    void approvingTwiceKeepsTheFirstDateAndTheFirstExpiry() {
        Establishment e = new Establishment("Ana Pérez", EstablishmentKind.INDIVIDUAL, NOW);
        e.approve(NOW, TRIAL);
        e.approve(NOW.plus(Duration.ofDays(3)), TRIAL);
        assertThat(e.getApprovedAt()).isEqualTo(NOW);
        assertThat(e.getTrialEndsAt()).isEqualTo(NOW.plus(TRIAL));
    }

    @Test
    void theFirstApprovalStartsTheTrial() {
        Establishment e = new Establishment("Ana Pérez", EstablishmentKind.INDIVIDUAL, NOW);
        assertThat(e.getTrialEndsAt()).isNull();
        e.approve(NOW, TRIAL);
        assertThat(e.getTrialEndsAt()).isEqualTo(NOW.plus(TRIAL));
    }

    @Test
    void reApprovingAfterRemovingTheExpiryKeepsItRemoved() {
        Establishment e = new Establishment("Centro", EstablishmentKind.INSTITUTION, NOW);
        e.approve(NOW, TRIAL);
        e.changeTrialEnd(null);
        e.approve(NOW.plusSeconds(60), TRIAL);
        assertThat(e.getTrialEndsAt()).isNull();
    }

    @Test
    void theTrialExpiresExactlyAtItsEnd() {
        Establishment e = new Establishment("Centro", EstablishmentKind.INSTITUTION, NOW);
        e.approve(NOW, TRIAL);
        Instant end = NOW.plus(TRIAL);
        assertThat(e.isTrialExpired(end.minusSeconds(1))).isFalse();
        assertThat(e.isTrialExpired(end)).isTrue();
    }

    @Test
    void withoutExpiryItNeverExpires() {
        Establishment e = new Establishment("Centro", EstablishmentKind.INSTITUTION, NOW);
        e.approve(NOW, TRIAL);
        e.changeTrialEnd(null);
        assertThat(e.isTrialExpired(NOW.plus(Duration.ofDays(3650)))).isFalse();
    }

    @Test
    void changingTheEndMovesIt() {
        Establishment e = new Establishment("Centro", EstablishmentKind.INSTITUTION, NOW);
        e.approve(NOW, TRIAL);
        e.changeTrialEnd(NOW.plus(Duration.ofDays(30)));
        assertThat(e.getTrialEndsAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
    }
```

  Also update `anIndividualPracticeIsBornPending` only if it calls `approve`; it doesn't. Add `import java.time.Duration;`.

- [ ] **Step 2: Implement the entity.** In `Establishment.java`, add after `approvedAt`:

```java
    @Column(name = "trial_ends_at")
    private Instant trialEndsAt;
```

  Replace `approve(Instant)` with:

```java
    /**
     * Approves the establishment. The first approval records the date and starts the trial; approving again keeps
     * both dates (and an expiry the superadmin removed stays removed). Approval always (re)activates.
     */
    public void approve(Instant when, Duration trial) {
        Objects.requireNonNull(when, "when is required");
        Objects.requireNonNull(trial, "trial is required");
        if (approvedAt == null) {
            approvedAt = when;
            trialEndsAt = when.plus(trial);
        }
        active = true;
    }

    /** Sets a new trial end; {@code null} removes the expiry (paid account). */
    public void changeTrialEnd(Instant endsAt) {
        this.trialEndsAt = endsAt;
    }

    public boolean isTrialExpired(Instant now) {
        return trialEndsAt != null && !now.isBefore(trialEndsAt);
    }

    public Instant getTrialEndsAt() {
        return trialEndsAt;
    }
```

  Ruling on the spec's "si `trialEndsAt` es null": the trial is set only on the first approval. An establishment that was approved, had its expiry removed, and is approved again stays without expiry, as the test pins.

- [ ] **Step 3: Migration and init script.** Create `V12__trial_period.sql`:

```sql
-- É10: trial period. NULL = no expiry; existing establishments stay without one.
ALTER TABLE establishment ADD COLUMN trial_ends_at timestamptz NULL;
```

  In `supabase/init/01_schema.sql`, after the `ck_establishment_kind` DO block:

```sql
ALTER TABLE establishment ADD COLUMN IF NOT EXISTS trial_ends_at TIMESTAMPTZ NULL;
```

- [ ] **Step 4: TrialProperties and env wiring.** Create `TrialProperties.java`. It is picked up by the existing `@ConfigurationPropertiesScan`:

```java
package com.tomanagement.establishment.config;

import java.time.Duration;

import jakarta.validation.constraints.Positive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Trial period settings (TRIAL_DURATION_DAYS in .env).
 */
@Validated
@ConfigurationProperties(prefix = "app.trial")
public record TrialProperties(@Positive int durationDays) {

    public Duration trial() {
        return Duration.ofDays(durationDays);
    }
}
```

  Then:
  - `application.yml`, under `app:` after `registration:`:

    ```yaml
      trial:
        duration-days: ${TRIAL_DURATION_DAYS}
    ```
  - `application-test.yml`: add `TRIAL_DURATION_DAYS: 7`.
  - `.env.example`: add `TRIAL_DURATION_DAYS=7`, with the comment line `# Días de prueba gratis desde que el superadmin aprueba o crea un establecimiento` next to `REGISTRATION_MAX_PER_HOUR`.
  - `docker-compose.yml`: in the backend environment, after line 85, add `TRIAL_DURATION_DAYS: ${TRIAL_DURATION_DAYS:?TRIAL_DURATION_DAYS is required}`.
  - `docs/cloud-run.md`: after line 16, add "Since É10, `TRIAL_DURATION_DAYS` is a required runtime variable (the backend does not start without it)."

- [ ] **Step 5: Fix the callers so everything compiles.**
  - `TestTenancy.create`: `establishment.approve(now, Duration.ofDays(7));`
  - `AuthControllerTest:86`: `north.approve(CREATED_AT, Duration.ofDays(7));` Then check that `CREATED_AT` plus 7 days is in the future. If `CREATED_AT` is a fixed past instant that would make the fixture expire, also call `north.changeTrialEnd(null);`, so existing login tests keep passing.
  - `EstablishmentControllerTest:267`: `establishment.approve(Instant.now(), Duration.ofDays(7));`
  - `EstablishmentCreatorImpl` and `EstablishmentServiceImpl.approve`: pass `Duration.ofDays(7)` for now (replaced in Task 2).
  - Add `import java.time.Duration;` wherever needed.

- [ ] **Step 6: Compile.** Run the backend compile command from Global Constraints. Expected: BUILD SUCCESS, no output.

- [ ] **Step 7: Commit.**

```bash
git add -A backend supabase .env.example docker-compose.yml docs/cloud-run.md
git commit -m "feat: trial_ends_at column and trial rules on Establishment (E10)"
```

---

### Task 2: Trial starts on approval or creation; superadmin manages the expiry

**Files:**
- Modify: `backend/src/main/java/com/tomanagement/establishment/service/EstablishmentCreatorImpl.java`
- Modify: `backend/src/main/java/com/tomanagement/establishment/service/EstablishmentService.java`, `EstablishmentServiceImpl.java`
- Modify: `backend/src/main/java/com/tomanagement/establishment/dto/EstablishmentSummaryResponse.java`
- Create: `backend/src/main/java/com/tomanagement/establishment/dto/ChangeTrialEndRequest.java`
- Modify: `backend/src/main/java/com/tomanagement/establishment/controller/EstablishmentController.java`
- Modify: `backend/src/main/java/com/tomanagement/common/exception/ErrorCode.java`
- Test: `backend/src/test/java/com/tomanagement/establishment/controller/EstablishmentControllerTest.java`

**Interfaces:**
- Consumes: Task 1 (`approve(Instant, Duration)`, `changeTrialEnd`, `isTrialExpired`, `getTrialEndsAt`, `TrialProperties`).
- Produces:
  - `EstablishmentSummaryResponse` with the new fields `Instant trialEndsAt`, `boolean trialExpired`, placed after `createdAt`. JSON: `"trialEndsAt": "2026-11-14T15:00:00Z" | null`, `"trialExpired": false`.
  - `PUT /api/establishments/{id}/trial`, body `{"trialEndsOn": "2026-11-30" | null}`, returning the summary.
  - `EstablishmentService#changeTrialEnd(Long id, LocalDate lastDay)`.
  - `ErrorCode.TRIAL_END_IN_PAST`.

- [ ] **Step 1: Write the failing integration tests** in `EstablishmentControllerTest`. Follow the file's existing helpers for the superadmin token and JSON requests; reuse whatever helper the existing approve test uses (read the file first).
  - `creatingAnEstablishmentStartsASevenDayTrial`: POST create as SUPERADMIN, then assert `jsonPath("$.trialEndsAt")` parses to `createdAt + 7 days` and `$.trialExpired` is false.
  - `approvingARequestStartsTheTrialAndApprovingAgainKeepsIt`:
    1. Create a pending INDIVIDUAL with `establishmentCreator.create(new NewEstablishment(..., EstablishmentKind.INDIVIDUAL, false, ...))`.
    2. POST `/{id}/approval`.
    3. Reload from `establishmentRepository`: `trialEndsAt == approvedAt + 7 days`.
    4. POST approval again: `trialEndsAt` unchanged.
  - `superadminSetsTheLastDayOfTrial`:
    1. PUT `/{id}/trial` with `{"trialEndsOn": "<today + 10 days>"}`, where today is `LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires"))`, the `APP_TIME_ZONE` of `application-test.yml`.
    2. Expect 200, and `trialEndsAt` equal to `today.plusDays(11).atStartOfDay(zone).toInstant()`.
  - `todayIsAValidLastDay`: PUT with today, expect 200 and `trialExpired` false.
  - `aPastLastDayIsRejected`: PUT with yesterday, expect 422 and `$.code == "TRIAL_END_IN_PAST"`.
  - `nullRemovesTheExpiry`: PUT `{"trialEndsOn": null}`, expect `$.trialEndsAt` to not exist or be null (`jsonPath("$.trialEndsAt").value(Matchers.nullValue())`, with an explicit import of `org.hamcrest.Matchers`).
  - `onlyTheSuperadminChangesTheTrial`: an ADMIN token on PUT `/{id}/trial` gets 403.
  - `unknownEstablishmentTrialIs404`: PUT `/999999/trial` gets 404 `ESTABLISHMENT_NOT_FOUND`.
  - `anExpiredEstablishmentIsFlaggedInTheList`:
    1. Save an approved establishment, then `changeTrialEnd(Instant.now().minusSeconds(60))` and save.
    2. GET the list: that row has `trialExpired == true`.

- [ ] **Step 2: Add the error code** in `ErrorCode.java`, after `ESTABLISHMENT_REQUIRED`:

```java
    TRIAL_END_IN_PAST(HttpStatus.UNPROCESSABLE_ENTITY, "La fecha de vencimiento no puede ser anterior a hoy."),
```

- [ ] **Step 3: Request DTO.**

```java
package com.tomanagement.establishment.dto;

import java.time.LocalDate;

/** Last day of trial chosen by the superadmin; {@code null} removes the expiry. */
public record ChangeTrialEndRequest(LocalDate trialEndsOn) {
}
```

- [ ] **Step 4: Summary fields.** Change the record to:

```java
public record EstablishmentSummaryResponse(Long id, String name, EstablishmentKind kind, boolean active,
        boolean pendingApproval, boolean rejected, Instant createdAt, Instant trialEndsAt, boolean trialExpired,
        long activeUsers, List<AdminRef> admins) {
```

  Fix every constructor call. `grep -rn "new EstablishmentSummaryResponse" backend/src` (main and test).

- [ ] **Step 5: Service.**
  - Inject `TrialProperties` into `EstablishmentServiceImpl` and `EstablishmentCreatorImpl`.
  - In the creator, take `Instant now = Instant.now(clock);` once, and use it for the constructor, `approve(now, trialProperties.trial())` and the user's `createdAt`.
  - In `EstablishmentServiceImpl`:

```java
    @Override
    @Transactional
    public EstablishmentSummaryResponse approve(Long id) {
        Establishment establishment = find(id);
        establishment.approve(Instant.now(clock), trialProperties.trial());
        return summarize(establishment);
    }

    @Override
    @Transactional
    public EstablishmentSummaryResponse changeTrialEnd(Long id, LocalDate lastDay) {
        Establishment establishment = find(id);
        if (lastDay == null) {
            establishment.changeTrialEnd(null);
        } else {
            if (lastDay.isBefore(LocalDate.now(clock))) {
                throw new BusinessRuleException(ErrorCode.TRIAL_END_IN_PAST);
            }
            // The chosen date is the last usable day: the account blocks at the next midnight in APP_TIME_ZONE.
            establishment.changeTrialEnd(lastDay.plusDays(1).atStartOfDay(clock.getZone()).toInstant());
        }
        return summarize(establishment);
    }
```

  - In `summarize`, pass `establishment.getTrialEndsAt(), establishment.isTrialExpired(Instant.now(clock))` after `getCreatedAt()`.
  - Add `EstablishmentSummaryResponse changeTrialEnd(Long id, LocalDate lastDay);` to the interface, with a Javadoc: "null removes the expiry; a day before today → TRIAL_END_IN_PAST".

- [ ] **Step 6: Controller.**

```java
    @PutMapping("/{id}/trial")
    public EstablishmentSummaryResponse changeTrialEnd(@PathVariable("id") Long id,
            @RequestBody ChangeTrialEndRequest request) {
        return establishmentService.changeTrialEnd(id, request.trialEndsOn());
    }
```

  The class is already `@PreAuthorize("hasRole('SUPERADMIN')")`.

- [ ] **Step 7: Compile** (backend command). Expected: success.

- [ ] **Step 8: Commit.**

```bash
git add -A backend
git commit -m "feat: start the trial on approval and let the superadmin change it (E10)"
```

---

### Task 3: Block expired establishments at login and on every request

**Files:**
- Modify: `backend/src/main/java/com/tomanagement/common/exception/ErrorCode.java`
- Modify: `backend/src/main/java/com/tomanagement/auth/service/AuthServiceImpl.java:57-70`, plus `AuthService.java` (Javadoc of `login`)
- Modify: `backend/src/main/java/com/tomanagement/establishment/repository/EstablishmentRepository.java`
- Create: `backend/src/main/java/com/tomanagement/establishment/service/TrialStatusChecker.java`, `TrialStatusCheckerImpl.java`
- Create: `backend/src/main/java/com/tomanagement/auth/security/TrialExpirationFilter.java`
- Modify: `backend/src/main/java/com/tomanagement/auth/config/SecurityConfig.java`
- Test: `backend/src/test/java/com/tomanagement/auth/controller/AuthControllerTest.java`
- Test: create `backend/src/test/java/com/tomanagement/auth/security/TrialExpirationTest.java`

**Interfaces:**
- Consumes: Task 1 (`isTrialExpired`, `changeTrialEnd`).
- Produces:
  - `ErrorCode.TRIAL_EXPIRED`;
  - `TrialStatusChecker#isExpired(Long establishmentId)`;
  - a 403 body `{"code":"TRIAL_EXPIRED",...}` on any authenticated request of an expired, non-superadmin user.

- [ ] **Step 1: Write the failing login tests** in `AuthControllerTest`, using the existing fixture style around line 86:
  - `anExpiredTrialCannotLogIn`:
    1. Set the user's establishment to `changeTrialEnd(Instant.now().minusSeconds(60))` and save.
    2. POST login: 403, `$.code == "TRIAL_EXPIRED"`.
  - `aRunningTrialLogsIn`: `changeTrialEnd(Instant.now().plus(Duration.ofDays(2)))`, then 200.
  - `noExpiryLogsIn`: `changeTrialEnd(null)`, then 200.
  - `inactiveWinsOverExpired`: inactive and expired gives `ESTABLISHMENT_INACTIVE`.

- [ ] **Step 2: Write the failing filter tests** in the new `TrialExpirationTest`:
  - Annotations: `@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Import({TestcontainersConfiguration.class, TestTables.class, TestTenancy.class})`. Copy the setup and cleanup pattern of `EstablishmentControllerTest`.
  - Tokens come from `tenancy.tokenFor(new AuthenticatedUser(userId, email, name, role, establishmentId))`. The user row must exist and be active for `/api/auth/me`; create it with `userRepository.save(User.builder()...)` as the existing tests do.
  - Tests:
    - `anOpenSessionOfAnExpiredEstablishmentIsCutOff`:
      1. Create an establishment with `tenancy.create("Vencido")`, plus an ADMIN user in it.
      2. Set the establishment to `changeTrialEnd(Instant.now().minusSeconds(60))` and save.
      3. GET `/api/auth/me` with the token: 403, `$.code == "TRIAL_EXPIRED"`.
    - `aRunningTrialPassesTheFilter`: same setup without the change (7 days ahead), so GET `/api/auth/me` gives 200.
    - `theSuperadminInsideAnExpiredEstablishmentIsNotBlocked`:
      1. A SUPERADMIN user with `establishmentId = null` in the DB.
      2. A token with `role SUPERADMIN, establishmentId = expired.getId()`.
      3. GET `/api/auth/me`: 200.
    - `aRequestWithoutTokenIsNotChecked`: GET `/api/auth/me` with no token gives 401 `AUTHENTICATION_REQUIRED`, not 403.

- [ ] **Step 3: Error code.** In `ErrorCode.java`, after `ESTABLISHMENT_PENDING_APPROVAL`:

```java
    TRIAL_EXPIRED(HttpStatus.FORBIDDEN, "Tu período de prueba venció. Contactá al administrador de la plataforma."),
```

- [ ] **Step 4: Login check.** In `AuthServiceImpl`, inject `Clock clock` through the constructor. The Clock bean exists in `ClockConfig`; update any test that builds `AuthServiceImpl` by hand: `grep -rn "new AuthServiceImpl" backend/src/test`. After the pending check:

```java
            if (establishment.isTrialExpired(Instant.now(clock))) {
                throw new AccessDeniedApplicationException(ErrorCode.TRIAL_EXPIRED);
            }
```

  Add "TRIAL_EXPIRED when its trial ended" to the `login` Javadoc in `AuthService`.

- [ ] **Step 5: Repository projection.**

```java
    /** Trial end of an establishment; empty when it has no expiry or does not exist. */
    @Query("select e.trialEndsAt from Establishment e where e.id = :id")
    Optional<Instant> findTrialEndsAtById(@Param("id") Long id);
```

  Imports: `java.time.Instant`, `java.util.Optional`, `org.springframework.data.jpa.repository.Query`, `org.springframework.data.repository.query.Param`.

- [ ] **Step 6: Checker (interface + impl).**

```java
package com.tomanagement.establishment.service;

/** Answers whether an establishment's trial has ended; used on every authenticated request. */
public interface TrialStatusChecker {

    boolean isExpired(Long establishmentId);
}
```

```java
package com.tomanagement.establishment.service;

import java.time.Clock;
import java.time.Instant;

import com.tomanagement.establishment.repository.EstablishmentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrialStatusCheckerImpl implements TrialStatusChecker {

    private final EstablishmentRepository establishmentRepository;
    private final Clock clock;

    public TrialStatusCheckerImpl(EstablishmentRepository establishmentRepository, Clock clock) {
        this.establishmentRepository = establishmentRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isExpired(Long establishmentId) {
        return establishmentRepository.findTrialEndsAtById(establishmentId)
                .map(end -> !Instant.now(clock).isBefore(end))
                .orElse(false);
    }
}
```

- [ ] **Step 7: Filter.** It is not a bean, like `JwtAuthenticationFilter`; SecurityConfig instantiates it.

```java
package com.tomanagement.auth.security;

import java.io.IOException;

import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.establishment.service.TrialStatusChecker;
import com.tomanagement.user.entity.Role;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Cuts off authenticated requests of an establishment whose trial ended (403 TRIAL_EXPIRED), so an open session
 * stops on its next action. The superadmin is never blocked, not even inside an expired establishment.
 * Not a Spring bean on purpose: SecurityConfig adds it right after JWT authentication.
 */
public class TrialExpirationFilter extends OncePerRequestFilter {

    private final TrialStatusChecker trialStatusChecker;
    private final ErrorResponseWriter errorResponseWriter;

    public TrialExpirationFilter(TrialStatusChecker trialStatusChecker, ErrorResponseWriter errorResponseWriter) {
        this.trialStatusChecker = trialStatusChecker;
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user
                && user.role() != Role.SUPERADMIN && user.establishmentId() != null
                && trialStatusChecker.isExpired(user.establishmentId())) {
            SecurityContextHolder.clearContext();
            errorResponseWriter.write(response, ErrorCode.TRIAL_EXPIRED);
            return;
        }
        chain.doFilter(request, response);
    }
}
```

- [ ] **Step 8: Register it** in `SecurityConfig.securityFilterChain`:
  - Add the parameter `TrialStatusChecker trialStatusChecker`.
  - After the existing `addFilterBefore(...)`, chain:

```java
                .addFilterAfter(new TrialExpirationFilter(trialStatusChecker, errorResponseWriter),
                        UsernamePasswordAuthenticationFilter.class);
```

  The JWT filter sits before `UsernamePasswordAuthenticationFilter` and this one sits after it, so the order is JWT → trial. Add the imports for `TrialExpirationFilter` and `TrialStatusChecker`.

- [ ] **Step 9: Compile** (backend command). Expected: success.

- [ ] **Step 10: Commit.**

```bash
git add -A backend
git commit -m "feat: block expired trials at login and on every request (E10)"
```

---

### Task 4: Frontend session end and login banner for an expired trial

**Files:**
- Modify: `frontend/src/app/core/auth/auth.models.ts` (around lines 26-29)
- Modify: `frontend/src/app/core/auth/auth.service.ts:78-81`
- Modify: `frontend/src/app/core/auth/auth.interceptor.ts`
- Modify: `frontend/src/app/features/auth/login-page/login-page.ts`, `login-page.html`
- Test: `frontend/src/app/core/auth/auth.interceptor.spec.ts`, `frontend/src/app/features/auth/login-page/login-page.spec.ts`, `frontend/src/app/core/auth/auth.service.spec.ts`

**Interfaces:**
- Produces:
  - `SessionEndReason` gains `'trialExpired'`;
  - `TRIAL_EXPIRED_QUERY = { prueba: 'vencida' } as const`;
  - `/login?prueba=vencida` opens with the expired-trial banner.

- [ ] **Step 1: Failing specs.**
  - `auth.interceptor.spec.ts`:

```ts
  it('ends the session when the trial expired', () => {
    vi.spyOn(auth, 'token').mockReturnValue('abc');
    const end = vi.spyOn(auth, 'endSession').mockImplementation(() => undefined);
    http.get('/api/patients').subscribe({ error: () => undefined });
    backend.expectOne('/api/patients').flush({ code: 'TRIAL_EXPIRED', message: 'x' }, { status: 403, statusText: 'Forbidden' });
    expect(end).toHaveBeenCalledWith('trialExpired');
  });

  it('keeps the session on any other 403', () => {
    vi.spyOn(auth, 'token').mockReturnValue('abc');
    const end = vi.spyOn(auth, 'endSession').mockImplementation(() => undefined);
    http.get('/api/users').subscribe({ error: () => undefined });
    backend.expectOne('/api/users').flush({ code: 'ACCESS_DENIED', message: 'x' }, { status: 403, statusText: 'Forbidden' });
    expect(end).not.toHaveBeenCalled();
  });
```

  - `auth.service.spec.ts`: `endSession('trialExpired')` navigates to `['/login']` with `{ queryParams: { prueba: 'vencida' } }`. Follow the file's existing `endSession('expired')` test.
  - `login-page.spec.ts`, following the existing 'explains a pending account' and icon tests:
    - `explains an expired trial`: flush a 403 `{ code: 'TRIAL_EXPIRED' }`; the banner text contains "Tu período de prueba venció" and "Contactá al administrador de la plataforma.", and it has the class `alert--error`.
    - `the expired-trial banner shows an hourglass icon`: the svg in the banner has `data-icon="hourglass"`. For this test, add `data-icon="clock"` / `"alert"` / `"hourglass"` attributes to the three svgs.
    - `opening /login?prueba=vencida shows the expired-trial banner`: provide `ActivatedRoute` with `queryParamMap` containing `prueba=vencida`, the way the existing `sesion=vencida` test does.

- [ ] **Step 2: Models and service.** In `auth.models.ts`:

```ts
export type SessionEndReason = 'expired' | 'invalid' | 'logout' | 'trialExpired';

/** Query params of /login that make it show the "Tu período de prueba venció" notice. */
export const TRIAL_EXPIRED_QUERY = { prueba: 'vencida' } as const;
```

  In `auth.service.ts` `endSession`:

```ts
  endSession(reason: SessionEndReason): void {
    this.clearSession();
    const queryParams = reason === 'expired' ? SESSION_EXPIRED_QUERY : reason === 'trialExpired' ? TRIAL_EXPIRED_QUERY : null;
    void this.router.navigate(['/login'], queryParams ? { queryParams } : {});
  }
```

- [ ] **Step 3: Interceptor.** Extend the `catchError`:

```ts
      if (sendsToken && error instanceof HttpErrorResponse && error.status === 403 && apiErrorCode(error) === TRIAL_EXPIRED_CODE) {
        auth.endSession('trialExpired');
      }
```

  Use `const TRIAL_EXPIRED_CODE = 'TRIAL_EXPIRED';` at the top. Update the doc comment to "…and on 403 TRIAL_EXPIRED (→ login with the trial notice)".

- [ ] **Step 4: Login page.**
  - `type LoginErrorKind = ... | 'trialExpired' | 'connection'`.
  - Add `icon: 'alert' | 'clock' | 'hourglass'` to `Banner`, replacing the implicit choice: pending gets `'clock'`, `trialExpired` gets `'hourglass'`, the rest get `'alert'`.
  - Banner entry:

```ts
  trialExpired: { title: 'Tu período de prueba venció', message: 'Contactá al administrador de la plataforma.', icon: 'hourglass' },
```

  - In `classify`, before `'connection'`: `if (apiErrorCode(err) === 'TRIAL_EXPIRED') { return 'trialExpired'; }`
  - Initial state from the query:

```ts
  private readonly route = inject(ActivatedRoute);
  protected readonly error = signal<LoginErrorKind | null>(
    this.route.snapshot.queryParamMap.get('prueba') === TRIAL_EXPIRED_QUERY.prueba ? 'trialExpired' : null,
  );
```

    Keep `sessionExpired` working; reuse `this.route` there too.
  - `login-page.html`: replace the warning-based svg choice with `@switch (banner(kind).icon)`, with the three svgs and their `data-icon` attributes. The hourglass path, from the mockup: `<path d="M6 3h12M6 21h12M7 3c0 5 10 5 10 9s-10 4-10 9M17 3c0 5-10 5-10 9"></path>`. Colour still comes from `warning` (amber) versus error (red); `trialExpired` is red.

- [ ] **Step 5: Compile** (frontend commands). Expected: no errors, no warnings.

- [ ] **Step 6: Commit.**

```bash
git add -A frontend/src
git commit -m "feat: expired-trial session end and login banner (E10)"
```

---

### Task 5: Establecimientos shows and edits the trial

**Files:**
- Modify: `frontend/src/app/features/establishments/establishments.models.ts`, `establishments.service.ts`
- Create: `frontend/src/app/features/establishments/trial-format.ts` (+ `trial-format.spec.ts`)
- Create: `frontend/src/app/features/establishments/trial-dialog/trial-dialog.ts`, `.html`, `.scss`, `.spec.ts`
- Modify: `frontend/src/app/features/establishments/establishments-page/establishments-page.ts`, `.html`, `.scss`, `.spec.ts`
- Test: `frontend/src/app/features/establishments/establishments.service.spec.ts`

**Interfaces:**
- Consumes: Task 2 JSON (`trialEndsAt: string | null`, `trialExpired: boolean`, `PUT /api/establishments/{id}/trial` with `{ trialEndsOn: string | null }`); `formatDate` and `todayIso` from `features/patients/patient-format`.
- Produces:
  - `EstablishmentsService#changeTrialEnd(id: number, trialEndsOn: string | null)`;
  - `trialLastDay(trialEndsAt: string): string` (yyyy-mm-dd, local);
  - `<app-trial-dialog>` with inputs `establishment`, `busy`, `error`, `desktop`, and outputs `save: string`, `remove: void`, `cancel: void`.

The mockups show only the new parts. Keep every existing control: rename, switch, users column and badges.

- [ ] **Step 1: Failing specs.**
  - `trial-format.spec.ts`:

```ts
import { trialLastDay } from './trial-format';

describe('trialLastDay', () => {
  it('is the day before the stored end (the account blocks at that midnight)', () => {
    const end = new Date(2026, 10, 15, 0, 0, 0).toISOString(); // local midnight starting 15/11
    expect(trialLastDay(end)).toBe('2026-11-14');
  });
});
```

  - `establishments.service.spec.ts`: `changeTrialEnd(4, '2026-11-30')` sends PUT `/api/establishments/4/trial` with body `{ trialEndsOn: '2026-11-30' }`; `changeTrialEnd(4, null)` sends `{ trialEndsOn: null }`.
  - `trial-dialog.spec.ts`:
    - It renders the title "Vencimiento de la prueba", the label "Último día de prueba" and the hint "La cuenta se bloquea a la medianoche de ese día.".
    - The date input has `min` = `todayIso()`.
    - It is pre-filled with `trialLastDay(trialEndsAt)` when the establishment has an expiry and that day is not in the past; otherwise it is empty.
    - "Guardar" with an empty date shows "Elegí una fecha." and does not emit.
    - "Guardar" with a date emits `save` with the yyyy-mm-dd.
    - "Quitar vencimiento" emits `remove`; "Cancelar" emits `cancel`.
    - When the input `error` is set, it shows under the field with `field__input--invalid`.
  - `establishments-page.spec.ts`, following the existing fixtures; add `trialEndsAt` and `trialExpired` to the fixture builder:
    - desktop: a "Prueba" column header; an expired row shows the "Vencida" badge and "el dd/mm/aaaa"; a running row shows "Hasta el dd/mm/aaaa"; a row with no expiry shows the "Sin vencimiento" badge;
    - mobile: the lines "Venció el …", "Prueba hasta el …" and "Sin vencimiento" under the name;
    - clicking "Vencimiento" (`aria-label` "Vencimiento de <name>") opens the dialog; saving calls `changeTrialEnd(id, date)`, replaces the row and closes the dialog;
    - a 422 `TRIAL_END_IN_PAST` keeps the dialog open with the message "La fecha de vencimiento no puede ser anterior a hoy."
    - "Quitar vencimiento" calls `changeTrialEnd(id, null)`.

- [ ] **Step 2: Models, service and format helper.**
  - In `EstablishmentSummary`:

```ts
  /** ISO instant when the trial ends (the account blocks), or null when it has no expiry. */
  trialEndsAt: string | null;
  trialExpired: boolean;
```

  - Add `trialEndPast: 'TRIAL_END_IN_PAST'` to `ESTABLISHMENT_ERROR_CODES`.
  - Service:

```ts
  changeTrialEnd(id: number, trialEndsOn: string | null): Observable<EstablishmentSummary> {
    return this.http.put<EstablishmentSummary>(`${ESTABLISHMENTS_URL}/${id}/trial`, { trialEndsOn });
  }
```

  - `trial-format.ts`:

```ts
import { todayIso } from '../patients/patient-format';

/** Last usable day (local yyyy-mm-dd) of a trial whose stored end is the following midnight. */
export function trialLastDay(trialEndsAt: string): string {
  return todayIso(new Date(Date.parse(trialEndsAt) - 1));
}
```

- [ ] **Step 3: Trial dialog component** (standalone, reactive form):
  - Desktop: a centered modal.
  - Mobile: a bottom sheet. The `desktop` input toggles the CSS modifier `trial-dialog--sheet`.
  - Both have an overlay; clicking it emits `cancel`. `role="dialog"`, `aria-modal="true"`, `aria-labelledby` the title.
  - Subtitle: "<name> · prueba hasta el dd/mm/aaaa", "<name> · venció el dd/mm/aaaa" or "<name> · sin vencimiento".
  - Field `<input type="date">` bound to a `FormControl<string>`, with `[min]="today"`.
  - Buttons:
    - desktop: "Quitar vencimiento" (red text button, left), "Cancelar", "Guardar" (primary);
    - mobile: "Guardar" (full width), "Quitar vencimiento" (outlined, red text), "Cancelar" (link).
  - Disable the buttons while `busy`. Mobile also shows the extra hint sentence from the mockup: "Si es una cuenta paga, quitá el vencimiento."
  - Styles: reuse the global classes (`field`, `field__input`, `field__error`, `alert`) and the colours in `docs/mockups/h10-prueba/source/mock.css` (`.modal`, `.sheet`, `.textbtn`).

- [ ] **Step 4: Page wiring.**
  - Signals: `trialEditing = signal<EstablishmentSummary | null>(null)` and `trialError = signal<string | null>(null)`.
  - Methods:
    - `openTrial(item)`;
    - `closeTrial()`;
    - `saveTrial(date: string)` and `removeTrial()`. Both call `changeTrialEnd`, use `setPending` for the row, and on success do `replace(updated, false)` and `closeTrial()`.
    - On a `TRIAL_END_IN_PAST` error, set `trialError` to `apiErrorMessage(err)`; on any other error, set it to `SAVE_ERROR`.
  - `trialLabel(item)` helper for the texts in Global Constraints. Use `formatDate(trialLastDay(item.trialEndsAt))` for "Prueba hasta el / Hasta el", and the same date for "Venció el / el", since the last day is the one shown.
  - Desktop table:
    - a new `<th scope="col">Prueba</th>` after "Estado", and its cell;
    - in the actions, a "Vencimiento" button (calendar icon from the mockup) before the edit button, with `aria-label="'Vencimiento de ' + item.name"`.
  - Mobile card: the trial line under the admin line, with the calendar icon and the `est-trial` class, and the "Vencimiento" button in `est-card__controls`.
  - Hide the trial UI for `pendingApproval` rows; they are in the pending section and have no trial yet.
  - Render `<app-trial-dialog>` when `trialEditing()` is set.

- [ ] **Step 5: Compile** (frontend commands). Expected: no errors, no warnings. Check the bundle-budget warning too.

- [ ] **Step 6: Commit.**

```bash
git add -A frontend/src
git commit -m "feat: show and edit the trial in Establecimientos (E10)"
```

---

### Task 6: Runtime check, merge to test, local smoke (controller)

Run by the controller after the final review, not by an implementer.

- [ ] **Step 1: Run the key integration classes once.** This is the one allowed exception to compile-only; in É9 it caught a backend that did not boot. From `backend/` in the worktree:

```bash
MSYS_NO_PATHCONV=1 docker run --rm -v "C:/Users/Maxi/Desktop/TO Managment-trial/backend:/app" -v to-management-m2:/root/.m2 -v //var/run/docker.sock:/var/run/docker.sock -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal -w /app maven:3.9-eclipse-temurin-21 ./mvnw -B -q test -Dtest='EstablishmentTest,AuthControllerTest,EstablishmentControllerTest,TrialExpirationTest,CrossEstablishmentIsolationTest' -Dsurefire.failIfNoSpecifiedTests=false
```

  Expected: all green in `target/surefire-reports/*.txt`.

- [ ] **Step 2: Init scripts.** Run `supabase/init/01_schema.sql` and `02_seed.sql` twice on an empty `postgres:15-alpine` (same procedure as É9). Expected: no errors.

- [ ] **Step 3: Merge to test.** In the main folder:
  1. Add `TRIAL_DURATION_DAYS=7` to `.env`.
  2. `git checkout test && git merge --no-ff feat/trial-period`.
  3. `docker compose up -d --build`.
  4. Confirm V12 applied and the backend started.

  Never `down -v`.

- [ ] **Step 4: Smoke.** With an approved establishment, set its trial end to the past in Studio, or through PUT with today then a DB edit. Check:
  - login gives 403 `TRIAL_EXPIRED`;
  - the superadmin list shows "Vencida";
  - PUT with a future date lets the user log in again.

## Branch flow

`feat/trial-period` → `test` for manual testing. It reaches `main` only after É9 is in `main`, and then only the feat branch, never `test`. Production needs `TRIAL_DURATION_DAYS` on Cloud Run.
