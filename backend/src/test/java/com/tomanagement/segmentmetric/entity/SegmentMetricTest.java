package com.tomanagement.segmentmetric.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.tomanagement.catalog.entity.BodySegment;
import com.tomanagement.template.entity.AnswerType;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SegmentMetricTest {

    private static SegmentMetric optionMetric(String... labels) {
        return new SegmentMetric(new BodySegment("Codo"), "Fuerza de agarre", AnswerType.OPTION, false, 1,
                List.of(labels));
    }

    @SuppressWarnings("unchecked")
    private static List<SegmentMetricOption> optionEntities(SegmentMetric metric) {
        return List.copyOf((List<SegmentMetricOption>) ReflectionTestUtils.getField(metric, "options"));
    }

    @Test
    void optionLabelsComeInOrder() {
        SegmentMetric metric = optionMetric("Firme", "Blanda", "Dolorosa");

        assertThat(metric.getOptionLabels()).containsExactly("Firme", "Blanda", "Dolorosa");
    }

    @Test
    void replaceOptionsRelabelsInPlaceAndKeepsTheSurvivingEntities() {
        SegmentMetric metric = optionMetric("Firme", "Blanda", "Dolorosa");
        List<SegmentMetricOption> before = optionEntities(metric);

        metric.replaceOptions(List.of("Muy firme", "Blanda"));

        List<SegmentMetricOption> after = optionEntities(metric);
        assertThat(metric.getOptionLabels()).containsExactly("Muy firme", "Blanda");
        assertThat(after).hasSize(2);
        assertThat(after.get(0)).isSameAs(before.get(0));
        assertThat(after.get(1)).isSameAs(before.get(1));
    }

    @Test
    void replaceOptionsAppendsNewLabelsAfterTheExistingOnes() {
        SegmentMetric metric = optionMetric("Firme", "Blanda");
        List<SegmentMetricOption> before = optionEntities(metric);

        metric.replaceOptions(List.of("Firme", "Blanda", "Rígida"));

        List<SegmentMetricOption> after = optionEntities(metric);
        assertThat(metric.getOptionLabels()).containsExactly("Firme", "Blanda", "Rígida");
        assertThat(after.get(0)).isSameAs(before.get(0));
        assertThat(after.get(1)).isSameAs(before.get(1));
        assertThat(after.get(2).getDisplayOrder()).isEqualTo(2);
    }

    @Test
    void moveToChangesTheOrder() {
        SegmentMetric metric = optionMetric("Firme", "Blanda");

        metric.moveTo(5);

        assertThat(metric.getDisplayOrder()).isEqualTo(5);
    }

    @Test
    void metricsHaveNoNumericRange() {
        SegmentMetric metric = new SegmentMetric(new BodySegment("Codo"), "Flexión", AnswerType.NUMERIC, true, 1,
                List.of());

        assertThat(metric.getMinValue()).isNull();
        assertThat(metric.getMaxValue()).isNull();
        assertThat(metric.getOptionLabels()).isEmpty();
        assertThat(metric.isActive()).isTrue();
        assertThat(metric.isRequired()).isTrue();
    }
}
