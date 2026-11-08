package com.tomanagement.template.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import com.tomanagement.catalog.entity.BodySegment;
import com.tomanagement.common.exception.ApplicationException;
import com.tomanagement.common.exception.BusinessRuleException;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.template.entity.AnswerType;
import com.tomanagement.template.entity.ConditionOperator;
import com.tomanagement.template.entity.EvaluationTemplate;
import com.tomanagement.template.entity.Question;
import com.tomanagement.template.entity.QuestionDefinition;

import org.junit.jupiter.api.Test;

class AnswerTypeRulesTest {

    private final AnswerTypeRulesFactory factory = new AnswerTypeRulesFactory(
            List.of(new NumericRules(), new YesNoRules(), new OptionRules(), new TextRules()));

    @Test
    void everyTypeHasRules() {
        for (AnswerType type : AnswerType.values()) {
            assertThat(factory.forType(type).type()).isEqualTo(type);
        }
    }

    @Test
    void onlyNumericUsesRangeAndOnlyOptionUsesOptions() {
        assertThat(factory.forType(AnswerType.NUMERIC).usesRange()).isTrue();
        assertThat(factory.forType(AnswerType.OPTION).usesOptions()).isTrue();
        assertThat(factory.forType(AnswerType.YES_NO).usesRange()).isFalse();
        assertThat(factory.forType(AnswerType.TEXT).usesOptions()).isFalse();
    }

    @Test
    void numericRangeNeedsMinNotAboveMax() {
        AnswerTypeRules numeric = factory.forType(AnswerType.NUMERIC);

        assertThatCode(() -> numeric.validateShape(BigDecimal.ZERO, new BigDecimal("24"), List.of())).doesNotThrowAnyException();
        assertThatCode(() -> numeric.validateShape(null, new BigDecimal("24"), List.of())).doesNotThrowAnyException();
        assertThatThrownBy(() -> numeric.validateShape(new BigDecimal("25"), new BigDecimal("24"), List.of()))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(error -> ((ApplicationException) error).getErrorCode())
                .isEqualTo(ErrorCode.QUESTION_RANGE_INVALID);
    }

    @Test
    void optionsNeedTwoDistinctLabels() {
        AnswerTypeRules option = factory.forType(AnswerType.OPTION);

        assertThatThrownBy(() -> option.validateShape(null, null, List.of("Cilíndrico")))
                .extracting(error -> ((ApplicationException) error).getErrorCode())
                .isEqualTo(ErrorCode.QUESTION_OPTIONS_REQUIRED);
        assertThatThrownBy(() -> option.validateShape(null, null, List.of("Cilíndrico", "cilíndrico")))
                .extracting(error -> ((ApplicationException) error).getErrorCode())
                .isEqualTo(ErrorCode.QUESTION_OPTIONS_DUPLICATE);
        assertThatCode(() -> option.validateShape(null, null, List.of("Cilíndrico", "Esférico"))).doesNotThrowAnyException();
    }

    @Test
    void expectedValuesAreCanonicalForTheParentType() {
        Question optionParent = question(AnswerType.OPTION, List.of("Cilíndrico", "Esférico"));

        assertThat(factory.forType(AnswerType.YES_NO).canonicalExpectedValue(" TRUE ", null)).contains("true");
        assertThat(factory.forType(AnswerType.YES_NO).canonicalExpectedValue("quizás", null)).isEmpty();
        assertThat(factory.forType(AnswerType.NUMERIC).canonicalExpectedValue("7.50", null)).contains("7.5");
        assertThat(factory.forType(AnswerType.NUMERIC).canonicalExpectedValue("siete", null)).isEmpty();
        assertThat(factory.forType(AnswerType.OPTION).canonicalExpectedValue("esférico", optionParent)).contains("Esférico");
        assertThat(factory.forType(AnswerType.OPTION).canonicalExpectedValue("Palmar", optionParent)).isEmpty();
        assertThat(factory.forType(AnswerType.TEXT).canonicalExpectedValue("   ", null)).isEmpty();
    }

    @Test
    void greaterAndLessThanOnlySupportNumericParents() {
        assertThat(ConditionOperator.GREATER_THAN.supports(AnswerType.NUMERIC)).isTrue();
        assertThat(ConditionOperator.GREATER_THAN.supports(AnswerType.YES_NO)).isFalse();
        assertThat(ConditionOperator.LESS_THAN.supports(AnswerType.OPTION)).isFalse();
        assertThat(ConditionOperator.EQUALS.supports(AnswerType.TEXT)).isTrue();
    }

    @Test
    void readsNumericAnswersInsideTheQuestionRange() {
        Question hours = new Question(new EvaluationTemplate("Plantilla", new BodySegment("Muñeca")), 1);
        hours.define(new QuestionDefinition("Horas", AnswerType.NUMERIC, BigDecimal.ZERO, new BigDecimal("24"), List.of(),
                null, null));
        AnswerTypeRules numeric = factory.forType(AnswerType.NUMERIC);

        assertThat(numeric.read(new RawAnswer(new BigDecimal("8"), null, null, null), hours)).isPresent();
        assertThat(numeric.read(new RawAnswer(new BigDecimal("25"), null, null, null), hours)).isEmpty();
        assertThat(numeric.read(new RawAnswer(new BigDecimal("8"), true, null, null), hours)).isEmpty();
        assertThat(numeric.read(new RawAnswer(null, null, null, "8"), hours)).isEmpty();
    }

    @Test
    void readsOptionsWithTheirOwnLabelAndRejectsUnknownOnes() {
        Question grip = question(AnswerType.OPTION, List.of("Cilíndrico", "Esférico"));
        AnswerTypeRules option = factory.forType(AnswerType.OPTION);

        assertThat(option.read(new RawAnswer(null, null, " cilíndrico ", null), grip))
                .hasValueSatisfying(value -> assertThat(value.text()).hasValue("Cilíndrico"));
        assertThat(option.read(new RawAnswer(null, null, "Pinza fina", null), grip)).isEmpty();
    }

    @Test
    void readsYesNoAndNonBlankText() {
        assertThat(factory.forType(AnswerType.YES_NO).read(new RawAnswer(null, false, null, null),
                question(AnswerType.YES_NO, List.of()))).hasValueSatisfying(value -> assertThat(value.yesNo()).hasValue(false));
        AnswerTypeRules text = factory.forType(AnswerType.TEXT);
        Question notes = question(AnswerType.TEXT, List.of());
        assertThat(text.read(new RawAnswer(null, null, null, "  Mejora  "), notes))
                .hasValueSatisfying(value -> assertThat(value.text()).hasValue("Mejora"));
        assertThat(text.read(new RawAnswer(null, null, null, "   "), notes)).isEmpty();
        assertThat(text.read(new RawAnswer(null, null, null,
                "x".repeat(QuestionLimit.ANSWER_TEXT_MAX_LENGTH.getValue() + 1)), notes)).isEmpty();
    }

    @Test
    void optionRulesAcceptAnyConstraintsAndReturnTheCanonicalLabel() {
        Limits limits = new Limits(null, null, List.of("Firme", "Blanda"));
        assertThat(factory.forType(AnswerType.OPTION).read(new RawAnswer(null, null, "firme", null), limits))
                .hasValueSatisfying(value -> assertThat(value.text()).hasValue("Firme"));
    }

    @Test
    void numericRulesWithoutLimitsAcceptAnyNumber() {
        Limits limits = new Limits(null, null, List.of());
        assertThat(factory.forType(AnswerType.NUMERIC)
                .read(new RawAnswer(new BigDecimal("-10"), null, null, null), limits)).isPresent();
    }

    private record Limits(BigDecimal getMinValue, BigDecimal getMaxValue, List<String> getOptionLabels)
            implements AnswerConstraints {
    }

    private static Question question(AnswerType type, List<String> options) {
        Question question = new Question(new EvaluationTemplate("Plantilla", new BodySegment("Muñeca")), 1);
        question.define(new QuestionDefinition("Pregunta", type, null, null, options, null, null));
        return question;
    }
}
