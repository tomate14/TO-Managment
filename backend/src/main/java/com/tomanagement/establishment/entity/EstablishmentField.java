package com.tomanagement.establishment.entity;

/**
 * Size limits of the establishment columns, so validation and name generation share one source.
 */
public enum EstablishmentField {

    NAME(120);

    private final int maxLength;

    EstablishmentField(int maxLength) {
        this.maxLength = maxLength;
    }

    public int maxLength() {
        return maxLength;
    }
}
