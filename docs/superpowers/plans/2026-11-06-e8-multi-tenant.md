# É8 Multi-tenencia por establecimiento — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the app multi-tenant: establishments with their own users and data, a platform SUPERADMIN that creates establishments and can enter any of them, and full data isolation between establishments.

**Architecture:** New `establishment` table and `establishment_id` on every data table (Flyway V10, which first empties the app tables). Hibernate 6 `@TenantId` on every data entity filters all HQL/Criteria queries by the tenant returned by a `CurrentTenantIdentifierResolver` that reads the JWT principal (or a thread-local `TenantContext` for system code and tests). Users are filtered explicitly (login searches by email without tenant). New establishment APIs for the superadmin and the admin; Angular gets the superadmin mode, the establishment banner/name and two new screens.

**Tech Stack:** Java 21, Spring Boot 3.5.6 (Hibernate 6.6), Flyway, Postgres 15, Testcontainers; Angular 21 standalone + signals, Vitest.

**Spec:** `docs/superpowers/specs/2026-11-06-multi-tenant-design.md` — Mockups: `docs/mockups/h8-establecimientos/` (README + PNGs + `source/*.html`).

## Global Constraints

- Branch `feat/multi-tenant` in the worktree `C:\Users\Maxi\Desktop\TO Managment-tenant` (from `main`). When done: merge into `test`; after approval, merge `feat/multi-tenant` into `main` (never `test`). Production deploy is a separate, explicitly confirmed step (V10 wipes the data; backup first).
- Verification is **compile-only** (standing user instruction): write every test, then backend `test-compile` in the Java 21 container (`cd backend && MSYS_NO_PATHCONV=1 docker run --rm -v "C:/Users/Maxi/Desktop/TO Managment-tenant/backend:/app" -v to-management-m2:/root/.m2 -w /app maven:3.9-eclipse-temurin-21 ./mvnw -B -q test-compile`) and frontend `npx ng build` (no warnings) + `npx tsc -p tsconfig.spec.json --noEmit`. The worktree's `frontend/node_modules` must be a junction to `C:\Users\Maxi\Desktop\TO Managment\frontend\node_modules` (create it with PowerShell `New-Item -ItemType Junction`; never delete through it).
- CLAUDE.md backend rules: no `import *`; services = interface + `Impl`; domain constants only as `enum`; only `ApplicationException` subclasses with an `ErrorCode`; DTOs with `jakarta.validation`; never log personal/clinical data; constructor injection; patterns named in class names or a short comment.
- Every schema change in a new Flyway migration **and** in `supabase/init/01_schema.sql` / `02_seed.sql` in the same task; run both init scripts twice on an empty `postgres:15-alpine`.
- UI copy Spanish; identifiers English; mobile-first with desktop from `min-width: 1024px`.
- Roles: `SUPERADMIN`, `ADMIN`, `TERAPEUTA`. SUPERADMIN has `establishment_id NULL`; ADMIN/TERAPEUTA always have one. Email unique globally; DNI unique per establishment; catalog names unique per establishment (ignoring case).
- Default catalogs copied into every new establishment (verbatim): segments Hombro, Codo, Muñeca, Mano, Dedos; surgery types Osteosíntesis con placa, Tenorrafia, Liberación del túnel carpiano, Artrodesis; scar types Normotrófica, Hipertrófica, Queloide, Retráctil.
- Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Never commit `.superpowers/`.

## Review Focus

- A request without an establishment in the token (superadmin outside, or a forged token) must never read or write tenant data: the resolver returns the `NONE` sentinel (`-1`), so reads come back empty and writes fail; data endpoints answer `ESTABLISHMENT_REQUIRED` (403) first — pinned in Task 3 (interceptor + resolver tests) and Task 5 (superadmin-outside integration test).
- Cross-establishment references by id (book with a therapist/patient of B, follow-up with B's scar type, diagnosis with B's surgery type, template of B's segment) must answer "not found" — pinned in Task 5's isolation suite.
- Same DNI / same catalog name in two establishments must be accepted; duplicates inside one establishment still rejected — pinned in Task 5.
- An inactive establishment blocks login of its users and blocks the superadmin's "Entrar" — pinned in Task 2 (login) and Task 4 (enter).
- The admin's users screen must not list or edit users of another establishment, nor create a SUPERADMIN — pinned in Task 5.

---

## File map

```
backend/src/main/resources/db/migration/V10__multi_tenant.sql
backend/src/main/java/com/tomanagement/
  establishment/entity/Establishment.java
  establishment/repository/EstablishmentRepository.java
  establishment/catalog/DefaultCatalogEntry.java, DefaultCatalogSeeder.java
  establishment/dto/{CreateEstablishmentRequest,EstablishmentAdminRequest,RenameEstablishmentRequest,
                     ChangeEstablishmentActiveRequest,EstablishmentSummaryResponse,CurrentEstablishmentResponse,EstablishmentRef}.java
  establishment/service/{EstablishmentService,EstablishmentServiceImpl,CurrentEstablishmentService,CurrentEstablishmentServiceImpl}.java
  establishment/controller/{EstablishmentController,CurrentEstablishmentController}.java
  tenant/{TenantSentinel,TenantContext,AuthenticatedTenantResolver,TenantHibernateConfig,CurrentEstablishmentProvider,
          SecurityContextEstablishmentProvider,EstablishmentRequiredInterceptor,TenantScopedPath,TenantWebConfig}.java
  auth/... (TokenClaim, AuthenticatedUser, JwtTokenProvider, AuthController, AuthServiceImpl, dto)
  user/... (Role, User, UserRepository, UserManagementServiceImpl, InitialAdminServiceImpl)
  every data entity (+ @TenantId), appointment services/visibility
frontend/src/app/
  core/auth/{auth.models,auth.service,auth.guards}.ts
  layout/{nav-items,user-initials}.ts, layout/side-nav, layout/tab-bar, layout/shell (+ establishment-banner/)
  features/establishments/ (superadmin list + form)
  features/establishment/ (admin page)
  features/more, features/users, features/agenda (role checks), app.routes.ts
supabase/init/01_schema.sql, 02_seed.sql
```

---

### Task 1: Schema V10, Establishment entity, SUPERADMIN role, user establishment

**Files:**
- Create: `db/migration/V10__multi_tenant.sql`, `establishment/entity/Establishment.java`, `establishment/repository/EstablishmentRepository.java`
- Modify: `user/entity/Role.java`, `user/entity/User.java` (+ builder), `user/repository/UserRepository.java`, `supabase/init/01_schema.sql`, `supabase/init/02_seed.sql`, `backend/src/test/java/com/tomanagement/TestTable.java`
- Test: `backend/src/test/java/com/tomanagement/establishment/repository/EstablishmentRepositoryTest.java`

**Interfaces:**
- Produces: `Establishment(String name, Instant createdAt)`; `getId()`, `getName()`, `isActive()`, `getCreatedAt()`, `rename(String)`, `changeActive(boolean)`. `EstablishmentRepository extends JpaRepository<Establishment, Long>`: `boolean existsByNameIgnoreCase(String)`, `boolean existsByNameIgnoreCaseAndIdNot(String, Long)`, `List<Establishment> findAllByOrderByNameAsc()`.
- Produces: `Role { SUPERADMIN, ADMIN, TERAPEUTA }`; `User.getEstablishmentId(): Long` (null for SUPERADMIN), builder `.establishmentId(Long)`; `UserRepository`: `List<User> findAllByEstablishmentIdOrderByFullNameAsc(Long)`, `long countByEstablishmentIdAndActiveTrue(Long)`, `List<User> findByEstablishmentIdAndRoleOrderByFullNameAsc(Long, Role)`.
- Produces: `TestTable` adds `ESTABLISHMENT("establishment")` and `APP_USER("app_user")`.

- [ ] **Step 1: Write the failing test** (setup copied from `AppointmentRepositoryTest`: `@SpringBootTest` + `TestcontainersConfiguration` + `TestTables`):
```java
@Test void establishmentNamesAreUniqueIgnoringCase() {
    establishmentRepository.saveAndFlush(new Establishment("Consultorio Norte", Instant.now()));
    assertThat(establishmentRepository.existsByNameIgnoreCase("consultorio norte")).isTrue();
    assertThatThrownBy(() -> establishmentRepository.saveAndFlush(new Establishment("CONSULTORIO NORTE", Instant.now())))
            .isInstanceOf(DataIntegrityViolationException.class);
}
@Test void superadminHasNoEstablishmentAndOthersMustHaveOne() {
    Establishment norte = establishmentRepository.saveAndFlush(new Establishment("Norte", Instant.now()));
    userRepository.saveAndFlush(user("root@x.com", Role.SUPERADMIN, null));
    userRepository.saveAndFlush(user("ana@x.com", Role.ADMIN, norte.getId()));
    assertThatThrownBy(() -> userRepository.saveAndFlush(user("bad@x.com", Role.TERAPEUTA, null)))
            .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> userRepository.saveAndFlush(user("bad2@x.com", Role.SUPERADMIN, norte.getId())))
            .isInstanceOf(DataIntegrityViolationException.class);
}
```
- [ ] **Step 2: Compile** (expect FAIL).
- [ ] **Step 3: Implement.** `V10__multi_tenant.sql`:
```sql
-- É8: multi-tenancy by establishment. Starts from an empty database (decided with the user).
TRUNCATE TABLE answer, segment_evaluation, follow_up, appointment, diagnosis, surgical_treatment, conservative_treatment,
    treatment, patient, question_option, question, evaluation_template, body_segment, surgery_type, scar_type, app_user
    RESTART IDENTITY CASCADE;

CREATE TABLE establishment (
    id         BIGSERIAL    PRIMARY KEY,
    name       VARCHAR(120) NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL
);
CREATE UNIQUE INDEX ux_establishment_name_lower ON establishment (lower(name));

ALTER TABLE app_user ADD COLUMN establishment_id BIGINT REFERENCES establishment (id);
ALTER TABLE app_user DROP CONSTRAINT app_user_role_check;
ALTER TABLE app_user ADD CONSTRAINT app_user_role_check CHECK (role IN ('SUPERADMIN', 'ADMIN', 'TERAPEUTA'));
ALTER TABLE app_user ADD CONSTRAINT ck_app_user_establishment CHECK ((role = 'SUPERADMIN') = (establishment_id IS NULL));
CREATE INDEX ix_app_user_establishment ON app_user (establishment_id);

-- every data table gets a NOT NULL establishment (tables are empty after the TRUNCATE above)
ALTER TABLE body_segment        ADD COLUMN establishment_id BIGINT NOT NULL REFERENCES establishment (id);
ALTER TABLE surgery_type        ADD COLUMN establishment_id BIGINT NOT NULL REFERENCES establishment (id);
ALTER TABLE scar_type           ADD COLUMN establishment_id BIGINT NOT NULL REFERENCES establishment (id);
ALTER TABLE evaluation_template ADD COLUMN establishment_id BIGINT NOT NULL REFERENCES establishment (id);
ALTER TABLE question            ADD COLUMN establishment_id BIGINT NOT NULL REFERENCES establishment (id);
ALTER TABLE question_option     ADD COLUMN establishment_id BIGINT NOT NULL REFERENCES establishment (id);
ALTER TABLE patient             ADD COLUMN establishment_id BIGINT NOT NULL REFERENCES establishment (id);
ALTER TABLE treatment           ADD COLUMN establishment_id BIGINT NOT NULL REFERENCES establishment (id);
ALTER TABLE diagnosis           ADD COLUMN establishment_id BIGINT NOT NULL REFERENCES establishment (id);
ALTER TABLE follow_up           ADD COLUMN establishment_id BIGINT NOT NULL REFERENCES establishment (id);
ALTER TABLE segment_evaluation  ADD COLUMN establishment_id BIGINT NOT NULL REFERENCES establishment (id);
ALTER TABLE answer              ADD COLUMN establishment_id BIGINT NOT NULL REFERENCES establishment (id);
ALTER TABLE appointment         ADD COLUMN establishment_id BIGINT NOT NULL REFERENCES establishment (id);

DROP INDEX ux_surgery_type_name_lower;
DROP INDEX ux_scar_type_name_lower;
DROP INDEX ux_body_segment_name_lower;
DROP INDEX ux_patient_dni;
CREATE UNIQUE INDEX ux_surgery_type_name_lower ON surgery_type (establishment_id, lower(name));
CREATE UNIQUE INDEX ux_scar_type_name_lower    ON scar_type (establishment_id, lower(name));
CREATE UNIQUE INDEX ux_body_segment_name_lower ON body_segment (establishment_id, lower(name));
CREATE UNIQUE INDEX ux_patient_dni             ON patient (establishment_id, dni);
CREATE INDEX ix_evaluation_template_establishment ON evaluation_template (establishment_id);
CREATE INDEX ix_appointment_establishment_start   ON appointment (establishment_id, start_at);
```
(Before writing the `DROP CONSTRAINT`, confirm the generated name of V2's inline role CHECK with `psql \d app_user` on the init-script Postgres; Postgres names it `app_user_role_check`.) Mirror the final state in `01_schema.sql`: add `establishment` before `app_user`, put `establishment_id` columns and the new indexes directly in each `CREATE TABLE IF NOT EXISTS`, replace the old global unique indexes, update the role CHECK and add `ck_app_user_establishment`; header "V1..V10". `02_seed.sql`: remove the catalog `INSERT`s and add `-- (É8: los catálogos se crean por establecimiento desde el backend; no hay datos globales.)`.

`Establishment` entity (`@Table(name = "establishment")`, name length 120, `active`, `createdAt` `Instant` `updatable=false`). `User`: `@Column(name = "establishment_id") private Long establishmentId;` + builder method + getter. `Role` adds `SUPERADMIN` first.
- [ ] **Step 4: Compile** (SUCCESS) and run the init scripts twice on `postgres:15-alpine` (expect 17 tables, no errors).
- [ ] **Step 5: Commit** — `feat: add establishments and the superadmin role (E8)`

---

### Task 2: Establishment in the token, login and /me

**Files:**
- Modify: `auth/security/TokenClaim.java`, `auth/security/AuthenticatedUser.java`, `auth/security/JwtTokenProvider.java`, `auth/dto/UserSummary.java`, `auth/dto/LoginResponse.java` (unchanged shape, new `UserSummary`), `auth/service/AuthService.java`, `auth/service/AuthServiceImpl.java`, `auth/controller/AuthController.java`, `common/exception/ErrorCode.java`
- Create: `auth/dto/ChangeEstablishmentRequest.java`, `establishment/dto/EstablishmentRef.java`
- Modify (mechanical): **every** `new AuthenticatedUser(...)` in `backend/src/test/java` gets the 5th argument (`null` where the test is not tenant-aware yet; Task 3 gives the integration tests a real establishment).
- Test: `auth/security/JwtTokenProviderTest.java` (extend), `auth/controller/AuthControllerTest.java` (extend)

**Interfaces:**
- Produces: `record AuthenticatedUser(Long id, String email, String fullName, Role role, Long establishmentId)`; `TokenClaim.ESTABLISHMENT("est")` (absent when null); `record EstablishmentRef(Long id, String name)`; `record UserSummary(Long id, String email, String fullName, Role role, EstablishmentRef establishment)` with `static UserSummary of(User user, EstablishmentRef establishment)`.
- Produces: `AuthService.changeEstablishment(AuthenticatedUser actor, Long establishmentId): LoginResponse` (SUPERADMIN only; `null` = leave); `POST /api/auth/establishment` body `ChangeEstablishmentRequest(Long establishmentId)`.
- ErrorCodes (add before `INTERNAL_ERROR`):
```java
ESTABLISHMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "El establecimiento no existe."),
ESTABLISHMENT_NAME_TAKEN(HttpStatus.CONFLICT, "Ya hay un establecimiento con ese nombre."),
ESTABLISHMENT_INACTIVE(HttpStatus.FORBIDDEN, "El establecimiento está inactivo."),
ESTABLISHMENT_REQUIRED(HttpStatus.FORBIDDEN, "Elegí un establecimiento para continuar."),
```

- [ ] **Step 1: Failing tests**
```java
// JwtTokenProviderTest
@Test void carriesTheEstablishment() {
    AuthenticatedUser parsed = provider.parse(provider.issue(new AuthenticatedUser(7L, "a@x.com", "Ana", Role.ADMIN, 3L)).value());
    assertThat(parsed.establishmentId()).isEqualTo(3L);
}
@Test void superadminOutsideHasNoEstablishment() {
    assertThat(provider.parse(provider.issue(new AuthenticatedUser(1L, "r@x.com", "Root", Role.SUPERADMIN, null)).value())
            .establishmentId()).isNull();
}
// AuthControllerTest (real users via builder + an establishment)
@Test void loginReturnsTheEstablishment() { /* POST /api/auth/login → $.user.establishment.name == "Consultorio Norte" */ }
@Test void usersOfAnInactiveEstablishmentCannotLogIn() { /* establishment.changeActive(false) → 403, code ESTABLISHMENT_INACTIVE */ }
@Test void superadminEntersAndLeavesAnEstablishment() {
    // login as SUPERADMIN → $.user.establishment null; POST /api/auth/establishment {establishmentId: norte} → new token,
    // $.user.establishment.id == norte; with {establishmentId: null} → establishment null
}
@Test void onlyTheSuperadminChangesEstablishment() { /* ADMIN → 403 ACCESS_DENIED */ }
@Test void superadminCannotEnterAnInactiveEstablishment() { /* 403 ESTABLISHMENT_INACTIVE */ }
```
- [ ] **Step 2: Compile** (FAIL). **Step 3: Implement** — `JwtTokenProvider.issue` adds `.claim(TokenClaim.ESTABLISHMENT.getName(), establishmentId)` only when non-null; `parse` reads it as `Long` (`claims.get(name, Long.class)`; numbers may parse as `Integer` → use `Number::longValue`). `AuthServiceImpl.login`: after the password check, if `user.getEstablishmentId() != null` load the establishment and reject inactive with `ESTABLISHMENT_INACTIVE` (use the existing exception for 403: `AccessDeniedApplicationException(ErrorCode)`); token built with `user.getEstablishmentId()`. `currentUser` returns the `EstablishmentRef` of the **token's** establishment (so the superadmin inside sees the entered one). `changeEstablishment`: actor must be SUPERADMIN (else `ACCESS_DENIED`); non-null id → must exist (`ESTABLISHMENT_NOT_FOUND`) and be active (`ESTABLISHMENT_INACTIVE`); returns a fresh `LoginResponse`.
- [ ] **Step 4: Compile** (SUCCESS). **Step 5: Commit** — `feat: carry the establishment in the session (E8)`

---

### Task 3: Tenant filter with Hibernate `@TenantId` and the establishment guard

**Files:**
- Create: `tenant/TenantSentinel.java`, `tenant/TenantContext.java`, `tenant/AuthenticatedTenantResolver.java`, `tenant/TenantHibernateConfig.java`, `tenant/CurrentEstablishmentProvider.java`, `tenant/SecurityContextEstablishmentProvider.java`, `tenant/TenantScopedPath.java`, `tenant/EstablishmentRequiredInterceptor.java`, `tenant/TenantWebConfig.java`
- Modify: every data entity — `BodySegment`, `SurgeryType`, `ScarType` (put the field in `CatalogItem` `@MappedSuperclass`), `EvaluationTemplate`, `Question`, `QuestionOption`, `Patient`, `Treatment` (base class only), `Diagnosis`, `FollowUp`, `SegmentEvaluation`, `Answer`, `Appointment`: add
```java
@TenantId
@Column(name = "establishment_id", nullable = false, updatable = false)
private Long establishmentId;
```
- Modify (tests): all integration tests that create data through repositories wrap setup in `TenantContext.runAs(establishmentId, () -> ...)` (or set/clear it in `@BeforeEach`/`@AfterEach`) and issue tokens with that establishment. Create `backend/src/test/java/com/tomanagement/TestTenancy.java` helper: `Establishment create(String name)` (saves via `EstablishmentRepository`), `String tokenFor(AuthenticatedUser)`, `void runAs(Long establishmentId, Runnable work)`.
- Test: `tenant/AuthenticatedTenantResolverTest.java`, `tenant/EstablishmentRequiredInterceptorTest.java` (MockMvc), `tenant/TenantIsolationRepositoryTest.java`

**Interfaces:**
```java
/** Tenant ids that are not real establishments. NONE is used when no establishment is in scope: matches no row and fails FK on insert. */
public enum TenantSentinel { NONE(-1L); private final Long id; ... public Long getId() }

/** Thread-local tenant override for system work (seeding) and tests; always restored. */
public final class TenantContext {
    public static void runAs(Long establishmentId, Runnable work);
    public static <T> T callAs(Long establishmentId, Supplier<T> work);
    static Optional<Long> current();
}

/** Hibernate CurrentTenantIdentifierResolver<Long>: TenantContext override, else the JWT principal's establishment, else NONE. */
@Component public class AuthenticatedTenantResolver implements CurrentTenantIdentifierResolver<Long> {
    public Long resolveCurrentTenantIdentifier();
    public boolean validateExistingCurrentSessions() { return false; }
}

@Configuration public class TenantHibernateConfig {
    @Bean HibernatePropertiesCustomizer tenantResolverCustomizer(AuthenticatedTenantResolver resolver) {
        return properties -> properties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, resolver);
    }
}

/** Adapter over the security context for services that need the session's establishment explicitly (users, agenda). */
public interface CurrentEstablishmentProvider {
    Optional<Long> establishmentId();
    /** @throws AccessDeniedApplicationException(ESTABLISHMENT_REQUIRED) when absent */
    Long requireEstablishmentId();
}

/** URL prefixes whose data belongs to an establishment. */
public enum TenantScopedPath { PATIENTS("/api/patients/**"), DIAGNOSES("/api/diagnoses/**"), CATALOGS("/api/catalogs/**"),
    TEMPLATES("/api/templates/**"), APPOINTMENTS("/api/appointments/**"), AGENDA("/api/agenda/**"),
    USERS("/api/users/**"), CURRENT_ESTABLISHMENT("/api/establishment/**"); ... }

/** Rejects tenant-scoped requests without an establishment in the token with ESTABLISHMENT_REQUIRED (403). */
public class EstablishmentRequiredInterceptor implements HandlerInterceptor { ... }  // registered by TenantWebConfig for every TenantScopedPath (also the bare prefix without /**)
```
- [ ] **Step 1: Failing tests**
```java
// AuthenticatedTenantResolverTest (unit; SecurityContextHolder set/cleared per test)
@Test void usesTheTokenEstablishment() { authenticate(new AuthenticatedUser(1L,"a","A",Role.ADMIN,5L)); assertThat(resolver.resolveCurrentTenantIdentifier()).isEqualTo(5L); }
@Test void contextOverrideWins() { authenticate(... 5L); assertThat(TenantContext.callAs(9L, resolver::resolveCurrentTenantIdentifier)).isEqualTo(9L); }
@Test void noEstablishmentMeansNone() { assertThat(resolver.resolveCurrentTenantIdentifier()).isEqualTo(TenantSentinel.NONE.getId()); }
// TenantIsolationRepositoryTest (integration)
@Test void repositoriesOnlySeeTheirEstablishment() {
    Long a = tenancy.create("A").getId(), b = tenancy.create("B").getId();
    Long patientOfA = TenantContext.callAs(a, () -> patientRepository.save(patient("30123456")).getId());
    TenantContext.runAs(b, () -> patientRepository.save(patient("30123456"))); // same DNI in another establishment is fine
    assertThat(TenantContext.callAs(b, () -> patientRepository.findById(patientOfA))).isEmpty();
    assertThat(TenantContext.callAs(a, () -> patientRepository.count())).isEqualTo(1);
    assertThat(TenantContext.callAs(TenantSentinel.NONE.getId(), () -> patientRepository.count())).isZero();
}
// EstablishmentRequiredInterceptorTest
@Test void dataEndpointsNeedAnEstablishment() { /* superadmin token without establishment → GET /api/patients → 403 ESTABLISHMENT_REQUIRED */ }
```
- [ ] **Step 2: Compile** (FAIL). **Step 3: Implement** as specified; `SecurityContextEstablishmentProvider` reads `AuthenticatedUser` from `SecurityContextHolder`. Update every existing integration test to run its repository setup under `TenantContext` and to issue tokens with that establishment (they otherwise insert with `NONE` and fail).
- [ ] **Step 4: Compile** (SUCCESS). **Step 5: Commit** — `feat: isolate data per establishment with Hibernate tenant ids (E8)`

---

### Task 4: Establishments API, default catalogs and superadmin bootstrap

**Files:**
- Create: `establishment/catalog/DefaultCatalogEntry.java`, `establishment/catalog/DefaultCatalogSeeder.java`, `establishment/dto/*` (see map), `establishment/service/{EstablishmentService,EstablishmentServiceImpl,CurrentEstablishmentService,CurrentEstablishmentServiceImpl}.java`, `establishment/controller/{EstablishmentController,CurrentEstablishmentController}.java`
- Modify: `user/service/InitialAdminServiceImpl.java`, `user/service/InitialAdminOutcome.java` (wording only if needed), `docs/dev/crear-usuario.md`
- Test: `establishment/controller/EstablishmentControllerTest.java`, `establishment/controller/CurrentEstablishmentControllerTest.java`, `user/bootstrap/InitialAdminBootstrapTest.java` (update)

**Interfaces:**
```java
/** Catalog rows copied into every new establishment (spec §2). */
public enum DefaultCatalogEntry {
    SEGMENT_SHOULDER(CatalogType.BODY_SEGMENTS, "Hombro"), SEGMENT_ELBOW(CatalogType.BODY_SEGMENTS, "Codo"),
    SEGMENT_WRIST(CatalogType.BODY_SEGMENTS, "Muñeca"), SEGMENT_HAND(CatalogType.BODY_SEGMENTS, "Mano"),
    SEGMENT_FINGERS(CatalogType.BODY_SEGMENTS, "Dedos"),
    SURGERY_PLATE(CatalogType.SURGERY_TYPES, "Osteosíntesis con placa"), SURGERY_TENORRHAPHY(CatalogType.SURGERY_TYPES, "Tenorrafia"),
    SURGERY_CARPAL_TUNNEL(CatalogType.SURGERY_TYPES, "Liberación del túnel carpiano"), SURGERY_ARTHRODESIS(CatalogType.SURGERY_TYPES, "Artrodesis"),
    SCAR_NORMOTROPHIC(CatalogType.SCAR_TYPES, "Normotrófica"), SCAR_HYPERTROPHIC(CatalogType.SCAR_TYPES, "Hipertrófica"),
    SCAR_KELOID(CatalogType.SCAR_TYPES, "Queloide"), SCAR_RETRACTILE(CatalogType.SCAR_TYPES, "Retráctil");
    ...
}

/** Catalog tables the seeder writes to (the JPA catalog entities map to these tables). */
public enum CatalogTable { BODY_SEGMENTS("body_segment"), SURGERY_TYPES("surgery_type"), SCAR_TYPES("scar_type"); ... public static CatalogTable of(CatalogType type) }

/** Seeds the default catalogs of a new establishment in the caller's transaction (atomic with the establishment and its admin). */
@Component public class DefaultCatalogSeeder { public void seed(Long establishmentId); }
// Implementation: plain JdbcTemplate INSERTs with an explicit establishment_id, e.g.
//   jdbcTemplate.update("INSERT INTO " + CatalogTable.of(entry.getType()).getTableName()
//       + " (name, active, establishment_id) VALUES (?, TRUE, ?)", entry.getName(), establishmentId);
// JpaTransactionManager exposes its JDBC connection, so these run in the same transaction as the JPA saves. Reason (comment it):
// the Hibernate session of the current transaction was opened with the superadmin's tenant (NONE), so saving tenant entities
// through JPA here would get the wrong tenant. The service saves the establishment with saveAndFlush before seeding (FK).
// Check the catalog tables' column names in V3 before writing the SQL (name, active).

public interface EstablishmentService {
    List<EstablishmentSummaryResponse> list();
    EstablishmentSummaryResponse create(CreateEstablishmentRequest request);
    EstablishmentSummaryResponse rename(Long id, RenameEstablishmentRequest request);
    EstablishmentSummaryResponse changeActive(Long id, boolean active);
}
public interface CurrentEstablishmentService {
    CurrentEstablishmentResponse get();                    // uses CurrentEstablishmentProvider.requireEstablishmentId()
    CurrentEstablishmentResponse rename(RenameEstablishmentRequest request);
}
record CreateEstablishmentRequest(@NotBlank @Size(max = 120) String name, @NotNull @Valid EstablishmentAdminRequest admin)
record EstablishmentAdminRequest(@NotBlank @Size(max = 120) String fullName, @NotBlank @Email String email, @NotBlank @Size(min = 8, max = 72) String password)
record RenameEstablishmentRequest(@NotBlank(message = "Ingresá el nombre del establecimiento.") @Size(max = 120) String name)
record ChangeEstablishmentActiveRequest(@NotNull Boolean active)
record EstablishmentSummaryResponse(Long id, String name, boolean active, long activeUsers, List<AdminRef> admins) { record AdminRef(Long id, String fullName, String email) {} }
record CurrentEstablishmentResponse(Long id, String name, long activeUsers, LocalDate createdDate)
```
Routes: `/api/establishments` (GET, POST 201, `PUT /{id}`, `PATCH /{id}/active`) with `@PreAuthorize("hasRole('SUPERADMIN')")`; `/api/establishment` (GET, PUT) with `@PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")` (the interceptor already requires an establishment). Validation messages in Spanish, matching the existing user DTO messages (copy them from `CreateUserRequest`). Name uniqueness → `ESTABLISHMENT_NAME_TAKEN` (also catch `DataIntegrityViolationException`); admin email taken → existing `USER_EMAIL_TAKEN`; password hashed with the existing `PasswordEncoder`.
`InitialAdminServiceImpl`: `existsByRole(Role.SUPERADMIN)` → `ALREADY_PRESENT`; else create the `.env` user with `Role.SUPERADMIN` and `establishmentId(null)`.

- [ ] **Step 1: Failing tests**
```java
@Test void superadminCreatesAnEstablishmentWithItsAdminAndDefaultCatalogs() {
    // POST /api/establishments {name:"Consultorio Norte", admin:{fullName:"Mariana Gómez", email:"mg@cn.com", password:"secreto123"}} → 201
    // $.admins[0].email == "mg@cn.com", $.activeUsers == 1
    // TenantContext.callAs(newId, () -> bodySegmentRepository.findAllByOrderByNameAsc()) names == [Codo, Dedos, Hombro, Mano, Muñeca]
    // 4 surgery types and 4 scar types likewise; the admin can log in and /me shows the establishment
}
@Test void duplicateNamesAndEmailsAreRejected() { /* same name other case → 409 ESTABLISHMENT_NAME_TAKEN; used email → 409 USER_EMAIL_TAKEN; nothing half-created */ }
@Test void onlyTheSuperadminManagesEstablishments() { /* ADMIN token → 403 on GET/POST */ }
@Test void renameAndDeactivate() { /* PUT name; PATCH active=false → list shows inactive; its admin login → 403 ESTABLISHMENT_INACTIVE */ }
@Test void adminSeesAndRenamesOwnEstablishment() { /* GET /api/establishment → name, activeUsers; PUT → renamed; superadmin outside → 403 ESTABLISHMENT_REQUIRED */ }
@Test void bootstrapCreatesTheSuperadmin() { /* InitialAdminBootstrapTest: SUPERADMIN with null establishment */ }
```
- [ ] **Step 2–4:** compile FAIL → implement → compile SUCCESS.
- [ ] **Step 5: Commit** — `feat: add establishment management for the superadmin and admins (E8)`

---

### Task 5: Scope users, agenda therapists and roles; cross-establishment isolation suite

**Files:**
- Modify: `user/service/UserManagementServiceImpl.java` (+ request validation), `appointment/service/AgendaTherapistServiceImpl.java`, `appointment/service/AppointmentServiceImpl.java` (therapist lookup), `appointment/validation/PermissionStep.java` if it loads users, `appointment/visibility/AppointmentVisibilityPolicyFactory.java` (SUPERADMIN → admin policy; add an `AdminVisibilityPolicy.roles()` returning ADMIN and SUPERADMIN, or map both keys), `catalog/controller/CatalogController.java` and `user/controller/UserController.java` (`hasAnyRole('ADMIN','SUPERADMIN')`)
- Test: `tenant/CrossEstablishmentIsolationTest.java`, `user/controller/UserControllerTest.java` (extend)

**Interfaces:**
- Consumes: `CurrentEstablishmentProvider.requireEstablishmentId()`, `UserRepository.findAllByEstablishmentIdOrderByFullNameAsc`, `findByEstablishmentIdAndRoleOrderByFullNameAsc`.
- Users: list/get/update/activate only within the session establishment (other establishment's id → `USER_NOT_FOUND`); create sets `establishmentId` from the session; role `SUPERADMIN` in a create/update request → `VALIDATION_ERROR` (400) via a `@ValidManagedRole` constraint or explicit check with field error `role` "Elegí Admin o Terapeuta.".
- Agenda therapists and appointment therapist lookups: users of the session establishment only (ADMIN, TERAPEUTA, active rules as today); other establishment's therapist id → `USER_NOT_FOUND`.

- [ ] **Step 1: Failing tests** (`CrossEstablishmentIsolationTest`, two establishments A/B each with admin + therapist + patient + diagnosis + template + appointment created via the API with each admin's token):
```java
@Test void readsNeverCrossEstablishments() {
    // A's admin: GET /api/patients/{patientOfB} → 404; GET /api/diagnoses/{diagnosisOfB} → 404; GET /api/templates/{templateOfB} → 404;
    // GET /api/appointments/{appointmentOfB} → 404; GET /api/patients?q= → only A's; GET /api/catalogs/body-segments → A's 5 rows;
    // GET /api/agenda/therapists → only A's users; GET /api/users → only A's users
}
@Test void writesCannotReferenceAnotherEstablishment() {
    // POST /api/appointments with therapistId of B → 404 USER_NOT_FOUND; with patientId of B → 404 PATIENT_NOT_FOUND;
    // POST diagnosis with surgeryTypeId of B → 404 CATALOG_ITEM_NOT_FOUND; POST follow-up with scarTypeId of B → 404;
    // PUT /api/users/{userOfB} → 404 USER_NOT_FOUND
}
@Test void uniquenessIsPerEstablishment() {
    // same DNI in A and B → both 201; second same DNI in A → 409 PATIENT_DNI_DUPLICATE;
    // catalog "Codo" exists in both by default; creating "codo" again in A → 409 CATALOG_ITEM_DUPLICATE
}
@Test void superadminInsideActsAsAdminOnlyThere() {
    // superadmin enters A (POST /api/auth/establishment) → GET /api/patients lists A's; GET /api/patients/{patientOfB} → 404;
    // can create users in A; outside (null) → GET /api/patients → 403 ESTABLISHMENT_REQUIRED
}
@Test void adminCannotCreateASuperadmin() { /* POST /api/users {role:"SUPERADMIN"} → 400 VALIDATION_ERROR */ }
```
- [ ] **Step 2–4:** compile FAIL → implement → compile SUCCESS.
- [ ] **Step 5: Commit** — `feat: scope users and agenda to the establishment (E8)`

---

### Task 6: Frontend session, guards, navigation and the superadmin banner

**Files:**
- Modify: `core/auth/auth.models.ts` (`Role` adds `'SUPERADMIN'`; `UserSummary.establishment: { id: number; name: string } | null`), `core/auth/auth.service.ts` (`enterEstablishment(id: number | null): Observable<void>` → `POST /api/auth/establishment` then `setSession`; computed `isSuperadmin`, `establishment`, `isAdminInEstablishment`), `core/auth/auth.guards.ts` (`superadminGuard`: SUPERADMIN without establishment, else `/pacientes`; `establishmentGuard`: session has establishment, else `/establecimientos` for superadmin; `adminGuard`: ADMIN, or SUPERADMIN with establishment), `app.routes.ts` (default redirect by mode; data routes under `establishmentGuard`; new routes `establecimientos` (superadminGuard) and `establecimiento` (adminGuard)), `layout/nav-items.ts` (`SUPERADMIN_NAV_ITEMS = [Establecimientos]`, icon `'establishment'`), `layout/side-nav/*` (establishment block under the logo per `desktop-dentro-establecimiento.png`; superadmin-outside menu per `desktop-establecimientos.png`; "Establecimiento" in Administración), `layout/tab-bar/*` (superadmin outside: Establecimientos + Más), `layout/user-initials.ts` (`ROLE_LABELS.SUPERADMIN = 'Superadmin'`), `layout/shell/*` (render `EstablishmentBanner` when superadmin inside), `features/users/user-form-page/user-form-page.ts` (role options only ADMIN/TERAPEUTA), `features/users/users-page/*` (badge for unknown role safe), `features/agenda/agenda-page/agenda-page.ts` and `features/agenda/appointment-form/appointment-form.ts` (admin checks → ADMIN or SUPERADMIN)
- Create: `layout/establishment-banner/establishment-banner.{ts,html,scss,spec.ts}` ("Estás en **‹nombre›**" + "Salir" (mobile) / "… como superadmin, con permisos de admin." + "Salir del establecimiento" (desktop) → `enterEstablishment(null)` then navigate `/establecimientos`)
- Test: `auth.guards.spec.ts`, `auth.service.spec.ts`, `nav-items.spec.ts`, `establishment-banner.spec.ts`, `shell.spec.ts` (update)

- [ ] **Step 1: Failing specs**
```ts
it('sends a superadmin without establishment to /establecimientos', () => { /* establishmentGuard → UrlTree '/establecimientos' */ });
it('lets the superadmin inside an establishment use admin screens', () => { /* adminGuard true for {role:'SUPERADMIN', establishment:{id:3,name:'Norte'}} */ });
it('enterEstablishment stores the new session', () => { /* POST /api/auth/establishment {establishmentId:3} → session token replaced, establishment Norte */ });
it('shows the banner and leaves the establishment', () => { /* text 'Estás en Norte'; click Salir → enterEstablishment(null) and navigate */ });
it('superadmin outside sees only Establecimientos in the menu', () => { /* side nav labels */ });
it('offers only Admin and Terapeuta when creating users', () => { /* user form options */ });
```
- [ ] **Step 2–4:** type-check FAIL → implement → `ng build` + type-check SUCCESS.
- [ ] **Step 5: Commit** — `feat: superadmin mode and establishment in the navigation (E8)`

---

### Task 7: Superadmin establishments screens

**Files:**
- Create: `features/establishments/establishments.models.ts`, `establishments.service.ts` (+spec), `establishments-page/*` (cards on mobile per `mobile-establecimientos.png`, table on desktop per `desktop-establecimientos.png`; inline rename on the pencil (same pattern as `features/catalogs` inline rename); active switch → `PATCH`; "Entrar" disabled when inactive → `auth.enterEstablishment(id)` then `/pacientes`), `establishment-form/*` (shared form: name + "Primer administrador" fullName/email/password with the hint "Mínimo 8 caracteres. El admin puede cambiarla después." and the info box; reactive form with validation; maps `USER_EMAIL_TAKEN` to the email field "Ya hay un usuario con este email." and `ESTABLISHMENT_NAME_TAKEN` to the name field), `establishment-form-page/*` (mobile route `/establecimientos/nuevo`), desktop side panel inside the list page per `desktop-nuevo-establecimiento.png`
- Modify: `app.routes.ts` (`establecimientos`, `establecimientos/nuevo`)
- Test: specs for page (list, rename, toggle, enter disabled when inactive), form (validation, error mapping, create emits), service (URLs)

- [ ] **Step 1: Failing specs** (as listed); **Step 2–4:** type-check FAIL → implement (styles from `docs/mockups/h8-establecimientos/source/*.html`) → `ng build` + type-check SUCCESS. **Step 5: Commit** — `feat: establishments screens for the superadmin (E8)`

---

### Task 8: Admin "Establecimiento" screen and "Más"

**Files:**
- Create: `features/establishment/current-establishment.service.ts` (+spec: `GET/PUT /api/establishment`), `features/establishment/establishment-page/*` (per `mobile-establecimiento-admin.png`: name field + "Guardar cambios", info rows "Usuarios · N activos", "Creado · dd/mm/aaaa", info box; after rename → `auth.refreshCurrentUser()` so the menu shows the new name)
- Modify: `features/more/more-page.*` (establishment card at the top for users with an establishment; "Establecimiento" row in Administración for ADMIN / SUPERADMIN inside), `app.routes.ts` (`establecimiento`, adminGuard + establishmentGuard)
- Test: page spec (loads, validates empty name, saves and refreshes the user), more-page spec (card + row visibility by role)

- [ ] **Step 1–4** as above. **Step 5: Commit** — `feat: establishment screen for admins (E8)`

---

### Task 9: Final verification and merge into test

- [ ] Backend `test-compile` (exit 0); frontend `ng build` (no warnings) and spec type-check (exit 0); init scripts twice on an empty Postgres (17 tables).
- [ ] `docs/superpowers/plans/2026-11-04-e0-pendientes.md`: É8 section (out-of-scope items from the spec §9; note that V10 wipes data).
- [ ] Commit on `feat/multi-tenant`; merge `--no-ff` into `test` from the main folder; rebuild local containers (`docker compose up -d --build backend frontend`) — **V10 wipes the local test database** (tell the user before rebuilding). Smoke: superadmin login → `/api/establishments` 200; create an establishment; its admin logs in and sees the 5 default segments.
- [ ] Only after the user approves on `test`: merge `feat/multi-tenant` into `main` (never `test`), remove the worktree (junction first). Production deploy only after a Supabase backup and the user's explicit confirmation.
