package com.tomanagement.establishment.catalog;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Seeds the default catalogs of a new establishment in the caller's transaction (atomic with the establishment and
 * its admin).
 *
 * <p>It uses plain JDBC INSERTs with an explicit establishment_id on purpose: the Hibernate session of the current
 * transaction was opened with the superadmin's tenant (NONE), so saving tenant entities through JPA here would get
 * the wrong tenant. JpaTransactionManager exposes its JDBC connection, so these run in the same transaction as the
 * JPA saves (the service flushes the establishment first, for the foreign key).
 */
@Component
public class DefaultCatalogSeeder {

    private final JdbcTemplate jdbcTemplate;
    private final String schema;

    public DefaultCatalogSeeder(JdbcTemplate jdbcTemplate, @Value("${APP_DB_SCHEMA}") String schema) {
        this.jdbcTemplate = jdbcTemplate;
        this.schema = schema;
    }

    public void seed(Long establishmentId) {
        for (DefaultCatalogEntry entry : DefaultCatalogEntry.values()) {
            jdbcTemplate.update("INSERT INTO " + schema + "." + CatalogTable.of(entry.getType()).getTableName()
                    + " (name, active, establishment_id) VALUES (?, TRUE, ?)", entry.getName(), establishmentId);
        }
    }
}
