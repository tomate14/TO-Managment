package com.tomanagement;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

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
        Set<TestTable> requested = new LinkedHashSet<>(Arrays.asList(tables));
        Set<TestTable> ordered = new LinkedHashSet<>();
        if (requested.contains(TestTable.BILLING) || requested.contains(TestTable.PATIENT)
                || requested.contains(TestTable.DIAGNOSIS) || requested.contains(TestTable.ESTABLISHMENT)) {
            ordered.add(TestTable.BILLING_APPOINTMENT);
            ordered.add(TestTable.BILLING_FOLLOW_UP);
            ordered.add(TestTable.BILLING);
        }
        if (requested.contains(TestTable.APPOINTMENT)) {
            ordered.add(TestTable.BILLING_APPOINTMENT);
        }
        if (requested.contains(TestTable.FOLLOW_UP)) {
            ordered.add(TestTable.BILLING_FOLLOW_UP);
        }
        ordered.addAll(requested);
        StringBuilder names = new StringBuilder();
        for (TestTable table : ordered) {
            if (!names.isEmpty()) {
                names.append(", ");
            }
            names.append(schema).append('.').append(table.getTableName());
        }
        jdbcTemplate.execute("TRUNCATE " + names + " RESTART IDENTITY CASCADE");
    }
}
