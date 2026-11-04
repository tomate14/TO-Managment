package com.tomanagement.template.service;

/**
 * Tells whether a question already has recorded answers (then it can only be deactivated, H3.4).
 * Answers arrive with É6; until then {@link NoRecordedAnswersLookup} is the implementation.
 */
public interface QuestionAnswerLookup {

    boolean hasAnswers(Long questionId);
}
