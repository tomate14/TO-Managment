package com.tomanagement.template.service;

/**
 * Tells whether a question already has recorded answers (then it can only be deactivated, H3.4).
 * Implemented by the follow-up feature, which owns the answers.
 */
public interface QuestionAnswerLookup {

    boolean hasAnswers(Long questionId);
}
