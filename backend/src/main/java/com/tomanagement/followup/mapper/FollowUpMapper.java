package com.tomanagement.followup.mapper;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.tomanagement.catalog.dto.CatalogItemResponse;
import com.tomanagement.followup.dto.FollowUpResponse;
import com.tomanagement.followup.dto.FollowUpResponse.AnswerResponse;
import com.tomanagement.followup.dto.FollowUpResponse.SegmentEvaluationResponse;
import com.tomanagement.followup.dto.FollowUpResponse.TemplateRef;
import com.tomanagement.followup.entity.Answer;
import com.tomanagement.followup.entity.ClinicalMetrics;
import com.tomanagement.followup.entity.FollowUp;
import com.tomanagement.followup.entity.SegmentEvaluation;
import com.tomanagement.template.entity.Question;
import com.tomanagement.template.tree.QuestionNode;

import org.springframework.stereotype.Component;

/**
 * Entity → DTO mapping for follow-ups. Answers are sorted in the template tree order, with their depth, using every
 * question of the template (inactive ones included, so old answers keep their question text).
 */
@Component
public class FollowUpMapper {

    /**
     * @param questionsByTemplate every question of the templates the follow-up answered, by template id
     */
    public FollowUpResponse toResponse(FollowUp followUp, Map<Long, List<Question>> questionsByTemplate) {
        List<SegmentEvaluationResponse> evaluations = followUp.getEvaluations().stream()
                .map(evaluation -> toResponse(evaluation, questionsByTemplate))
                .toList();
        return new FollowUpResponse(followUp.getId(), followUp.getDiagnosis().getId(), followUp.getDate(),
                followUp.getScarType().map(CatalogItemResponse::from).orElse(null), evaluations);
    }

    private SegmentEvaluationResponse toResponse(SegmentEvaluation evaluation,
            Map<Long, List<Question>> questionsByTemplate) {
        ClinicalMetrics metrics = evaluation.getMetrics();
        TemplateRef template = evaluation.getTemplate()
                .map(item -> new TemplateRef(item.getId(), item.getName()))
                .orElse(null);
        List<AnswerResponse> answers = template == null
                ? List.of()
                : toAnswers(evaluation.getAnswers(), questionsByTemplate.getOrDefault(template.id(), List.of()));
        return new SegmentEvaluationResponse(evaluation.getId(), CatalogItemResponse.from(evaluation.getSegment()),
                metrics.getEdema(), metrics.getPain(), metrics.getRom(), metrics.getStrength(), template, answers);
    }

    private static List<AnswerResponse> toAnswers(List<Answer> answers, List<Question> templateQuestions) {
        Map<Long, TreePosition> positions = new HashMap<>();
        AtomicInteger order = new AtomicInteger();
        QuestionNode.walk(QuestionNode.forest(templateQuestions), (question, depth) ->
                positions.put(question.getId(), new TreePosition(question, order.getAndIncrement(), depth)));
        return answers.stream()
                .filter(answer -> positions.containsKey(answer.getQuestion().getId()))
                .sorted(Comparator.comparingInt(answer -> positions.get(answer.getQuestion().getId()).order()))
                .map(answer -> toAnswer(answer, positions.get(answer.getQuestion().getId())))
                .toList();
    }

    private static AnswerResponse toAnswer(Answer answer, TreePosition position) {
        Question question = position.question();
        return new AnswerResponse(question.getId(), question.getText(), position.depth(), question.getAnswerType(),
                answer.getNumericValue(), answer.getBooleanValue(), answer.getOptionValue(), answer.getTextValue());
    }

    private record TreePosition(Question question, int order, int depth) {
    }
}
