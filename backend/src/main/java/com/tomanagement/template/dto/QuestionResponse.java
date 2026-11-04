package com.tomanagement.template.dto;

import java.math.BigDecimal;
import java.util.List;

import com.tomanagement.template.entity.AnswerType;

/**
 * A question as shown in the template editor.
 *
 * @param number        position in tree order, starting at 1
 * @param depth         0 for top-level questions, +1 per chaining level
 * @param locked        it has recorded answers: it can only be (de)activated
 * @param hasDependents other questions are chained to it: its type and options cannot change, nor can it be deleted
 */
public record QuestionResponse(
        Long id,
        int number,
        int depth,
        int displayOrder,
        String text,
        AnswerType answerType,
        BigDecimal minValue,
        BigDecimal maxValue,
        List<String> options,
        Long parentQuestionId,
        ConditionDto condition,
        boolean active,
        boolean locked,
        boolean hasDependents) {
}
