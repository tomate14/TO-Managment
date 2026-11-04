package com.tomanagement.template.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.tomanagement.template.dto.ConditionDto;
import com.tomanagement.template.dto.QuestionResponse;
import com.tomanagement.template.dto.SegmentRef;
import com.tomanagement.template.dto.TemplateDetailResponse;
import com.tomanagement.template.dto.TemplateSummaryResponse;
import com.tomanagement.template.entity.EvaluationTemplate;
import com.tomanagement.template.entity.Question;
import com.tomanagement.template.service.QuestionAnswerLookup;
import com.tomanagement.template.tree.QuestionNode;

import org.springframework.stereotype.Component;

/**
 * Entity → DTO mapping for templates; walks the question tree to number and indent the questions.
 */
@Component
public class TemplateMapper {

    private final QuestionAnswerLookup answerLookup;

    public TemplateMapper(QuestionAnswerLookup answerLookup) {
        this.answerLookup = answerLookup;
    }

    public TemplateSummaryResponse toSummary(EvaluationTemplate template, List<Question> questions) {
        int chained = (int) questions.stream().filter(question -> question.getParent().isPresent()).count();
        return new TemplateSummaryResponse(template.getId(), template.getName(), SegmentRef.from(template.getSegment()),
                template.isActive(), questions.size(), chained);
    }

    public TemplateDetailResponse toDetail(EvaluationTemplate template, List<Question> questions) {
        Set<Long> parentsWithChildren = questions.stream()
                .flatMap(question -> question.getParent().stream())
                .map(Question::getId)
                .collect(Collectors.toSet());
        List<QuestionResponse> ordered = new ArrayList<>();
        AtomicInteger number = new AtomicInteger();
        QuestionNode.walk(QuestionNode.forest(questions), (question, depth) -> ordered.add(toResponse(
                question, number.incrementAndGet(), depth, parentsWithChildren.contains(question.getId()))));
        return new TemplateDetailResponse(template.getId(), template.getName(), SegmentRef.from(template.getSegment()),
                template.isActive(), ordered);
    }

    private QuestionResponse toResponse(Question question, int number, int depth, boolean hasDependents) {
        return new QuestionResponse(
                question.getId(),
                number,
                depth,
                question.getDisplayOrder(),
                question.getText(),
                question.getAnswerType(),
                question.getMinValue(),
                question.getMaxValue(),
                question.getOptionLabels(),
                question.getParent().map(Question::getId).orElse(null),
                question.getCondition().map(ConditionDto::from).orElse(null),
                question.isActive(),
                answerLookup.hasAnswers(question.getId()),
                hasDependents);
    }
}
