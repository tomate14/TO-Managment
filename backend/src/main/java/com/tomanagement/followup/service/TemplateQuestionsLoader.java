package com.tomanagement.followup.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.tomanagement.followup.entity.FollowUp;
import com.tomanagement.followup.entity.SegmentEvaluation;
import com.tomanagement.template.entity.EvaluationTemplate;
import com.tomanagement.template.entity.Question;
import com.tomanagement.template.repository.QuestionRepository;

import org.springframework.stereotype.Component;

/**
 * Loads, in one query, every question of the templates answered in some follow-ups, grouped by template id (what
 * {@link com.tomanagement.followup.mapper.FollowUpMapper} needs).
 */
@Component
public class TemplateQuestionsLoader {

    private final QuestionRepository questionRepository;

    public TemplateQuestionsLoader(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public Map<Long, List<Question>> questionsByTemplate(Collection<FollowUp> followUps) {
        List<Long> templateIds = followUps.stream()
                .flatMap(followUp -> followUp.getEvaluations().stream())
                .map(SegmentEvaluation::getTemplate)
                .flatMap(Optional::stream)
                .map(EvaluationTemplate::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (templateIds.isEmpty()) {
            return Map.of();
        }
        return questionRepository.findByTemplate_IdIn(templateIds).stream()
                .collect(Collectors.groupingBy(question -> question.getTemplate().getId()));
    }
}
