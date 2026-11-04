package com.tomanagement.template.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import com.tomanagement.common.exception.BusinessRuleException;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.common.exception.ResourceNotFoundException;
import com.tomanagement.template.dto.ConditionDto;
import com.tomanagement.template.dto.QuestionRequest;
import com.tomanagement.template.dto.TemplateDetailResponse;
import com.tomanagement.template.entity.ActivationConditionData;
import com.tomanagement.template.entity.EvaluationTemplate;
import com.tomanagement.template.entity.Question;
import com.tomanagement.template.entity.QuestionDefinition;
import com.tomanagement.template.mapper.TemplateMapper;
import com.tomanagement.template.repository.EvaluationTemplateRepository;
import com.tomanagement.template.repository.QuestionRepository;
import com.tomanagement.template.rules.AnswerTypeRules;
import com.tomanagement.template.rules.AnswerTypeRulesFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionServiceImpl implements QuestionService {

    private final EvaluationTemplateRepository templateRepository;
    private final QuestionRepository questionRepository;
    private final AnswerTypeRulesFactory rulesFactory;
    private final QuestionAnswerLookup answerLookup;
    private final TemplateMapper mapper;

    public QuestionServiceImpl(EvaluationTemplateRepository templateRepository, QuestionRepository questionRepository,
            AnswerTypeRulesFactory rulesFactory, QuestionAnswerLookup answerLookup, TemplateMapper mapper) {
        this.templateRepository = templateRepository;
        this.questionRepository = questionRepository;
        this.rulesFactory = rulesFactory;
        this.answerLookup = answerLookup;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public TemplateDetailResponse create(Long templateId, QuestionRequest request) {
        EvaluationTemplate template = findTemplate(templateId);
        Question question = new Question(template, questionRepository.maxDisplayOrder(templateId) + 1);
        question.define(definition(request, question));
        questionRepository.saveAndFlush(question);
        return detail(template);
    }

    @Override
    @Transactional
    public TemplateDetailResponse update(Long templateId, Long questionId, QuestionRequest request) {
        Question question = findQuestion(templateId, questionId);
        if (answerLookup.hasAnswers(questionId)) {
            throw new BusinessRuleException(ErrorCode.QUESTION_LOCKED);
        }
        QuestionDefinition definition = definition(request, question);
        if (questionRepository.existsByParent_Id(questionId) && changesWhatChildrenDependOn(question, definition)) {
            throw new BusinessRuleException(ErrorCode.QUESTION_HAS_DEPENDENTS);
        }
        question.define(definition);
        questionRepository.saveAndFlush(question);
        return detail(question.getTemplate());
    }

    @Override
    @Transactional
    public TemplateDetailResponse changeActive(Long templateId, Long questionId, boolean active) {
        Question question = findQuestion(templateId, questionId);
        question.changeActive(active);
        return detail(question.getTemplate());
    }

    @Override
    @Transactional
    public void delete(Long templateId, Long questionId) {
        Question question = findQuestion(templateId, questionId);
        if (answerLookup.hasAnswers(questionId)) {
            throw new BusinessRuleException(ErrorCode.QUESTION_LOCKED);
        }
        if (questionRepository.existsByParent_Id(questionId)) {
            throw new BusinessRuleException(ErrorCode.QUESTION_HAS_DEPENDENTS);
        }
        questionRepository.delete(question);
    }

    /**
     * Validates the request against the type rules and the chaining rules (H3.2, H3.3) and normalizes it.
     */
    private QuestionDefinition definition(QuestionRequest request, Question self) {
        AnswerTypeRules rules = rulesFactory.forType(request.answerType());
        BigDecimal minValue = rules.usesRange() ? request.minValue() : null;
        BigDecimal maxValue = rules.usesRange() ? request.maxValue() : null;
        List<String> options = rules.usesOptions() ? request.options() : List.of();
        rules.validateShape(minValue, maxValue, options);

        if (request.parentQuestionId() == null) {
            return new QuestionDefinition(request.text(), request.answerType(), minValue, maxValue, options, null, null);
        }
        Question parent = validParent(request.parentQuestionId(), self);
        ConditionDto condition = request.condition();
        if (condition == null) {
            throw new BusinessRuleException(ErrorCode.CONDITION_VALUE_INVALID);
        }
        if (!condition.operator().supports(parent.getAnswerType())) {
            throw new BusinessRuleException(ErrorCode.CONDITION_OPERATOR_INCOMPATIBLE);
        }
        String expected = rulesFactory.forType(parent.getAnswerType())
                .canonicalExpectedValue(condition.expectedValue(), parent)
                .orElseThrow(() -> new BusinessRuleException(ErrorCode.CONDITION_VALUE_INVALID));
        return new QuestionDefinition(request.text(), request.answerType(), minValue, maxValue, options, parent,
                new ActivationConditionData(condition.operator(), expected));
    }

    /**
     * A parent is an active, earlier question of the same template, which also rules out cycles.
     */
    private Question validParent(Long parentId, Question self) {
        return questionRepository.findById(parentId)
                .filter(parent -> parent.getTemplate().getId().equals(self.getTemplate().getId()))
                .filter(Question::isActive)
                .filter(parent -> parent.getDisplayOrder() < self.getDisplayOrder())
                .orElseThrow(() -> new BusinessRuleException(ErrorCode.CONDITION_PARENT_INVALID));
    }

    private static boolean changesWhatChildrenDependOn(Question current, QuestionDefinition next) {
        return current.getAnswerType() != next.answerType()
                || !normalized(current.getOptionLabels()).equals(normalized(next.optionLabels()));
    }

    private static Set<String> normalized(List<String> labels) {
        return labels.stream().map(label -> label.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
    }

    private TemplateDetailResponse detail(EvaluationTemplate template) {
        return mapper.toDetail(template, questionRepository.findByTemplate_IdOrderByDisplayOrderAsc(template.getId()));
    }

    private EvaluationTemplate findTemplate(Long templateId) {
        return templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TEMPLATE_NOT_FOUND));
    }

    private Question findQuestion(Long templateId, Long questionId) {
        return questionRepository.findById(questionId)
                .filter(question -> question.getTemplate().getId().equals(templateId))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.QUESTION_NOT_FOUND));
    }
}
