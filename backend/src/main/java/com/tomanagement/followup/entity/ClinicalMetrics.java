package com.tomanagement.followup.entity;

import java.math.BigDecimal;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * The fixed metrics of a segment evaluation (H6.2). Edema may be omitted; pain and strength are required. Ranges are
 * checked before building it (see {@code ClinicalRange}). rom: legacy fixed ROM (before É11); null for new
 * evaluations, whose ROM is recorded as segment metric values.
 */
@Embeddable
public class ClinicalMetrics {

    @Column(precision = 6, scale = 2)
    private BigDecimal edema;

    @Column(nullable = false)
    private int pain;

    @Column(precision = 6, scale = 2)
    private BigDecimal rom;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal strength;

    protected ClinicalMetrics() {
        // Required by JPA.
    }

    public ClinicalMetrics(BigDecimal edema, int pain, BigDecimal strength) {
        this(edema, pain, null, strength);
    }

    /**
     * Keeps a legacy fixed ROM (evaluations recorded before É11).
     */
    public ClinicalMetrics(BigDecimal edema, int pain, BigDecimal rom, BigDecimal strength) {
        this.edema = edema;
        this.pain = pain;
        this.rom = rom;
        this.strength = Objects.requireNonNull(strength, "strength is required");
    }

    /**
     * @return the edema, or null when it was omitted
     */
    public BigDecimal getEdema() {
        return edema;
    }

    public int getPain() {
        return pain;
    }

    /**
     * @return the legacy fixed ROM, or null for evaluations recorded with segment metrics
     */
    public BigDecimal getRom() {
        return rom;
    }

    public BigDecimal getStrength() {
        return strength;
    }
}
