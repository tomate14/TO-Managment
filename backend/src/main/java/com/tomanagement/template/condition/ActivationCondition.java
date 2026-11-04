package com.tomanagement.template.condition;

import java.util.Optional;

/**
 * Specification pattern: decides whether a chained question is shown for the parent's answer. An unanswered
 * parent never activates its children.
 */
public interface ActivationCondition {

    boolean isSatisfiedBy(Optional<AnswerValue> parentAnswer);
}
