package com.tomanagement.template.rules;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import com.tomanagement.common.exception.BusinessRuleException;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.template.condition.AnswerValue;
import com.tomanagement.template.entity.AnswerType;
import com.tomanagement.template.entity.Question;

import org.springframework.stereotype.Component;

/**
 * Option questions need at least two distinct (case-insensitive) labels; a child compares against one of them.
 */
@Component
public class OptionRules implements AnswerTypeRules {

    @Override
    public AnswerType type() {
        return AnswerType.OPTION;
    }

    @Override
    public boolean usesOptions() {
        return true;
    }

    @Override
    public void validateShape(BigDecimal minValue, BigDecimal maxValue, List<String> optionLabels) {
        if (optionLabels.size() < QuestionLimit.MINIMUM_OPTIONS.getValue()) {
            throw new BusinessRuleException(ErrorCode.QUESTION_OPTIONS_REQUIRED);
        }
        Set<String> seen = new HashSet<>();
        for (String label : optionLabels) {
            if (!seen.add(label.toLowerCase(Locale.ROOT))) {
                throw new BusinessRuleException(ErrorCode.QUESTION_OPTIONS_DUPLICATE);
            }
        }
    }

    @Override
    public Optional<String> canonicalExpectedValue(String rawValue, Question parent) {
        String value = rawValue.trim();
        return parent.getOptionLabels().stream().filter(label -> label.equalsIgnoreCase(value)).findFirst();
    }

    /**
     * The answer is stored with the option's own label (its canonical spelling).
     */
    @Override
    public Optional<AnswerValue> read(RawAnswer raw, Question question) {
        if (!raw.carriesOnly(AnswerType.OPTION)) {
            return Optional.empty();
        }
        return canonicalExpectedValue(raw.optionValue(), question).map(AnswerValue::ofOption);
    }
}
