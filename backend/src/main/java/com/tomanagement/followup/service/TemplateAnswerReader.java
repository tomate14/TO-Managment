package com.tomanagement.followup.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.tomanagement.common.exception.BusinessRuleException;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.common.exception.ResourceNotFoundException;
import com.tomanagement.followup.dto.AnswerRequest;
import com.tomanagement.template.condition.AnswerValue;
import com.tomanagement.template.entity.EvaluationTemplate;
import com.tomanagement.template.entity.Question;
import com.tomanagement.template.repository.EvaluationTemplateRepository;
import com.tomanagement.template.repository.QuestionRepository;
import com.tomanagement.template.rules.AnswerTypeRulesFactory;
import com.tomanagement.template.tree.VisibleQuestionResolver;

import org.springframework.stereotype.Component;

/**
 * Reads the answers to a template in a segment evaluation (H6.3), re-checking what the form already enforces: the
 * template is active and belongs to the segment, each answer is valid for its question type
 * ({@link com.tomanagement.template.rules.AnswerTypeRules}), and only visible questions are answered
 * ({@link VisibleQuestionResolver}). Used by the validation chain and, once valid, to build the answers.
 */
@Component
public class TemplateAnswerReader {

    private final EvaluationTemplateRepository templateRepository;
    private final QuestionRepository questionRepository;
    private final AnswerTypeRulesFactory rulesFactory;

    public TemplateAnswerReader(EvaluationTemplateRepository templateRepository, QuestionRepository questionRepository,
            AnswerTypeRulesFactory rulesFactory) {
        this.templateRepository = templateRepository;
        this.questionRepository = questionRepository;
        this.rulesFactory = rulesFactory;
    }

    /**
     * @throws ResourceNotFoundException when the template does not exist
     * @throws BusinessRuleException     when the template is not available or an answer is not acceptable
     */
    public TemplateAnswers read(Long segmentId, Long templateId, List<AnswerRequest> answers) {
        EvaluationTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TEMPLATE_NOT_FOUND));
        if (!template.isActive() || !template.getSegment().getId().equals(segmentId)) {
            throw new BusinessRuleException(ErrorCode.TEMPLATE_NOT_AVAILABLE);
        }
        List<Question> questions = questionRepository.findByTemplate_IdOrderByDisplayOrderAsc(templateId);
        Map<Long, Question> questionsById = questions.stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        Map<Long, AnswerValue> valuesById = new LinkedHashMap<>();
        for (AnswerRequest answer : answers) {
            Question question = Optional.ofNullable(questionsById.get(answer.questionId()))
                    .orElseThrow(() -> new BusinessRuleException(ErrorCode.ANSWER_QUESTION_INVALID));
            if (valuesById.containsKey(question.getId())) {
                throw new BusinessRuleException(ErrorCode.ANSWER_QUESTION_INVALID);
            }
            AnswerValue value = rulesFactory.forType(question.getAnswerType()).read(answer.toRawAnswer(), question)
                    .orElseThrow(() -> new BusinessRuleException(ErrorCode.ANSWER_VALUE_INVALID));
            valuesById.put(question.getId(), value);
        }

        Set<Long> visible = VisibleQuestionResolver.visibleQuestionIds(questions, valuesById);
        if (!visible.containsAll(valuesById.keySet())) {
            throw new BusinessRuleException(ErrorCode.ANSWER_QUESTION_INVALID);
        }
        Map<Question, AnswerValue> byQuestion = new LinkedHashMap<>();
        valuesById.forEach((questionId, value) -> byQuestion.put(questionsById.get(questionId), value));
        return new TemplateAnswers(template, byQuestion);
    }

    /**
     * A valid template with its typed answers, in the order they were sent.
     */
    public record TemplateAnswers(EvaluationTemplate template, Map<Question, AnswerValue> answers) {

        public TemplateAnswers {
            Objects.requireNonNull(template, "template is required");
            answers = new LinkedHashMap<>(answers);
        }
    }
}
