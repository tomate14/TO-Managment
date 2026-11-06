# É9 Terapeutas independientes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A therapist without an establishment can register from the login, wait for superadmin approval, and then work in her own one-person practice ("Mi consultorio"), isolated from everyone else.

**Architecture:** A practice is an `Establishment` of kind `INDIVIDUAL` created through the same path the superadmin uses (extracted into an `EstablishmentCreator`), but born pending (`approved_at NULL`). Tenant isolation from É8 is untouched. A public `POST /api/auth/register` (rate-limited per IP) creates it. Login rejects pending establishments, and the superadmin approves from the Establecimientos screen.

**Tech Stack:** Java 21, Spring Boot 3.5, Hibernate 6.6, Flyway, Postgres (Supabase), Angular 21 (standalone, signals, reactive forms), Testcontainers.

**Spec:** `docs/superpowers/specs/2026-11-06-independent-therapists-design.md` · Mockups: `docs/mockups/h9-independientes/`

## Global Constraints

- Worktree `C:\Users\Maxi\Desktop\TO Managment-independent`, branch `feat/independent-therapists`. Never commit `.superpowers/`. Never touch other branches, never merge, never push, and never `--amend`.
- **Verification is compile-only** (standing user instruction): write every test, then compile. Do NOT run test suites.
  - Backend: `cd backend && MSYS_NO_PATHCONV=1 docker run --rm -v "C:/Users/Maxi/Desktop/TO Managment-independent/backend:/app" -v to-management-m2:/root/.m2 -w /app maven:3.9-eclipse-temurin-21 ./mvnw -B -q test-compile`
  - Frontend: `cd frontend && npx ng build` (no warnings) and `npx tsc -p tsconfig.spec.json --noEmit` (exit 0). `frontend/node_modules` is a junction to the main folder's; never delete or modify it.
- **Backend rules (CLAUDE.md):**
  - No `import *` (also static).
  - Services are an interface plus an `Impl`, and callers depend on the interface.
  - Domain constants only as `enum`.
  - Throw only `ApplicationException` subclasses carrying an `ErrorCode`.
  - DTOs validated with `jakarta.validation`, with Spanish messages. DTOs carrying personal data mask `toString`.
  - Constructor injection. Name patterns in class names or in a short comment.
  - Never log personal data (email, name).
- **Every schema change** goes in a new Flyway migration AND `supabase/init/01_schema.sql` (idempotent) in the same change. Run both init scripts twice on an empty `postgres:15-alpine`.
- **New env var `REGISTRATION_MAX_PER_HOUR`**, in the same change:
  - `.env.example` with the value `5`;
  - the `docker-compose.yml` backend `environment` block, with `${REGISTRATION_MAX_PER_HOUR:?REGISTRATION_MAX_PER_HOUR is required}`;
  - `application.yml` as `app.registration.max-per-hour: ${REGISTRATION_MAX_PER_HOUR}`;
  - `src/test/resources/application-test.yml` with `REGISTRATION_MAX_PER_HOUR: 1000`.
  - The real `.env` is outside the worktree; the controller adds it at merge time.
- **UI:** copy in Spanish, identifiers and commits in English. Mobile-first, desktop from `min-width: 1024px`. Angular standalone components, signals, reactive forms, HTTP in services.
- **Exact copy:**
  - "Tu cuenta está pendiente de aprobación." (ErrorCode)
  - "Demasiados intentos. Probá de nuevo más tarde." (ErrorCode)
  - "Ya hay una cuenta con este email." (register email field)
  - "Las contraseñas no coinciden."
  - "Recibimos tu solicitud" / "Vas a poder ingresar cuando el administrador de la plataforma la apruebe."
  - "¿Sos terapeuta independiente? Creá tu cuenta"
  - "Mi consultorio"
  - "Terapeuta independiente"
  - "Pendientes de aprobación"
  - "Independiente"
  - "Rechazada"
  - "¿Rechazar la solicitud? No va a poder ingresar."
- Commit messages end with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Bash heredocs fail here, so use `-m` flags or `-F`.

## Review Focus

1. **A registration whose name equals an existing establishment's** (unique ignoring case) must not fail. It gets a suffix " (2)", " (3)"… capped at 120 characters (Task 3 test).
2. **Email in different case or with spaces** (" Ana@Gmail.com ") must be detected as the same account → 409 `USER_EMAIL_TAKEN` (Task 3 test).
3. **A rejected request that logs in** must get 403 `ESTABLISHMENT_INACTIVE`, not "pendiente" (Task 4 test).
4. **The superadmin approving an already approved, or inactive, establishment:** approval is idempotent and also reactivates (Task 2 test).
5. **Many registrations from the same IP behind Cloud Run:** the IP comes from the first `X-Forwarded-For` value, and the counter resets after one hour (Task 3 test with a fixed clock).

---

### Task 1: Schema and entity — kind and approval

**Files:**
- Create: `backend/src/main/resources/db/migration/V11__independent_therapists.sql`, `backend/src/main/java/com/tomanagement/establishment/entity/EstablishmentKind.java`
- Modify: `backend/src/main/java/com/tomanagement/establishment/entity/Establishment.java`, `supabase/init/01_schema.sql` (establishment table)
- Test: `backend/src/test/java/com/tomanagement/establishment/entity/EstablishmentTest.java` (unit), `backend/src/test/java/com/tomanagement/establishment/repository/EstablishmentRepositoryTest.java` (extend)

**Interfaces:**
- Produces:
  - `enum EstablishmentKind { INSTITUTION, INDIVIDUAL }`
  - `Establishment(String name, EstablishmentKind kind, Instant createdAt)`. The old 2-argument constructor is removed; update its callers to pass `INSTITUTION`.
  - `getKind()`, `getApprovedAt()`, `approve(Instant when)`, `isPendingApproval()` (approvedAt null and active), `isRejected()` (approvedAt null and not active).

- [ ] **Step 1: Failing tests**

```java
class EstablishmentTest {
    private static final Instant NOW = Instant.parse("2026-11-06T12:00:00Z");

    @Test
    void anIndividualPracticeIsBornPending() {
        Establishment e = new Establishment("Ana Pérez", EstablishmentKind.INDIVIDUAL, NOW);
        assertThat(e.isPendingApproval()).isTrue();
        assertThat(e.isRejected()).isFalse();
    }

    @Test
    void approvingSetsTheDateAndReactivates() {
        Establishment e = new Establishment("Ana Pérez", EstablishmentKind.INDIVIDUAL, NOW);
        e.changeActive(false);
        assertThat(e.isRejected()).isTrue();
        e.approve(NOW.plusSeconds(60));
        assertThat(e.getApprovedAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(e.isActive()).isTrue();
        assertThat(e.isPendingApproval()).isFalse();
    }

    @Test
    void approvingTwiceKeepsTheFirstDate() {
        Establishment e = new Establishment("Ana Pérez", EstablishmentKind.INDIVIDUAL, NOW);
        e.approve(NOW);
        e.approve(NOW.plusSeconds(60));
        assertThat(e.getApprovedAt()).isEqualTo(NOW);
    }
}
```

In `EstablishmentRepositoryTest`, add a case that saves an `INDIVIDUAL` establishment, then reads it back with `kind == INDIVIDUAL` and `approvedAt == null`.

- [ ] **Step 2: Compile and confirm the RED**

Run the backend compile command. Expected: compilation error, because `EstablishmentKind` does not exist.

- [ ] **Step 3: Migration and init script**

```sql
-- V11__independent_therapists.sql
ALTER TABLE ${app_schema}.establishment
    ADD COLUMN kind varchar(20) NOT NULL DEFAULT 'INSTITUTION',
    ADD COLUMN approved_at timestamptz NULL;
ALTER TABLE ${app_schema}.establishment
    ADD CONSTRAINT ck_establishment_kind CHECK (kind IN ('INSTITUTION', 'INDIVIDUAL'));
UPDATE ${app_schema}.establishment SET approved_at = created_at WHERE approved_at IS NULL;
```

Check how V10 references the schema (placeholder name) and use the same one. In `01_schema.sql`, add both columns and the CHECK to the `establishment` table, idempotently, the way the file already does (`ADD COLUMN IF NOT EXISTS`, plus a guarded `DO` block for the constraint).

- [ ] **Step 4: Entity**

```java
/** Who the establishment is for: a clinic with a team, or a one-person practice (É9). */
public enum EstablishmentKind { INSTITUTION, INDIVIDUAL }
```

```java
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 20)
private EstablishmentKind kind;

@Column(name = "approved_at")
private Instant approvedAt;

public Establishment(String name, EstablishmentKind kind, Instant createdAt) {
    this.name = Objects.requireNonNull(name, "name is required");
    this.kind = Objects.requireNonNull(kind, "kind is required");
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
}

/** Approves a pending practice; approving again keeps the first date. Approval always (re)activates. */
public void approve(Instant when) {
    if (approvedAt == null) {
        approvedAt = Objects.requireNonNull(when, "when is required");
    }
    active = true;
}

public boolean isPendingApproval() { return approvedAt == null && active; }
public boolean isRejected() { return approvedAt == null && !active; }
```

Update every caller of the old constructor (grep `new Establishment(`, in main and test) to pass `EstablishmentKind.INSTITUTION`. Where the superadmin creates one, approval comes in Task 2. Until then, test fixtures that must log in call `approve(now)`, or insert `approved_at`.

- [ ] **Step 5: Compile (GREEN) and run the init scripts twice on an empty Postgres** (see Global Constraints).

- [ ] **Step 6: Commit** — `feat: establishment kind and approval state (E9)`

---

### Task 2: Shared creation, approval endpoint and the superadmin summary

**Files:**
- Create:
  - `backend/src/main/java/com/tomanagement/establishment/service/EstablishmentCreator.java` (interface)
  - `EstablishmentCreatorImpl.java`
  - `establishment/service/NewEstablishment.java` (record)
- Modify:
  - `EstablishmentServiceImpl.java` (`create` delegates; new `approve`; summary fields; ordering)
  - `EstablishmentService.java`
  - `establishment/controller/EstablishmentController.java` (`POST /{id}/approval`)
  - `establishment/dto/EstablishmentSummaryResponse.java`
  - `establishment/dto/EstablishmentRef.java` (adds `kind`)
  - `establishment/repository/EstablishmentRepository.java` (if a query is needed)
- Test: `EstablishmentControllerTest` (extend), `EstablishmentCreatorImplTest` (integration)

**Interfaces:**
- Consumes: Task 1 entity API.
- Produces:
  - `record NewEstablishment(String name, EstablishmentKind kind, boolean approved, String adminFullName, String adminEmail, String adminPassword)`. `toString` masks the password and the email.
  - `interface EstablishmentCreator { Establishment create(NewEstablishment request); }`. It creates the establishment, the ADMIN user and the default catalogs in the caller's transaction. It throws `DuplicateResourceException(USER_EMAIL_TAKEN)` if the email exists, and maps races with `EstablishmentViolationMapper`. It does NOT check name uniqueness: the caller decides.
  - `EstablishmentSummaryResponse(Long id, String name, EstablishmentKind kind, boolean active, boolean pendingApproval, boolean rejected, Instant createdAt, long activeUsers, List<AdminRef> admins)`.
  - `EstablishmentRef(Long id, String name, EstablishmentKind kind)`.
  - `EstablishmentService.approve(Long id): EstablishmentSummaryResponse`.
  - `POST /api/establishments/{id}/approval` returns 200 with the summary. Only SUPERADMIN, the same as the rest of the controller.

- [ ] **Step 1: Failing tests** (in `EstablishmentControllerTest`, mirroring its existing style and tokens)
  - `superadminCreatesApprovedInstitutions`: POST `/api/establishments` → the response has `kind = "INSTITUTION"`, `pendingApproval = false`, `rejected = false`.
  - `approvalActivatesAPendingPractice`: insert an INDIVIDUAL pending establishment through `EstablishmentCreator` with `approved=false`. POST `/{id}/approval` → `pendingApproval = false`, `active = true`.
  - `approvalIsIdempotentAndReactivates`: approve, deactivate with PATCH `/{id}/active {active:false}`, approve again → `active = true`, `rejected = false`.
  - `approvalIsOnlyForTheSuperadmin`: an ADMIN token → 403.
  - `approvalOfAnUnknownEstablishmentIs404`: → `ESTABLISHMENT_NOT_FOUND`.
  - `pendingPracticesAreListedFirst`: an INSTITUTION "Aaa" and a pending INDIVIDUAL "Zzz" → `list[0].name == "Zzz"`.
  - `EstablishmentCreatorImplTest`: `create` with INDIVIDUAL and `approved=false` produces an establishment with `isPendingApproval()`, one ADMIN user and the 13 default catalog rows. The same email again throws `USER_EMAIL_TAKEN`.

- [ ] **Step 2: Compile (RED).**

- [ ] **Step 3: Implement**
  - Move the body of `EstablishmentServiceImpl.create` (save establishment, seed, save admin user, race mapping) into `EstablishmentCreatorImpl.create(NewEstablishment)`. When `approved`, call `establishment.approve(Instant.now(clock))` before `saveAndFlush`.
  - `EstablishmentServiceImpl.create` keeps its name check, then calls `creator.create(new NewEstablishment(request.name(), INSTITUTION, true, admin.fullName(), admin.email(), admin.password()))`.
  - `approve(id)`: find the establishment, call `approve(Instant.now(clock))`, return the summary.
  - `list()`: sort with `Comparator.comparing((Establishment e) -> !e.isPendingApproval()).thenComparing(Establishment::getName, String.CASE_INSENSITIVE_ORDER)`.
  - `summarize` fills the new fields.
  - `EstablishmentRef.from` copies `kind`.

- [ ] **Step 4: Compile (GREEN).** **Step 5: Commit** — `feat: shared establishment creation and superadmin approval (E9)`

---

### Task 3: Public registration with a per-IP limit

**Files:**
- Create:
  - `backend/src/main/java/com/tomanagement/registration/dto/RegisterTherapistRequest.java`
  - `registration/service/TherapistRegistrationService.java`, `TherapistRegistrationServiceImpl.java`
  - `registration/service/PracticeNameAllocator.java`
  - `registration/ratelimit/RegistrationRateLimiter.java` (interface), `FixedWindowRegistrationRateLimiter.java`
  - `registration/ratelimit/ClientIpResolver.java`
  - `registration/config/RegistrationProperties.java`
  - `registration/controller/RegistrationController.java`
  - `common/exception/TooManyRequestsException.java`
- Modify:
  - `auth/config/PublicEndpoint.java` (`REGISTER(HttpMethod.POST, "/api/auth/register")`)
  - `common/exception/ErrorCode.java` (`TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos. Probá de nuevo más tarde.")`)
  - `application.yml`, `src/test/resources/application-test.yml`, `.env.example`, `docker-compose.yml` (see Global Constraints)
- Test:
  - `registration/controller/RegistrationControllerTest.java` (SpringBootTest + Testcontainers, like `EstablishmentControllerTest`)
  - `registration/ratelimit/FixedWindowRegistrationRateLimiterTest.java` (unit)
  - `registration/service/PracticeNameAllocatorTest.java` (unit with a fake predicate)
  - `ClientIpResolverTest.java` (unit)

**Interfaces:**
- Consumes: `EstablishmentCreator`, `NewEstablishment`, `EstablishmentRepository.existsByNameIgnoreCase(String)`.
- Produces:
  - `POST /api/auth/register` with `{fullName, email, password}` → 201 with an empty body.
  - Errors: 409 `USER_EMAIL_TAKEN`, 400 `VALIDATION_ERROR`, 429 `TOO_MANY_REQUESTS`.

- [ ] **Step 1: Failing tests**

```java
// FixedWindowRegistrationRateLimiterTest
@Test
void blocksAfterTheMaximumAndResetsAfterAnHour() {
    MutableClock clock = new MutableClock(Instant.parse("2026-11-06T12:00:00Z"));
    RegistrationRateLimiter limiter = new FixedWindowRegistrationRateLimiter(new RegistrationProperties(2), clock);
    limiter.acquire("1.2.3.4");
    limiter.acquire("1.2.3.4");
    assertThatThrownBy(() -> limiter.acquire("1.2.3.4"))
            .isInstanceOf(TooManyRequestsException.class);
    limiter.acquire("5.6.7.8"); // other IPs are independent
    clock.advance(Duration.ofHours(1));
    limiter.acquire("1.2.3.4"); // new window
}
```

`MutableClock` is a small test helper (`extends Clock`). Create it under `src/test/java/com/tomanagement/support/` if none exists.

```java
// PracticeNameAllocatorTest
@Test
void usesTheNameWhenFree() {
    assertThat(PracticeNameAllocator.allocate("Ana Pérez", name -> false)).isEqualTo("Ana Pérez");
}
@Test
void addsTheFirstFreeSuffix() {
    Set<String> taken = Set.of("ana pérez", "ana pérez (2)");
    assertThat(PracticeNameAllocator.allocate("Ana Pérez", n -> taken.contains(n.toLowerCase(Locale.ROOT))))
            .isEqualTo("Ana Pérez (3)");
}
@Test
void keepsTheSuffixWithin120Characters() {
    String base = "x".repeat(120);
    String result = PracticeNameAllocator.allocate(base, n -> n.equals(base));
    assertThat(result).hasSizeLessThanOrEqualTo(120).endsWith(" (2)");
}

// ClientIpResolverTest
@Test
void prefersTheFirstForwardedAddress() {
    MockHttpServletRequest r = new MockHttpServletRequest();
    r.addHeader("X-Forwarded-For", "203.0.113.7, 10.0.0.1");
    r.setRemoteAddr("10.0.0.2");
    assertThat(new ClientIpResolver().resolve(r)).isEqualTo("203.0.113.7");
}
@Test
void fallsBackToTheRemoteAddress() {
    MockHttpServletRequest r = new MockHttpServletRequest();
    r.setRemoteAddr("10.0.0.2");
    assertThat(new ClientIpResolver().resolve(r)).isEqualTo("10.0.0.2");
}
```

`RegistrationControllerTest` cases:
- `registersAPendingIndividualPractice`: 201. Then the DB has an INDIVIDUAL pending establishment named "Ana Pérez", an ADMIN user with that email, and 5 body segments for that establishment.
- `normalizesTheEmailAndRejectsDuplicates`: register "ana@gmail.com", then " Ana@Gmail.com " → 409 `USER_EMAIL_TAKEN`.
- `aRepeatedNameGetsASuffix`: two registrations named "Ana Pérez" with different emails → the second establishment is "Ana Pérez (2)".
- `validatesTheBody`: an empty name or a 7-character password → 400 `VALIDATION_ERROR` with the field errors.
- `isReachableWithoutAToken`: no Authorization header → not 401.

- [ ] **Step 2: Compile (RED).**

- [ ] **Step 3: Implement**

```java
public record RegisterTherapistRequest(
        @NotBlank(message = "Ingresá tu nombre.") @Size(max = 120, message = "El nombre es demasiado largo.") String fullName,
        @NotBlank(message = "Ingresá tu email.") @Email(message = "Ingresá un email válido.")
        @Size(max = 254, message = "El email es demasiado largo.") String email,
        @NotNull(message = "Ingresá una contraseña.")
        @Size(min = 8, max = 72, message = "La contraseña tiene que tener entre 8 y 72 caracteres.") String password) {

    public RegisterTherapistRequest {
        email = EmailNormalizer.normalize(email);
        fullName = fullName == null ? null : fullName.trim();
    }

    @Override
    public String toString() {
        return "RegisterTherapistRequest[fullName=***, email=***, password=***]";
    }
}
```

```java
/** Picks a unique practice name: the therapist's name, or the first free "Name (n)" (n ≥ 2), within 120 chars. */
final class PracticeNameAllocator {
    private static final int MAX_LENGTH = 120; // mirrors the establishment.name column

    private PracticeNameAllocator() { }

    static String allocate(String base, Predicate<String> taken) {
        if (!taken.test(base)) {
            return base;
        }
        for (int n = 2; ; n++) {
            String suffix = " (" + n + ")";
            String candidate = base.substring(0, Math.min(base.length(), MAX_LENGTH - suffix.length())) + suffix;
            if (!taken.test(candidate)) {
                return candidate;
            }
        }
    }
}
```

Domain constants must be enums. If `MAX_LENGTH` trips that rule, put the 120 in an existing enum or a new `EstablishmentLimits` enum with the value as a field. Either is acceptable; say which in the report.

- `TherapistRegistrationServiceImpl.register(RegisterTherapistRequest)` is `@Transactional`:
  - `name = PracticeNameAllocator.allocate(fullName, establishmentRepository::existsByNameIgnoreCase)`
  - then `creator.create(new NewEstablishment(name, INDIVIDUAL, false, fullName, email, password))`.
- `RegistrationController`:
  - `@PostMapping("/api/auth/register") @ResponseStatus(CREATED)`.
  - Calls `rateLimiter.acquire(ipResolver.resolve(request))` **before** validating the body: inject `HttpServletRequest`, and take `@Valid @RequestBody` after the limiter. If Spring validates first, accept that and say so.
  - Then calls `registrationService.register(body)`.
- `FixedWindowRegistrationRateLimiter`:
  - `ConcurrentHashMap<String, Window>`, where `record Window(Instant start, int count)`, updated with `compute`.
  - A window older than 1 h starts over. Evict entries older than 1 h on each call, so the map stays bounded.
  - Throws `new TooManyRequestsException()`, which uses `ErrorCode.TOO_MANY_REQUESTS`.
- `RegistrationProperties`: `@ConfigurationProperties(prefix = "app.registration") record RegistrationProperties(int maxPerHour)`.
- `TooManyRequestsException extends ApplicationException`, mirroring `AccessDeniedApplicationException`. Check that `GlobalExceptionHandler` maps the code's `HttpStatus` generically; if not, add the case.

- [ ] **Step 4: Compile (GREEN).** **Step 5: Commit** — `feat: public registration for independent therapists (E9)`

---

### Task 4: Login blocks pending practices; isolation covers a practice

**Files:**
- Modify:
  - `auth/service/AuthServiceImpl.java` (login)
  - `common/exception/ErrorCode.java` (`ESTABLISHMENT_PENDING_APPROVAL(HttpStatus.FORBIDDEN, "Tu cuenta está pendiente de aprobación.")`)
- Test: `auth/controller/AuthControllerTest.java` (extend), `tenant/CrossEstablishmentIsolationTest.java` (extend)

**Interfaces:**
- Consumes: Task 1 `isPendingApproval()`; the Task 3 endpoint (for the isolation fixture).
- Produces: login 403 `ESTABLISHMENT_PENDING_APPROVAL`; `/me` and the login `user.establishment.kind`.

- [ ] **Step 1: Failing tests**
  - `AuthControllerTest`:
    - `aPendingPracticeCannotLogIn`: register through `/api/auth/register`, then log in → 403 with code `ESTABLISHMENT_PENDING_APPROVAL`.
    - `aRejectedPracticeSeesInactive`: register, then the superadmin PATCHes `/api/establishments/{id}/active {active:false}`, then log in → 403 `ESTABLISHMENT_INACTIVE`.
    - `anApprovedPracticeLogsInAndCarriesItsKind`: register, approve, log in → 200 with `user.establishment.kind == "INDIVIDUAL"` and `user.role == "ADMIN"`.
  - `CrossEstablishmentIsolationTest`: new test `anApprovedPracticeSeesOnlyItsOwnData`. Register and approve a practice, log in, then:
    - GET `/api/patients` → empty;
    - GET A's patient by id → 404 `PATIENT_NOT_FOUND`;
    - GET `/api/catalogs/body-segments` → 5 items, none of them with A's or B's ids;
    - GET `/api/users` → only herself.

- [ ] **Step 2: Compile (RED).**

- [ ] **Step 3: Implement.** In `login`, keep the inactive check first, then add:

```java
if (establishment.isPendingApproval()) {
    throw new AccessDeniedApplicationException(ErrorCode.ESTABLISHMENT_PENDING_APPROVAL);
}
```

A rejected practice is inactive, so it is caught by the existing check first. `changeEstablishment` (superadmin entering) keeps allowing an active pending practice, which is useful for support. No change there.

- [ ] **Step 4: Compile (GREEN).** **Step 5: Commit** — `feat: block login of pending practices (E9)`

---

### Task 5: Frontend — registration screen and login states

**Files:**
- Create: `frontend/src/app/features/auth/register-page/register-page.{ts,html,scss,spec.ts}`, `frontend/src/app/features/auth/registration.service.ts` (+ `.spec.ts`)
- Modify:
  - `frontend/src/app/app.routes.ts`: public lazy route `registro`, next to `login`, with the same guard `login` uses for already-logged-in users, if any.
  - `features/auth/login-page/login-page.{ts,html,spec.ts}`: register link; kinds `'pending'` and `'tooMany'`.

**Interfaces:**
- Consumes: `POST /api/auth/register {fullName, email, password}` → 201; errors `USER_EMAIL_TAKEN` (409), `VALIDATION_ERROR` (400), `TOO_MANY_REQUESTS` (429). `apiErrorCode` / `apiErrorMessage` from `core/api/api-error.ts`.
- Produces: `RegistrationService.register(payload: { fullName: string; email: string; password: string }): Observable<void>`.

- [ ] **Step 1: Failing specs**

```ts
it('blocks submit until the form is valid and the passwords match', () => { /* confirm ≠ password → error 'Las contraseñas no coinciden.', service not called */ });
it('trims name and email but not the password', () => { /* ' Ana ' → 'Ana' */ });
it('shows the email error on 409 USER_EMAIL_TAKEN', () => { /* text 'Ya hay una cuenta con este email.' under the email field */ });
it('shows the confirmation after 201', () => { /* 'Recibimos tu solicitud' and the email used; button 'Volver al inicio de sesión' → /login */ });
it('shows the too-many message on 429', () => { /* 'Demasiados intentos. Probá de nuevo más tarde.' */ });
// login-page.spec.ts
it('explains a pending account', () => { /* 403 ESTABLISHMENT_PENDING_APPROVAL → 'Tu cuenta está pendiente de aprobación' + detail */ });
it('links to the registration', () => { /* link text '¿Sos terapeuta independiente? Creá tu cuenta' → /registro */ });
```

- [ ] **Step 2–4:** type-check FAIL → implement per `mobile-registro.png`, `desktop-registro.png`, `mobile-solicitud-enviada.png` and `mobile-login-pendiente.png`. Reuse the login's layout pieces (`login-hero`, styles) so the register page shares the split layout. Then `ng build` and the type-check pass.
  - Validators: fullName required, notBlank, max 120; email required, email, max 254; password required, 8–72; confirm required plus a group validator for matching.
  - Pending banner copy: title "Tu cuenta está pendiente de aprobación", detail "Vas a poder ingresar cuando el administrador de la plataforma la apruebe.". Use the amber/warning variant if the login banners support variants; otherwise add one.
- [ ] **Step 5: Commit** — `feat: registration screen and pending login state (E9)`

---

### Task 6: Frontend — approvals in Establecimientos

**Files:**
- Modify:
  - `features/establishments/establishments.models.ts`: `EstablishmentSummary` adds `kind: 'INSTITUTION' | 'INDIVIDUAL'`, `pendingApproval: boolean`, `rejected: boolean`, `createdAt: string`.
  - `establishments.service.ts` (+ spec): `approve(id: number): Observable<EstablishmentSummary>` → `POST /api/establishments/{id}/approval`.
  - `establishments-page/*` (+ spec).
  - `layout/side-nav/*`: pending count badge for the superadmin outside, per `desktop-pendientes.png`. Mobile has no count.

**Interfaces:**
- Consumes: the Task 2 summary shape and `POST /{id}/approval`. Reject = the existing `PATCH /{id}/active {active:false}` (`changeActive`).
- Produces: the pending count. Keep it simple: side-nav reads it from a small signal service the page updates (`EstablishmentsService.pendingCount` signal set after `list()`). It must not trigger an extra request on every page.

- [ ] **Step 1: Failing specs**
  - `'renders the pending section only when there are pending practices'`: title "Pendientes de aprobación · 2", with each request's name, email and "pidió el dd/mm/aaaa".
  - `'approves a request and moves it to the list'`: the item leaves the section, and the list shows "Independiente" with Activo.
  - `'asks before rejecting'`: "Rechazar" shows "¿Rechazar la solicitud? No va a poder ingresar."; "Sí, rechazar" calls `changeActive(id, false)` and the item appears as "Rechazada"; "Cancelar" hides the confirmation.
  - `'shows the Independiente badge and the Rechazada state in the list'`.
  - `side-nav.spec`: `'shows the pending count next to Establecimientos'`.
- [ ] **Step 2–4:** type-check FAIL → implement per `mobile-pendientes.png` and `desktop-pendientes.png` (amber card `#FFFBF2` / border `#F2C46D`; rejected badge red) → `ng build` and the type-check pass. "Entrar" stays disabled for inactive establishments and is hidden for pending ones.
- [ ] **Step 5: Commit** — `feat: approve independent practices from Establecimientos (E9)`

---

### Task 7: Frontend — "Mi consultorio" for an individual practice

**Files:**
- Modify:
  - `core/auth/auth.models.ts`: `EstablishmentRef.kind: 'INSTITUTION' | 'INDIVIDUAL'`. Add the helper `isIndividualPractice(user)`. Update spec literals that build `EstablishmentRef`.
  - `layout/side-nav/*`, `features/more/more-page.*`, `layout/user-initials.ts` (role label override), `features/establishment/establishment-page/*` (title)

**Interfaces:**
- Consumes: the Task 4 `user.establishment.kind`.
- Produces: `isIndividualPractice(user: UserSummary | null | undefined): boolean`.

- [ ] **Step 1: Failing specs**
  - `side-nav`: for an INDIVIDUAL ADMIN, the establishment block label and the Administración item read "Mi consultorio", there is no "Usuarios" link, and the role reads "Terapeuta independiente".
  - `more-page`: the same, as on `mobile-mas-independiente.png`.
  - `establishment-page`: the title is "Mi consultorio" for INDIVIDUAL and "Establecimiento" otherwise.
  - An INSTITUTION ADMIN is unchanged (regression case).
- [ ] **Step 2–4:** type-check FAIL → implement per `desktop-independiente.png` and `mobile-mas-independiente.png` → `ng build` and the type-check pass. Do not block the `/usuarios` route (spec §6).
- [ ] **Step 5: Commit** — `feat: Mi consultorio navigation for independent therapists (E9)`

---

### Task 8: Final verification and merge into test

- [ ] Backend `test-compile` exits 0; frontend `ng build` has no warnings and the spec type-check exits 0; the init scripts run twice on an empty Postgres.
- [ ] `docs/superpowers/plans/2026-11-04-e0-pendientes.md`: add an É9 section with the out-of-scope items from spec §9, the in-memory per-instance rate limit, and the new `REGISTRATION_MAX_PER_HOUR` variable for Cloud Run.
- [ ] Controller: add `REGISTRATION_MAX_PER_HOUR=5` to the main folder's real `.env` (never commit it). Merge `--no-ff` into `test` from the main folder. Rebuild `docker compose up -d --build backend frontend` (V11 does not wipe data).
- [ ] Smoke:
  - register → 201;
  - login → 403 `ESTABLISHMENT_PENDING_APPROVAL`;
  - superadmin approves → login 200 with `kind INDIVIDUAL`;
  - she sees the 5 default segments.
- [ ] Only after the user approves on `test`: merge `feat/independent-therapists` into `main` (never `test`), then remove the worktree (junction first). Production needs `REGISTRATION_MAX_PER_HOUR` in Cloud Run, and É8's production deploy still needs a backup, explicit confirmation and a rotated `JWT_SECRET`.
