package com.tomanagement.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;

import com.tomanagement.TestTable;
import com.tomanagement.TestTables;
import com.tomanagement.TestTenancy;
import com.tomanagement.TestcontainersConfiguration;
import com.tomanagement.catalog.entity.BodySegment;
import com.tomanagement.catalog.repository.BodySegmentRepository;
import com.tomanagement.patient.entity.Dominance;
import com.tomanagement.patient.entity.Patient;
import com.tomanagement.patient.entity.PatientData;
import com.tomanagement.patient.repository.PatientRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

/**
 * Hibernate {@code @TenantId} isolation (É8) against a real Postgres: each establishment only sees its own rows.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestTables.class, TestTenancy.class})
class TenantIsolationRepositoryTest {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private BodySegmentRepository bodySegmentRepository;

    @Autowired
    private TestTables testTables;

    @Autowired
    private TestTenancy tenancy;

    @BeforeEach
    void clean() {
        testTables.truncate(TestTable.ESTABLISHMENT);
    }

    @Test
    void repositoriesOnlySeeTheirEstablishment() {
        Long a = tenancy.create("A").getId();
        Long b = tenancy.create("B").getId();
        Long patientOfA = TenantContext.callAs(a, () -> patientRepository.save(patient("30123456")).getId());
        TenantContext.runAs(b, () -> patientRepository.save(patient("30123456"))); // same DNI in another establishment is fine

        assertThat(TenantContext.callAs(b, () -> patientRepository.findById(patientOfA))).isEmpty();
        assertThat(TenantContext.callAs(a, () -> patientRepository.findById(patientOfA))).isPresent();
        assertThat(TenantContext.callAs(a, () -> patientRepository.count())).isEqualTo(1);
        assertThat(TenantContext.callAs(TenantSentinel.NONE.getId(), () -> patientRepository.count())).isZero();
    }

    @Test
    void catalogsInheritTheTenantFromTheMappedSuperclass() {
        Long a = tenancy.create("A").getId();
        Long b = tenancy.create("B").getId();
        TenantContext.runAs(a, () -> bodySegmentRepository.save(new BodySegment("Codo")));
        TenantContext.runAs(b, () -> bodySegmentRepository.save(new BodySegment("Codo")));

        assertThat(TenantContext.callAs(a, () -> bodySegmentRepository.count())).isEqualTo(1);
        assertThat(TenantContext.callAs(b, () -> bodySegmentRepository.count())).isEqualTo(1);
    }

    @Test
    void withoutAnEstablishmentNothingCanBeWritten() {
        assertThatThrownBy(() -> patientRepository.saveAndFlush(patient("30123456")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static Patient patient(String dni) {
        return Patient.builder()
                .dni(dni)
                .data(new PatientData("Lucía", "Fernández", LocalDate.of(1984, 3, 21), "249 412-3456", "Docente", "OSDE",
                        "61 234567 8 01", Dominance.RIGHT))
                .createdAt(Instant.parse("2026-11-04T12:00:00Z"))
                .build();
    }
}
