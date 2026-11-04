package com.tomanagement.common.pagination;

/**
 * Page sizes for paginated lists.
 */
public enum PaginationLimit {
    DEFAULT_PAGE_SIZE(20),
    MAX_PAGE_SIZE(50);

    private final int value;

    PaginationLimit(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    /**
     * Clamps a requested page size to 1..MAX_PAGE_SIZE (null → default).
     */
    public static int clampPageSize(Integer requested) {
        if (requested == null) {
            return DEFAULT_PAGE_SIZE.value;
        }
        return Math.max(1, Math.min(requested, MAX_PAGE_SIZE.value));
    }
}
