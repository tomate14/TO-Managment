package com.tomanagement.template.rules;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;

import com.tomanagement.template.entity.AnswerType;

/**
 * An answer as it arrives, before knowing whether it fits its question: at most one value should be present.
 */
public record RawAnswer(BigDecimal numericValue, Boolean booleanValue, String optionValue, String textValue) {

    /**
     * @return true when the value of {@code type} is present and no other value is
     */
    public boolean carriesOnly(AnswerType type) {
        return presentTypes().equals(EnumSet.of(type));
    }

    private Set<AnswerType> presentTypes() {
        Set<AnswerType> present = EnumSet.noneOf(AnswerType.class);
        if (numericValue != null) {
            present.add(AnswerType.NUMERIC);
        }
        if (booleanValue != null) {
            present.add(AnswerType.YES_NO);
        }
        if (optionValue != null) {
            present.add(AnswerType.OPTION);
        }
        if (textValue != null) {
            present.add(AnswerType.TEXT);
        }
        return present;
    }
}
