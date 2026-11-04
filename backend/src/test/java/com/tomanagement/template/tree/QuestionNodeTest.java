package com.tomanagement.template.tree;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import com.tomanagement.catalog.entity.BodySegment;
import com.tomanagement.template.entity.ActivationConditionData;
import com.tomanagement.template.entity.AnswerType;
import com.tomanagement.template.entity.ConditionOperator;
import com.tomanagement.template.entity.EvaluationTemplate;
import com.tomanagement.template.entity.Question;
import com.tomanagement.template.entity.QuestionDefinition;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class QuestionNodeTest {

    private final EvaluationTemplate template = new EvaluationTemplate("Fractura de muñeca", new BodySegment("Muñeca"));

    @Test
    void walksEachChildRightAfterItsParentWithItsDepth() {
        Question splint = question(1L, 1, "¿Usa férula?", null);
        Question grip = question(3L, 3, "Tipo de agarre", null);
        Question hours = question(2L, 2, "Horas por día", splint);
        Question night = question(5L, 5, "¿También de noche?", hours);
        Question notes = question(4L, 4, "Observaciones", null);

        List<String> visited = new ArrayList<>();
        QuestionNode.walk(QuestionNode.forest(List.of(notes, night, grip, hours, splint)),
                (question, depth) -> visited.add(depth + ":" + question.getText()));

        assertThat(visited).containsExactly(
                "0:¿Usa férula?", "1:Horas por día", "2:¿También de noche?", "0:Tipo de agarre", "0:Observaciones");
    }

    @Test
    void aQuestionWhoseParentIsMissingBecomesARoot() {
        Question orphanParent = question(9L, 1, "Fuera de la lista", null);
        Question child = question(2L, 2, "Hija", orphanParent);

        List<QuestionNode> forest = QuestionNode.forest(List.of(child));

        assertThat(forest).extracting(node -> node.getQuestion().getText()).containsExactly("Hija");
    }

    private Question question(Long id, int order, String text, Question parent) {
        Question question = new Question(template, order);
        question.define(new QuestionDefinition(text, AnswerType.YES_NO, null, null, List.of(), parent,
                parent == null ? null : new ActivationConditionData(ConditionOperator.EQUALS, "true")));
        ReflectionTestUtils.setField(question, "id", id);
        return question;
    }
}
