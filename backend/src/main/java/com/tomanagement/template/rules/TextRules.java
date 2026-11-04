package com.tomanagement.template.rules;

import java.util.Optional;

import com.tomanagement.template.entity.AnswerType;
import com.tomanagement.template.entity.Question;

import org.springframework.stereotype.Component;

@Component
public class TextRules implements AnswerTypeRules {

    @Override
    public AnswerType type() {
        return AnswerType.TEXT;
    }

    @Override
    public Optional<String> canonicalExpectedValue(String rawValue, Question parent) {
        String value = rawValue.trim();
        return value.isEmpty() ? Optional.empty() : Optional.of(value);
    }
}
