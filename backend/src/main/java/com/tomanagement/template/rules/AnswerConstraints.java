package com.tomanagement.template.rules;

import java.math.BigDecimal;
import java.util.List;

/**
 * Limits a typed answer is checked against (template questions and segment metrics).
 */
public interface AnswerConstraints {

    BigDecimal getMinValue();

    BigDecimal getMaxValue();

    List<String> getOptionLabels();
}
