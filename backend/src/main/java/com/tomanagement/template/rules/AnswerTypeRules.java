package com.tomanagement.template.rules;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.tomanagement.template.condition.AnswerValue;
import com.tomanagement.template.entity.AnswerType;
import com.tomanagement.template.entity.Question;

/**
 * Strategy with the rules that depend on a question's answer type: which extra fields it uses, how they are
 * validated, and which expected values a child condition may compare against it.
 */
public interface AnswerTypeRules {

    AnswerType type();

    default boolean usesRange() {
        return false;
    }

    default boolean usesOptions() {
        return false;
    }

    /**
     * Validates range or options (only called with the fields this type uses).
     *
     * @throws com.tomanagement.common.exception.BusinessRuleException when the shape is invalid
     */
    default void validateShape(BigDecimal minValue, BigDecimal maxValue, List<String> optionLabels) {
        // Types without extra fields have nothing to validate.
    }

    /**
     * @param parent a question of this type
     * @return the canonical form of a child condition's expected value, or empty when it is not a valid answer
     */
    Optional<String> canonicalExpectedValue(String rawValue, Question parent);

    /**
     * Reads an answer to a question of this type (H6.3): it must carry only the value of this type, and that value
     * must be valid for the question (range, options, length).
     *
     * @return the typed answer, or empty when the value is not a valid answer
     */
    Optional<AnswerValue> read(RawAnswer raw, Question question);
}
