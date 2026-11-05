package com.tomanagement;

/**
 * Tables integration tests may empty with {@link TestTables}.
 */
public enum TestTable {
    BODY_SEGMENT("body_segment"),
    SURGERY_TYPE("surgery_type"),
    SCAR_TYPE("scar_type"),
    EVALUATION_TEMPLATE("evaluation_template"),
    PATIENT("patient"),
    FOLLOW_UP("follow_up"),
    DIAGNOSIS("diagnosis"),
    TREATMENT("treatment");

    private final String tableName;

    TestTable(String tableName) {
        this.tableName = tableName;
    }

    public String getTableName() {
        return tableName;
    }
}
