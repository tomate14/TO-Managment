package com.tomanagement.segmentmetric.service;

import java.util.List;

import com.tomanagement.segmentmetric.dto.SegmentMetricCountResponse;
import com.tomanagement.segmentmetric.dto.SegmentMetricRequest;
import com.tomanagement.segmentmetric.dto.SegmentMetricResponse;

public interface SegmentMetricService {

    List<SegmentMetricResponse> list(Long segmentId);

    SegmentMetricResponse create(Long segmentId, SegmentMetricRequest request);

    SegmentMetricResponse update(Long segmentId, Long id, SegmentMetricRequest request);

    SegmentMetricResponse changeActive(Long segmentId, Long id, boolean active);

    List<SegmentMetricResponse> reorder(Long segmentId, List<Long> ids);

    /** Active metrics per segment (segments without active metrics are omitted). */
    List<SegmentMetricCountResponse> counts();
}
