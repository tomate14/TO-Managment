package com.tomanagement.segmentmetric.repository;

import java.util.Collection;
import java.util.List;

import com.tomanagement.segmentmetric.dto.SegmentMetricCountResponse;
import com.tomanagement.segmentmetric.entity.SegmentMetric;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SegmentMetricRepository extends JpaRepository<SegmentMetric, Long> {

    List<SegmentMetric> findBySegment_IdOrderByDisplayOrderAsc(Long segmentId);

    List<SegmentMetric> findBySegment_IdAndActiveTrueOrderByDisplayOrderAsc(Long segmentId);

    List<SegmentMetric> findByIdIn(Collection<Long> ids);

    boolean existsBySegment_IdAndNameIgnoreCase(Long segmentId, String name);

    boolean existsBySegment_IdAndNameIgnoreCaseAndIdNot(Long segmentId, String name, Long id);

    @Query("select coalesce(max(m.displayOrder), 0) from SegmentMetric m where m.segment.id = :segmentId")
    int maxDisplayOrder(@Param("segmentId") Long segmentId);

    @Query("select new com.tomanagement.segmentmetric.dto.SegmentMetricCountResponse(m.segment.id, count(m)) "
            + "from SegmentMetric m where m.active = true group by m.segment.id")
    List<SegmentMetricCountResponse> countActiveBySegment();
}
