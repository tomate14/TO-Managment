package com.tomanagement.template.rules;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.tomanagement.common.exception.BusinessRuleException;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.template.condition.AnswerValue;
import com.tomanagement.template.entity.AnswerType;

import org.springframework.stereotype.Component;

@Component
public class NumericRules implements AnswerTypeRules {

    @Override
    public AnswerType type() {
        return AnswerType.NUMERIC;
    }

    @Override
    public boolean usesRange() {
        return true;
    }

    @Override
    public void validateShape(BigDecimal minValue, BigDecimal maxValue, List<String> optionLabels) {
        if (minValue != null && maxValue != null && minValue.compareTo(maxValue) > 0) {
            throw new BusinessRuleException(ErrorCode.QUESTION_RANGE_INVALID);
        }
    }

    @Override
    public Optional<String> canonicalExpectedValue(String rawValue, AnswerConstraints constraints) {
        try {
            return Optional.of(new BigDecimal(rawValue.trim()).stripTrailingZeros().toPlainString());
        } catch (NumberFormatException notANumber) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<AnswerValue> read(RawAnswer raw, AnswerConstraints constraints) {
        if (!raw.carriesOnly(AnswerType.NUMERIC)) {
            return Optional.empty();
        }
        BigDecimal value = raw.numericValue();
        boolean aboveMinimum = constraints.getMinValue() == null || value.compareTo(constraints.getMinValue()) >= 0;
        boolean belowMaximum = constraints.getMaxValue() == null || value.compareTo(constraints.getMaxValue()) <= 0;
        return aboveMinimum && belowMaximum ? Optional.of(AnswerValue.ofNumber(value)) : Optional.empty();
    }
}
