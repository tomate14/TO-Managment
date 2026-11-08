package com.tomanagement.establishment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tomanagement.TestTable;
import com.tomanagement.TestTables;
import com.tomanagement.TestTenancy;
import com.tomanagement.TestcontainersConfiguration;
import com.tomanagement.catalog.repository.BodySegmentRepository;
import com.tomanagement.catalog.repository.ScarTypeRepository;
import com.tomanagement.catalog.repository.SurgeryTypeRepository;
import com.tomanagement.common.exception.DuplicateResourceException;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.establishment.entity.Establishment;
import com.tomanagement.establishment.entity.EstablishmentKind;
import com.tomanagement.user.entity.Role;
import com.tomanagement.user.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestTables.class, TestTenancy.class})
class EstablishmentCreatorImplTest {

    @Autowired
    private EstablishmentCreator creator;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BodySegmentRepository bodySegmentRepository;

    @Autowired
    private SurgeryTypeRepository surgeryTypeRepository;

    @Autowired
    private ScarTypeRepository scarTypeRepository;

    @Autowired
    private TestTables testTables;

    @Autowired
    private TestTenancy tenancy;

    @BeforeEach
    void clean() {
        testTables.truncate(TestTable.APP_USER, TestTable.ESTABLISHMENT);
    }

    @Test
    void createsAPendingPracticeWithItsAdminAndDefaultCatalogs() {
        Establishment practice = creator.create(request("ana@ind.com"));

        assertThat(practice.getKind()).isEqualTo(EstablishmentKind.INDIVIDUAL);
        assertThat(practice.isPendingApproval()).isTrue();
        assertThat(userRepository.findByEstablishmentIdAndRoleOrderByFullNameAsc(practice.getId(), Role.ADMIN))
                .hasSize(1);
        long catalogRows = tenancy.callAs(practice.getId(), () -> bodySegmentRepository.count()
                + surgeryTypeRepository.count() + scarTypeRepository.count());
        assertThat(catalogRows).isEqualTo(13);
    }

    @Test
    void anExistingEmailIsRejected() {
        creator.create(request("ana@ind.com"));

        assertThatThrownBy(() -> creator.create(request("ana@ind.com")))
                .isInstanceOfSatisfying(DuplicateResourceException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_EMAIL_TAKEN));
    }

    private NewEstablishment request(String email) {
        return new NewEstablishment("Mi consultorio", EstablishmentKind.INDIVIDUAL, false, "Ana Perez", email,
                "secreto123");
    }
}
