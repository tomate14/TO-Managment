package com.tomanagement.template.service;

import org.springframework.stereotype.Component;

/**
 * Answers do not exist until É6 (follow-ups), so no question is locked yet. É6 replaces this bean with one backed
 * by the answers table.
 */
@Component
public class NoRecordedAnswersLookup implements QuestionAnswerLookup {

    @Override
    public boolean hasAnswers(Long questionId) {
        return false;
    }
}
