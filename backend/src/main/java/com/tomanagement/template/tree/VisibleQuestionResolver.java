package com.tomanagement.template.tree;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.tomanagement.template.condition.ActivationConditionFactory;
import com.tomanagement.template.condition.AnswerValue;
import com.tomanagement.template.entity.Question;

/**
 * Decides which questions of a template are shown for a set of answers (spec §3.5): a question is shown when it is
 * active and either has no parent, or its parent is shown, answered, and satisfies the activation condition. The
 * child of an inactive question is never shown, since its parent can no longer be answered. The follow-up form
 * applies the same rule (frontend {@code question-visibility.ts}).
 */
public final class VisibleQuestionResolver {

    private VisibleQuestionResolver() {
    }

    /**
     * @param questions every question of the template, active and inactive
     * @param answers   typed answers by question id
     * @return ids of the questions that are shown
     */
    public static Set<Long> visibleQuestionIds(List<Question> questions, Map<Long, AnswerValue> answers) {
        Set<Long> visible = new HashSet<>();
        QuestionNode.forest(questions).forEach(root -> visit(root, true, answers, visible));
        return visible;
    }

    private static void visit(QuestionNode node, boolean reachable, Map<Long, AnswerValue> answers, Set<Long> visible) {
        Question question = node.getQuestion();
        boolean shown = reachable && question.isActive();
        if (shown) {
            visible.add(question.getId());
        }
        Optional<AnswerValue> answer = shown ? Optional.ofNullable(answers.get(question.getId())) : Optional.empty();
        node.getChildren().forEach(child -> visit(child, shown && activates(child.getQuestion(), answer), answers, visible));
    }

    private static boolean activates(Question child, Optional<AnswerValue> parentAnswer) {
        return child.getCondition()
                .map(ActivationConditionFactory::from)
                .map(condition -> condition.isSatisfiedBy(parentAnswer))
                .orElse(true);
    }
}
