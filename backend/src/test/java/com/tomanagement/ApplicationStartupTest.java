package com.tomanagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ApplicationStartupTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${APP_DB_SCHEMA}")
    private String appSchema;

    @Test
    void healthEndpointReportsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void flywayCreatesApplicationSchemaAndAppliesFirstMigration() {
        Integer schemaCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.schemata where schema_name = ?",
                Integer.class,
                appSchema);
        assertThat(schemaCount).isEqualTo(1);

        Boolean firstMigrationApplied = jdbcTemplate.queryForObject(
                "select success from " + appSchema + ".flyway_schema_history where version = '1'",
                Boolean.class);
        assertThat(firstMigrationApplied).isTrue();
    }
}
