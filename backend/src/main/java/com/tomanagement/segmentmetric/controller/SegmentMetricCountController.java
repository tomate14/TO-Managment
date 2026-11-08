package com.tomanagement.segmentmetric.controller;

import java.util.List;

import com.tomanagement.segmentmetric.dto.SegmentMetricCountResponse;
import com.tomanagement.segmentmetric.service.SegmentMetricService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Active metrics per segment, for the "Métricas ROM · n" counters.
 */
@RestController
@RequestMapping("/api/segment-metrics")
public class SegmentMetricCountController {

    private final SegmentMetricService service;

    public SegmentMetricCountController(SegmentMetricService service) {
        this.service = service;
    }

    @GetMapping("/counts")
    public List<SegmentMetricCountResponse> counts() {
        return service.counts();
    }
}
