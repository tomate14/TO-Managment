package com.tomanagement.followup.validation;

import java.math.BigDecimal;

/**
 * Allowed values of the fixed metrics of a segment evaluation (H6.2), inclusive. The backend is the only source of
 * these limits: the form shows the messages it returns under each field.
 */
public enum ClinicalRange {
    EDEMA("edema", "Edema", BigDecimal.ZERO, BigDecimal.valueOf(50)),
    PAIN("pain", "Dolor", BigDecimal.ZERO, BigDecimal.TEN),
    STRENGTH("strength", "Fuerza", BigDecimal.ZERO, BigDecimal.valueOf(100));

    private final String fieldName;
    private final String label;
    private final BigDecimal minimum;
    private final BigDecimal maximum;

    ClinicalRange(String fieldName, String label, BigDecimal minimum, BigDecimal maximum) {
        this.fieldName = fieldName;
        this.label = label;
        this.minimum = minimum;
        this.maximum = maximum;
    }

    public boolean contains(BigDecimal value) {
        return value.compareTo(minimum) >= 0 && value.compareTo(maximum) <= 0;
    }

    /**
     * @return the request property this range checks (inside each evaluation)
     */
    public String getFieldName() {
        return fieldName;
    }

    public String outOfRangeMessage() {
        return label + " fuera del rango permitido.";
    }
}
