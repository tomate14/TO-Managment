package com.tomanagement.template.tree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import com.tomanagement.template.entity.Question;

/**
 * Composite pattern: a question with its chained children, so a template's questions can be walked as a tree
 * (each child right after its parent, ordered by displayOrder).
 */
public final class QuestionNode {

    private final Question question;
    private final List<QuestionNode> children = new ArrayList<>();

    private QuestionNode(Question question) {
        this.question = question;
    }

    /**
     * Builds the forest of a template. Questions whose parent is missing from the list become roots.
     */
    public static List<QuestionNode> forest(List<Question> questions) {
        Map<Long, QuestionNode> nodes = new LinkedHashMap<>();
        questions.stream()
                .sorted(Comparator.comparingInt(Question::getDisplayOrder))
                .forEach(question -> nodes.put(question.getId(), new QuestionNode(question)));
        List<QuestionNode> roots = new ArrayList<>();
        nodes.values().forEach(node -> node.question.getParent()
                .map(parent -> nodes.get(parent.getId()))
                .ifPresentOrElse(parentNode -> parentNode.children.add(node), () -> roots.add(node)));
        return roots;
    }

    /**
     * Visits the forest depth-first (pre-order) with each node's depth (roots are 0).
     */
    public static void walk(List<QuestionNode> forest, BiConsumer<Question, Integer> visitor) {
        forest.forEach(root -> root.visit(0, visitor));
    }

    private void visit(int depth, BiConsumer<Question, Integer> visitor) {
        visitor.accept(question, depth);
        children.forEach(child -> child.visit(depth + 1, visitor));
    }

    public Question getQuestion() {
        return question;
    }

    public List<QuestionNode> getChildren() {
        return List.copyOf(children);
    }
}
