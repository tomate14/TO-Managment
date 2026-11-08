package com.tomanagement.template.rules;

import java.util.Locale;
import java.util.Optional;

import com.tomanagement.template.condition.AnswerValue;
import com.tomanagement.template.entity.AnswerType;

import org.springframework.stereotype.Component;

/**
 * Yes/no answers compare as "true" / "false".
 */
@Component
public class YesNoRules implements AnswerTypeRules {

    @Override
    public AnswerType type() {
        return AnswerType.YES_NO;
    }

    @Override
    public Optional<String> canonicalExpectedValue(String rawValue, AnswerConstraints constraints) {
        String value = rawValue.trim().toLowerCase(Locale.ROOT);
        return Boolean.TRUE.toString().equals(value) || Boolean.FALSE.toString().equals(value)
                ? Optional.of(value)
                : Optional.empty();
    }

    @Override
    public Optional<AnswerValue> read(RawAnswer raw, AnswerConstraints constraints) {
        return raw.carriesOnly(AnswerType.YES_NO) ? Optional.of(AnswerValue.ofYesNo(raw.booleanValue())) : Optional.empty();
    }
}
