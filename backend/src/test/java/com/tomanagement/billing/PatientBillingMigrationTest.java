package com.tomanagement.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import com.tomanagement.TestContainerImage;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class PatientBillingMigrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(TestContainerImage.POSTGRES.getImageName());

    @Test
    void migrationPreservesLegacyHistoryAndDoesNotConfirmAppointments() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).schemas("migration_app").target("14").load().migrate();
        dataSource.setSchema("migration_app");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        seedV14(jdbc);
        List<Map<String, Object>> originalBillings = jdbc.queryForList("SELECT * FROM billing ORDER BY id");
        List<Map<String, Object>> originalLinks = jdbc.queryForList("SELECT * FROM billing_follow_up ORDER BY billing_id, follow_up_id");
        List<Map<String, Object>> originalFollowUps = jdbc.queryForList("SELECT * FROM follow_up ORDER BY id");
        List<Map<String, Object>> originalAppointments = jdbc.queryForList("SELECT * FROM appointment ORDER BY id");

        Flyway.configure().dataSource(dataSource).schemas("migration_app").load().migrate();

        assertThat(jdbc.queryForList("SELECT id, establishment_id, diagnosis_id, billed_on, amount, created_at, voided_at FROM billing ORDER BY id"))
                .isEqualTo(originalBillings);
        assertThat(jdbc.queryForList("SELECT * FROM billing_follow_up ORDER BY billing_id, follow_up_id"))
                .isEqualTo(originalLinks);
        assertThat(jdbc.queryForList("SELECT * FROM follow_up ORDER BY id")).isEqualTo(originalFollowUps);
        assertThat(jdbc.queryForList("SELECT id, therapist_id, patient_id, diagnosis_id, start_at, end_at, notes, created_at, establishment_id FROM appointment ORDER BY id"))
                .isEqualTo(originalAppointments);
        assertThat(jdbc.queryForList("SELECT patient_id FROM billing ORDER BY id", Long.class)).containsExactly(1L, 2L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM appointment WHERE confirmed_at IS NOT NULL", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM billing_appointment", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM follow_up f WHERE NOT EXISTS (SELECT 1 FROM billing_follow_up b WHERE b.follow_up_id = f.id)", Long.class))
                .isEqualTo(1L);

        jdbc.update("INSERT INTO billing (id, establishment_id, patient_id, billed_on, amount, created_at) VALUES (3, 1, 1, '2026-11-09', 100, now())");
        jdbc.update("INSERT INTO billing_appointment (billing_id, appointment_id, establishment_id) VALUES (1, 1, 1)");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO billing_appointment (billing_id, appointment_id, establishment_id) VALUES (3, 1, 1)"))
                .isInstanceOf(DataIntegrityViolationException.class);
        jdbc.update("UPDATE billing_appointment SET active = false WHERE billing_id = 1");
        jdbc.update("INSERT INTO billing_appointment (billing_id, appointment_id, establishment_id) VALUES (3, 1, 1)");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO billing (establishment_id, billed_on, amount, created_at) VALUES (1, '2026-11-09', 100, now())"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void seedV14(JdbcTemplate jdbc) {
        jdbc.execute("""
                INSERT INTO establishment (id, name, created_at, approved_at) VALUES (1, 'Migration fixture', now(), now());
                INSERT INTO app_user (id, email, password_hash, full_name, role, establishment_id)
                    VALUES (1, 'migration@example.test', 'unused', 'Fixture therapist', 'TERAPEUTA', 1);
                INSERT INTO patient (id, dni, first_name, last_name, birth_date, phone, occupation,
                    health_insurance, health_insurance_number, dominance, establishment_id)
                    VALUES (1, '10000001', 'Fixture', 'One', '1990-01-01', '', '', '', '', 'RIGHT', 1),
                           (2, '10000002', 'Fixture', 'Two', '1990-01-01', '', '', '', '', 'LEFT', 1);
                INSERT INTO treatment (id, treatment_type, establishment_id) VALUES (1, 'CONSERVATIVE', 1), (2, 'CONSERVATIVE', 1);
                INSERT INTO conservative_treatment (treatment_id, has_immobilization) VALUES (1, false), (2, false);
                INSERT INTO diagnosis (id, patient_id, description, created_date, treatment_id, establishment_id)
                    VALUES (1, 1, 'Fixture one', '2026-10-01', 1, 1), (2, 2, 'Fixture two', '2026-10-01', 2, 1);
                INSERT INTO follow_up (id, diagnosis_id, follow_up_date, establishment_id)
                    VALUES (1, 1, '2026-10-02', 1), (2, 1, '2026-10-03', 1),
                           (3, 2, '2026-10-04', 1), (4, 1, '2026-10-05', 1);
                INSERT INTO billing (id, establishment_id, diagnosis_id, billed_on, amount, created_at, voided_at)
                    VALUES (1, 1, 1, '2026-10-06', 200, '2026-10-06T12:00:00Z', null),
                           (2, 1, 2, '2026-10-06', 100, '2026-10-06T12:00:00Z', '2026-10-07T12:00:00Z');
                INSERT INTO billing_follow_up (billing_id, follow_up_id, establishment_id, active)
                    VALUES (1, 1, 1, true), (1, 2, 1, true), (2, 3, 1, false);
                INSERT INTO appointment (id, therapist_id, patient_id, diagnosis_id, start_at, end_at, created_at, establishment_id)
                    VALUES (1, 1, 1, 1, '2026-11-10T10:00:00', '2026-11-10T10:30:00', now(), 1),
                           (2, 1, 2, null, '2026-11-10T11:00:00', '2026-11-10T11:30:00', now(), 1);
                """);
    }
}
