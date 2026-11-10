# Patient appointment billing verification

Verified locally on 2026-11-09/10: initial implementation `ccfc329` and the final calendar-entry correction based on `5c6f040`, on `feat/patient-appointment-billing`. No environment file or production database was used or changed. No deployment or merge was performed.

## Results

| Check | Actual result |
| --- | --- |
| Full backend, unchanged production Java at `6103eae` | 385 tests: 370 passed, 0 assertion failures, 15 pre-existing fixture errors; Maven/Docker exit 1 |
| Added/strengthened backend integration checks, Java 21 + disposable PostgreSQL 15 | 14 passed, 0 failures/errors; Maven/Docker exit 0 |
| Backend test sensitivity, mutations confined to disposable source copy | 2 tests failed as intended: timestamp overwrite and missing billing patient NOT NULL |
| Original frontend `a6a4b61`, fresh disposable checkout using the same dependencies | 464 tests: 448 passed, 16 failed in 7 files; exit 1 |
| Initial feature frontend `ccfc329` | 478 tests: 449 passed, 29 failed in 9 files; exit 1 |
| Full frontend after route test correction, before final calendar-entry correction | 478 tests: 462 passed, the same 16 baseline failures in 7 files; exit 1 |
| Production frontend build after the final CSS correction | Passed, exit 0 |
| Chromium browser against real Angular dev server, synthetic intercepted API | Passed at 360×900 and 1366×900; exit 0 |
| Final calendar-entry correction: focused agenda tests | 25 passed in 4 files; exit 0 |
| Final calendar-entry correction: production build | Passed, exit 0 |
| Final calendar-entry correction: extended Chromium browser harness | Existing flows plus yesterday/previous-week calendar confirmation passed at both sizes; exit 0 |
| `git diff --check` | Passed |

The backend evidence is unchanged. The recorded full frontend suite precedes the CSS width and calendar-entry corrections. Focused agenda tests, production build, and the complete extended browser checks were repeated after the final frontend-only correction. Backend and full frontend suites were not repeated for that scoped correction; their documented baseline failures remain.

## Checks completed

- Two actual concurrent service calls in separate Spring transactions synchronize at a barrier, use distinct request times, and return the same persisted confirmation instant. Patient billing then contains exactly one pending appointment session. The test runs against PostgreSQL, with no service/repository mocks.
- V14 legacy fixtures migrate to V15 without changing old billing/follow-up data or confirming old appointments. The NOT NULL assertion now uses verified-unused ID 999 and specifically requires SQLSTATE `23502` mentioning `patient_id`, so duplicate-key errors cannot satisfy it.
- Two fresh databases inside a disposable Testcontainers instance receive Flyway and consolidated SQL respectively. `01_schema.sql` and `02_seed.sql` execute twice without errors. Each pass compares sorted column types, sizes, precision, defaults, nullability, all table constraint definitions, and indexes against Flyway. Column physical order and Flyway's own bookkeeping table are excluded. Scripts run through JDBC after substituting the psql schema placeholder; no production connection is involved.
- The existing affected backend tests cover ownership/tenant isolation, stale billing selection, concurrent billing registration, new/legacy voiding, preserved history, and independence from clinical follow-ups.
- Both browser sizes book a turn without diagnosis, confirm it, open its patient, select only the appointment, register billing, see the agenda marked billed, void and restore pending, inspect and void legacy history without increasing counters, follow the old diagnosis billing URL, and submit the clinical follow-up form without changing billing counters.
- Both sizes also open yesterday (2026-11-09) and the previous week (2026-11-03) from the calendar while the browser clock advances from 2026-11-10. The existing panel/mobile page shows the original date/year/time, confirms without changing that date, and includes it in patient pending billing. The assigned list still excludes both dates. The harness verifies one POST on double click, an injected recoverable failure/retry, confirmation by Enter, 44px controls, no editing controls after success, and no horizontal viewport overflow. Detail Reprogramar opens the existing form; detail cancellation sends DELETE. Existing direct rescheduling is also submitted successfully.
- Browser keyboard coverage includes Enter activation for booking, confirmation, billing and voiding, focus verification, and Space to deselect/reselect a session. Both footer buttons must remain completely inside their footer. This is focused keyboard coverage, not a complete accessibility audit.
- Screenshots were inspected for the mobile billing page and desktop side panel. All captured data is synthetic. [Mobile form](screenshots/360-billing-form.png), [desktop form](screenshots/1366-billing-form.png), [mobile billed agenda](screenshots/360-agenda-billed.png), [desktop legacy history](screenshots/1366-voided-legacy-history.png). Other screenshots and the request-path checklist are in `screenshots/`.

## Corrections and scope rulings

The new route unit test executed the lazy component graph while importing the same component eagerly. This changed Angular's test chunks and exposed undefined imported constants/empty option lists in otherwise unchanged forms. The new failing files passed 11/11 in isolation; excluding the route spec restored exactly the baseline 16 failures. Replacing only the eager import with a dynamic import still produced 32 failures. Keeping the spec focused on registered lazy routes and redirect UrlTrees restored the baseline with all 478 tests included. Actual lazy navigation and component rendering are verified by the browser harness. No application route was changed.

Visual verification found a pre-existing desktop footer defect in the reused billing form: the global primary-button width occupied the entire footer and clipped Cancelar. The controller authorized a scoped usability fix. A browser geometry assertion first failed with `1366px footer clips Cancelar`; setting only the desktop submit width to `auto` made it pass at both sizes. Mobile retains its full-width flexible submit behavior. Cost: one CSS declaration and review of the footer screenshots.

Whole-branch review found that the assigned-list date filter made past appointments unreachable for confirmation. The final correction preserves that filter and exposes the existing appointment card in the calendar-selected desktop panel or existing mobile page (`detalle=1`). Its Reprogramar action transitions to the original form; direct reschedule URLs without that flag retain their form. Successful confirmation updates the selected card and calendar state, and locks editing. No date restriction, reversal, new screen, or backend change was added. The cancellation callback closes only a matching selected appointment, preserving an unrelated open booking form.

Reviewed synthetic captures: [mobile previous-week confirmation](screenshots/360-calendar-2026-11-03-confirmed.png), [desktop yesterday confirmation](screenshots/1366-calendar-2026-11-09-confirmed.png), and [previous-week patient pending](screenshots/360-calendar-2026-11-03-pending.png). The complete browser request checklist remains in `screenshots/results.json`.

## Reproduce

Run from repository root. The backend command copies the two mounted source directories into disposable Linux storage to avoid Windows bind-mount JVM overhead. It uses the existing Java 21 image, Maven cache and Docker socket, never `.env`.

```powershell
$backendPath = (Resolve-Path backend).Path
$schemaPath = (Resolve-Path supabase).Path
$script = @'
mkdir -p /tmp/task5/backend /tmp/task5/supabase
cp -a /source/. /tmp/task5/backend/
cp -a /schema/. /tmp/task5/supabase/
cd /tmp/task5/backend
./mvnw -B -q test -Dtest=AppointmentControllerTest,PatientBillingMigrationTest 2>&1
result=$?
echo MAVEN_EXIT_CODE=$result
mkdir -p /source/target/task5-reports
cp -a target/surefire-reports/. /source/target/task5-reports/
exit $result
'@
docker run --rm --mount "type=bind,source=$backendPath,target=/source" --mount "type=bind,source=$schemaPath,target=/schema,readonly" --mount type=volume,source=to-management-m2,target=/root/.m2 --mount type=bind,source=/var/run/docker.sock,target=/var/run/docker.sock -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal maven:3.9-eclipse-temurin-21 sh -c $script
$taskDockerExit = $LASTEXITCODE
Write-Output "DOCKER_EXIT_CODE=$taskDockerExit"
```

For the two mutation checks, in the disposable copy only, replace `if (confirmedAt == null)` with `if (true)` in `Appointment.java`, remove `ALTER TABLE billing ALTER COLUMN patient_id SET NOT NULL;` from V15, and run:

```sh
./mvnw -B -q test '-Dtest=AppointmentControllerTest#concurrentConfirmationsKeepOneTimestampAndOnePendingSession,PatientBillingMigrationTest#migrationPreservesLegacyHistoryAndDoesNotConfirmAppointments'
```

The original full backend command used the same copy/socket/cache approach with `./mvnw -B test` and merged stderr; the task report keeps its original log and XMLs. For frontend, run in `frontend/`:

```powershell
npm.cmd test -- --watch=false
npm.cmd run build
npm.cmd start -- --host 127.0.0.1 --port 4217
```

For the final calendar-entry correction's focused tests, run in `frontend/`:

```powershell
npm.cmd test -- --watch=false --include='src/app/features/agenda/agenda-page/agenda-confirmation.spec.ts' --include='src/app/features/agenda/appointment-form-page/appointment-form-page.spec.ts' --include='src/app/features/agenda/appointments-list/appointments-list.spec.ts' --include='src/app/features/agenda/agenda.service.spec.ts'
```

The browser harness uses Playwright as temporary tooling, without adding an application dependency. With the dev server running, from repository root:

```powershell
npm.cmd install --prefix .superpowers/sdd/2026-11-09-patient-appointment-billing/browser-tooling --no-package-lock --no-save playwright
$env:NODE_PATH = (Resolve-Path .superpowers/sdd/2026-11-09-patient-appointment-billing/browser-tooling/node_modules).Path
$env:CHROMIUM_EXECUTABLE = 'C:/Users/Maxi/AppData/Local/ms-playwright/chromium_headless_shell-1243/chrome-headless-shell-win64/chrome-headless-shell.exe'
node docs/verification/patient-appointment-billing/browser-check.cjs
$browserExit = $LASTEXITCODE
Remove-Item Env:NODE_PATH
Remove-Item Env:CHROMIUM_EXECUTABLE
Write-Output "BROWSER_EXIT_CODE=$browserExit"
```

Use a local Chromium executable on another machine, or omit `CHROMIUM_EXECUTABLE` when Playwright's default browser is installed. The harness intercepts every `/api/**` request, checks request IDs/amounts, and rejects unexpected endpoints. Its assertions verify Angular navigation, rendering, actions and layout; they do **not** prove live browser-to-backend integration or server rules. Separate real-PostgreSQL backend tests provide that server evidence. The owned dev server was stopped after verification.

## Remaining baseline failures

Backend errors all occur before the test body: fixture User creation omits required `createdAt`.

- `AvailabilityServiceImplTest`: `sundaysHaveNoSlots`, `theExcludedAppointmentDoesNotBlockItsOwnSlot`, `listsStartsEveryHalfHourSoTheLastOneEndsAtClosing`, `marksPastBusyTherapistAndBusyPatient`.
- `AppointmentValidationChainTest`: `aTherapistCannotBookAnotherTherapistsAgenda`, `rejectsStartsOutsideTheAgenda` (4 parameter cases), `diagnosisOfAnotherPatient`, `backToBackIsNotABusyTherapist`, `rejectsThePast`, `inactiveTherapist`, `busyTherapistAndBusyPatient`, `acceptsTheLastSlotThatEndsAtClosing`.

Frontend failure names match the fresh original revision:

- `AgendaPage`: cancels an appointment, reloads the week and shows an alert on failure; keeps "Turnos asignados" on the real current week after the calendar moves.
- `AppointmentForm`: keeps the patient locked when rescheduling; shows an alert when availability fails; shows a hint when the day has no slots.
- `EstablishmentsPage` / trial period: focuses the date input on open and returns focus to the trigger on close.
- `FollowUpFormPage`: does not save while a required ROM metric is empty; saves the whole follow-up and goes back to the diagnosis; shows out-of-range errors under their field; shows the per-metric errors of the server under their metric.
- `PatientFormPage`: shows the mockup sections and Diestro preselected; creates the patient with the DNI as digits and opens the patient page.
- `QuestionFormPage`: chains to an earlier active question with a yes/no value and a preview; shows the mockup error for greater-than on a yes/no parent and does not save; offers the parent options as expected values.
- `TemplateEditPage`: creates a template and continues to its editor.

These unrelated fixtures, timing and selector failures remain. The project-wide suites are therefore not green. No new unresolved feature failure was observed. Independent review of the final task and complete feature diff belongs to the controller before integration.

## Local detailed evidence

The ignored handoff directory `.superpowers/sdd/2026-11-09-patient-appointment-billing/` retains `backend-full.log`, `backend-full-reports/`, `task-5-backend-green.log`, `task-5-backend-mutation.log`, `frontend-baseline.log`, `baseline-current.log`, `frontend-final.log`, `frontend-isolated-investigation.log`, `frontend-excluding-routes.log`, `frontend-routes-fix.log`, `frontend-routes-fix2.log`, `task-5-frontend-build.log`, `browser-layout-red.log`, `browser-layout-green.log`, and `browser-final.log`. Focused Java XMLs are under `backend/target/task5-reports/`; mutation XMLs are under `backend/target/task5-mutation-reports/`. This committed report and browser artifacts preserve the useful result if scratch handoff files are removed.

Final calendar-entry evidence: `final-fix-report.md`, `final-fix-red-corrected.log`, `final-fix-booking-red.log`, `final-fix-final-tests.log`, `final-fix-final-build.log`, and `final-fix-final-browser.log` in the same ignored handoff directory. The final browser run still intercepts every API request using synthetic fixtures; it does not constitute live full-stack integration.
