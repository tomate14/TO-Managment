package com.tomanagement.followup.service;

import com.tomanagement.followup.repository.AnswerRepository;
import com.tomanagement.template.service.QuestionAnswerLookup;

import org.springframework.stereotype.Component;

/**
 * A question with answers in some follow-up can only be deactivated (H3.4).
 */
@Component
public class RecordedAnswersLookup implements QuestionAnswerLookup {

    private final AnswerRepository answerRepository;

    public RecordedAnswersLookup(AnswerRepository answerRepository) {
        this.answerRepository = answerRepository;
    }

    @Override
    public boolean hasAnswers(Long questionId) {
        return answerRepository.existsByQuestion_Id(questionId);
    }
}
