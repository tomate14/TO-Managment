package com.tomanagement.template.condition;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import com.tomanagement.template.entity.AnswerType;

/**
 * A typed answer to a question, as seen by activation conditions. Exactly one value matches the type.
 */
public final class AnswerValue {

    private final AnswerType type;
    private final BigDecimal number;
    private final Boolean yesNo;
    private final String text;

    private AnswerValue(AnswerType type, BigDecimal number, Boolean yesNo, String text) {
        this.type = type;
        this.number = number;
        this.yesNo = yesNo;
        this.text = text;
    }

    public static AnswerValue ofNumber(BigDecimal number) {
        return new AnswerValue(AnswerType.NUMERIC, Objects.requireNonNull(number, "number is required"), null, null);
    }

    public static AnswerValue ofYesNo(boolean yesNo) {
        return new AnswerValue(AnswerType.YES_NO, null, yesNo, null);
    }

    public static AnswerValue ofOption(String label) {
        return new AnswerValue(AnswerType.OPTION, null, null, Objects.requireNonNull(label, "label is required"));
    }

    public static AnswerValue ofText(String text) {
        return new AnswerValue(AnswerType.TEXT, null, null, Objects.requireNonNull(text, "text is required"));
    }

    public AnswerType getType() {
        return type;
    }

    /**
     * @return the number, only for NUMERIC answers
     */
    public Optional<BigDecimal> number() {
        return Optional.ofNullable(number);
    }

    /**
     * @return the value, only for YES_NO answers
     */
    public Optional<Boolean> yesNo() {
        return Optional.ofNullable(yesNo);
    }

    /**
     * @return the label or text, only for OPTION and TEXT answers
     */
    public Optional<String> text() {
        return Optional.ofNullable(text);
    }

    /**
     * Compares with a condition's canonical expected value: numbers by value, yes/no by "true"/"false", options and
     * text ignoring case and surrounding spaces.
     */
    public boolean matches(String expectedValue) {
        return switch (type) {
            case NUMERIC -> parseNumber(expectedValue).map(expected -> number.compareTo(expected) == 0).orElse(false);
            case YES_NO -> yesNo.toString().equalsIgnoreCase(expectedValue.trim());
            case OPTION, TEXT -> normalize(text).equals(normalize(expectedValue));
        };
    }

    static Optional<BigDecimal> parseNumber(String raw) {
        try {
            return Optional.of(new BigDecimal(raw.trim()));
        } catch (NumberFormatException notANumber) {
            return Optional.empty();
        }
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
