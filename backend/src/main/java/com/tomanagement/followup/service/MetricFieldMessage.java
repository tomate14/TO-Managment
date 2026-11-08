package com.tomanagement.followup.service;

/**
 * Messages shown under a segment ROM metric of the follow-up form (É11).
 */
enum MetricFieldMessage {
    REQUIRED("Completá esta métrica."),
    INVALID("El valor no es válido para esta métrica.");

    private final String text;

    MetricFieldMessage(String text) {
        this.text = text;
    }

    String getText() {
        return text;
    }
}
