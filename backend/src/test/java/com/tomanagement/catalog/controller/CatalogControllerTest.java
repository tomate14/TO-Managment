package com.tomanagement.catalog.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomanagement.TestTable;
import com.tomanagement.TestTables;
import com.tomanagement.TestcontainersConfiguration;
import com.tomanagement.auth.security.AuthenticatedUser;
import com.tomanagement.auth.security.TokenProvider;
import com.tomanagement.catalog.entity.BodySegment;
import com.tomanagement.catalog.entity.ScarType;
import com.tomanagement.catalog.entity.SurgeryType;
import com.tomanagement.catalog.repository.BodySegmentRepository;
import com.tomanagement.catalog.repository.ScarTypeRepository;
import com.tomanagement.catalog.repository.SurgeryTypeRepository;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.user.entity.Role;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestTables.class})
class CatalogControllerTest {

    private static final String SEGMENTS = "/api/catalogs/body-segments";
    private static final AuthenticatedUser ADMIN = new AuthenticatedUser(1L, "admin@c.com", "Admin", Role.ADMIN, null);
    private static final AuthenticatedUser THERAPIST = new AuthenticatedUser(2L, "t@c.com", "Terapeuta", Role.TERAPEUTA, null);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BodySegmentRepository bodySegmentRepository;

    @Autowired
    private SurgeryTypeRepository surgeryTypeRepository;

    @Autowired
    private ScarTypeRepository scarTypeRepository;

    @Autowired
    private TestTables testTables;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private BodySegment wrist;

    @BeforeEach
    void seedCatalogs() {
        testTables.truncate(TestTable.BODY_SEGMENT, TestTable.SURGERY_TYPE, TestTable.SCAR_TYPE);
        wrist = bodySegmentRepository.save(new BodySegment("Muñeca"));
        BodySegment fingers = new BodySegment("Dedos");
        fingers.changeActive(false);
        bodySegmentRepository.save(fingers);
        bodySegmentRepository.save(new BodySegment("Codo"));
        surgeryTypeRepository.save(new SurgeryType("Tenorrafia"));
        scarTypeRepository.save(new ScarType("Queloide"));
    }

    @Test
    void anySignedInUserListsACatalogOrderedByName() throws Exception {
        mockMvc.perform(as(THERAPIST, get(SEGMENTS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("Codo"))
                .andExpect(jsonPath("$[1].name").value("Dedos"))
                .andExpect(jsonPath("$[1].active").value(false));
    }

    @Test
    void activeOnlyHidesInactiveItems() throws Exception {
        mockMvc.perform(as(THERAPIST, get(SEGMENTS).param("activeOnly", "true")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void eachSlugServesItsOwnCatalog() throws Exception {
        mockMvc.perform(as(ADMIN, get("/api/catalogs/surgery-types")))
                .andExpect(jsonPath("$[0].name").value("Tenorrafia"));
        mockMvc.perform(as(ADMIN, get("/api/catalogs/scar-types")))
                .andExpect(jsonPath("$[0].name").value("Queloide"));
    }

    @Test
    void unknownCatalogIsNotFound() throws Exception {
        mockMvc.perform(as(ADMIN, get("/api/catalogs/medicamentos")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.CATALOG_NOT_FOUND.name()));
    }

    @Test
    void adminCreatesAnItem() throws Exception {
        mockMvc.perform(as(ADMIN, post(SEGMENTS)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "  Antebrazo "))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Antebrazo"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void duplicateNameIgnoringCaseIsRejected() throws Exception {
        mockMvc.perform(as(ADMIN, post(SEGMENTS)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "muñeca"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.CATALOG_ITEM_DUPLICATE.name()));
    }

    @Test
    void blankNameIsInvalid() throws Exception {
        mockMvc.perform(as(ADMIN, post(SEGMENTS)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "   "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()));
    }

    @Test
    void adminRenamesAnItemEvenChangingOnlyItsCase() throws Exception {
        mockMvc.perform(as(ADMIN, put(SEGMENTS + "/" + wrist.getId())).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "MUÑECA"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("MUÑECA"));
    }

    @Test
    void adminDeactivatesAnItem() throws Exception {
        mockMvc.perform(as(ADMIN, patch(SEGMENTS + "/" + wrist.getId() + "/active"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void therapistsCannotChangeCatalogs() throws Exception {
        mockMvc.perform(as(THERAPIST, post(SEGMENTS)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Antebrazo"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.ACCESS_DENIED.name()));
    }

    @Test
    void unknownItemIsNotFound() throws Exception {
        mockMvc.perform(as(ADMIN, put(SEGMENTS + "/999999")).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Algo"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.CATALOG_ITEM_NOT_FOUND.name()));
    }

    @Test
    void anonymousRequestsAreUnauthorized() throws Exception {
        mockMvc.perform(get(SEGMENTS)).andExpect(status().isUnauthorized());
    }

    private MockHttpServletRequestBuilder as(AuthenticatedUser user, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.issue(user).value());
    }

    private String json(Map<String, ?> body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }
}
