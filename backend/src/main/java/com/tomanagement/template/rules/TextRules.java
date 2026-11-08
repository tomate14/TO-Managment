package com.tomanagement.template.rules;

import java.util.Optional;

import com.tomanagement.template.condition.AnswerValue;
import com.tomanagement.template.entity.AnswerType;

import org.springframework.stereotype.Component;

@Component
public class TextRules implements AnswerTypeRules {

    @Override
    public AnswerType type() {
        return AnswerType.TEXT;
    }

    @Override
    public Optional<String> canonicalExpectedValue(String rawValue, AnswerConstraints constraints) {
        String value = rawValue.trim();
        return value.isEmpty() ? Optional.empty() : Optional.of(value);
    }

    @Override
    public Optional<AnswerValue> read(RawAnswer raw, AnswerConstraints constraints) {
        if (!raw.carriesOnly(AnswerType.TEXT)) {
            return Optional.empty();
        }
        String value = raw.textValue().trim();
        boolean fits = !value.isEmpty() && value.length() <= QuestionLimit.ANSWER_TEXT_MAX_LENGTH.getValue();
        return fits ? Optional.of(AnswerValue.ofText(value)) : Optional.empty();
    }
}
