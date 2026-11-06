package com.tomanagement.tenant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tomanagement.TestTable;
import com.tomanagement.TestTables;
import com.tomanagement.TestTenancy;
import com.tomanagement.TestcontainersConfiguration;
import com.tomanagement.auth.security.AuthenticatedUser;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.user.entity.Role;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Tenant-scoped endpoints need an establishment in the token (É8); other endpoints do not.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestTables.class, TestTenancy.class})
class EstablishmentRequiredInterceptorTest {

    private static final AuthenticatedUser SUPERADMIN_OUTSIDE =
            new AuthenticatedUser(1L, "root@plataforma.com", "Root", Role.SUPERADMIN, null);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestTables testTables;

    @Autowired
    private TestTenancy tenancy;

    private Long establishmentId;

    @BeforeEach
    void seed() {
        testTables.truncate(TestTable.ESTABLISHMENT);
        establishmentId = tenancy.create("Consultorio").getId();
    }

    @Test
    void dataEndpointsNeedAnEstablishment() throws Exception {
        performAs(SUPERADMIN_OUTSIDE, "/api/patients")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.ESTABLISHMENT_REQUIRED.name()));
        performAs(SUPERADMIN_OUTSIDE, "/api/patients/1")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.ESTABLISHMENT_REQUIRED.name()));
        performAs(SUPERADMIN_OUTSIDE, "/api/catalogs/body-segments")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.ESTABLISHMENT_REQUIRED.name()));
        performAs(SUPERADMIN_OUTSIDE, "/api/agenda/therapists")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.ESTABLISHMENT_REQUIRED.name()));
    }

    @Test
    void aTokenWithAnEstablishmentReachesTheData() throws Exception {
        AuthenticatedUser superadminInside =
                new AuthenticatedUser(1L, "root@plataforma.com", "Root", Role.SUPERADMIN, establishmentId);

        performAs(superadminInside, "/api/patients")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void endpointsOutsideTheTenantPathsDoNotNeedAnEstablishment() throws Exception {
        performAs(SUPERADMIN_OUTSIDE, "/api/test/secured").andExpect(status().isOk());
    }

    @Test
    void anonymousRequestsStillGet401() throws Exception {
        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.AUTHENTICATION_REQUIRED.name()));
    }

    private ResultActions performAs(AuthenticatedUser user, String url) throws Exception {
        return mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, "Bearer " + tenancy.tokenFor(user)));
    }
}
