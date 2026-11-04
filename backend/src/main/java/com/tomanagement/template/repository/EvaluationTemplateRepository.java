package com.tomanagement.template.repository;

import java.util.List;

import com.tomanagement.template.entity.EvaluationTemplate;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluationTemplateRepository extends JpaRepository<EvaluationTemplate, Long> {

    List<EvaluationTemplate> findAllByOrderByNameAsc();

    List<EvaluationTemplate> findBySegment_IdOrderByNameAsc(Long segmentId);

    boolean existsBySegment_IdAndNameIgnoreCase(Long segmentId, String name);

    boolean existsBySegment_IdAndNameIgnoreCaseAndIdNot(Long segmentId, String name, Long id);
}
