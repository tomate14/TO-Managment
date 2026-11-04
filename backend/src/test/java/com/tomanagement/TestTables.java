package com.tomanagement;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Empties tables between integration tests, following foreign keys (TRUNCATE … CASCADE), so test classes can run
 * in any order.
 */
@TestComponent
public class TestTables {

    private final JdbcTemplate jdbcTemplate;
    private final String schema;

    public TestTables(JdbcTemplate jdbcTemplate, @Value("${APP_DB_SCHEMA}") String schema) {
        this.jdbcTemplate = jdbcTemplate;
        this.schema = schema;
    }

    public void truncate(TestTable... tables) {
        StringBuilder names = new StringBuilder();
        for (TestTable table : tables) {
            if (!names.isEmpty()) {
                names.append(", ");
            }
            names.append(schema).append('.').append(table.getTableName());
        }
        jdbcTemplate.execute("TRUNCATE " + names + " RESTART IDENTITY CASCADE");
    }
}
