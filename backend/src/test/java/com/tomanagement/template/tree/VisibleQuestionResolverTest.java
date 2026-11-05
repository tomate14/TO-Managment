package com.tomanagement.template.tree;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.tomanagement.catalog.entity.BodySegment;
import com.tomanagement.template.condition.AnswerValue;
import com.tomanagement.template.entity.ActivationConditionData;
import com.tomanagement.template.entity.AnswerType;
import com.tomanagement.template.entity.ConditionOperator;
import com.tomanagement.template.entity.EvaluationTemplate;
import com.tomanagement.template.entity.Question;
import com.tomanagement.template.entity.QuestionDefinition;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class VisibleQuestionResolverTest {

    private final EvaluationTemplate template = new EvaluationTemplate("Fractura de muñeca", new BodySegment("Muñeca"));

    private final Question splint = question(1L, 1, AnswerType.YES_NO, null, null);
    private final Question hours = question(2L, 2, AnswerType.NUMERIC, splint,
            new ActivationConditionData(ConditionOperator.EQUALS, "true"));
    private final Question night = question(3L, 3, AnswerType.YES_NO, hours,
            new ActivationConditionData(ConditionOperator.GREATER_THAN, "12"));
    private final Question notes = question(4L, 4, AnswerType.TEXT, null, null);

    @Test
    void rootsAreShownAndChildrenNeedTheirParentAnswer() {
        assertThat(VisibleQuestionResolver.visibleQuestionIds(all(), Map.of())).containsExactlyInAnyOrder(1L, 4L);
    }

    @Test
    void aSatisfiedConditionShowsTheChildAndChainsFurther() {
        Map<Long, AnswerValue> answers = Map.of(
                1L, AnswerValue.ofYesNo(true),
                2L, AnswerValue.ofNumber(new BigDecimal("16")));

        assertThat(VisibleQuestionResolver.visibleQuestionIds(all(), answers)).containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
    }

    @Test
    void answeringNoHidesTheWholeBranch() {
        Map<Long, AnswerValue> answers = Map.of(
                1L, AnswerValue.ofYesNo(false),
                2L, AnswerValue.ofNumber(new BigDecimal("16")));

        assertThat(VisibleQuestionResolver.visibleQuestionIds(all(), answers)).containsExactlyInAnyOrder(1L, 4L);
    }

    @Test
    void inactiveQuestionsAndTheirChildrenAreHidden() {
        splint.changeActive(false);

        Map<Long, AnswerValue> answers = Map.of(1L, AnswerValue.ofYesNo(true));

        assertThat(VisibleQuestionResolver.visibleQuestionIds(all(), answers)).containsExactly(4L);
    }

    private List<Question> all() {
        return List.of(splint, hours, night, notes);
    }

    private Question question(Long id, int order, AnswerType type, Question parent, ActivationConditionData condition) {
        Question question = new Question(template, order);
        question.define(new QuestionDefinition("Pregunta " + id, type, null, null, List.of(), parent, condition));
        ReflectionTestUtils.setField(question, "id", id);
        return question;
    }
}
