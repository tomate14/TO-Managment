package com.tomanagement.template.rules;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.tomanagement.template.entity.AnswerType;

import org.springframework.stereotype.Component;

/**
 * Factory for the answer-type strategies; every {@link AnswerTypeRules} bean registers itself by type.
 */
@Component
public class AnswerTypeRulesFactory {

    private final Map<AnswerType, AnswerTypeRules> rulesByType = new EnumMap<>(AnswerType.class);

    public AnswerTypeRulesFactory(List<AnswerTypeRules> rules) {
        rules.forEach(rule -> rulesByType.put(rule.type(), rule));
    }

    public AnswerTypeRules forType(AnswerType type) {
        return rulesByType.get(type);
    }
}
