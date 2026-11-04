package com.tomanagement.template.repository;

import java.util.Collection;
import java.util.List;

import com.tomanagement.template.entity.Question;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByTemplate_IdOrderByDisplayOrderAsc(Long templateId);

    List<Question> findByTemplate_IdIn(Collection<Long> templateIds);

    List<Question> findByParent_Id(Long parentId);

    boolean existsByParent_Id(Long parentId);

    @Query("select coalesce(max(q.displayOrder), 0) from Question q where q.template.id = :templateId")
    int maxDisplayOrder(@Param("templateId") Long templateId);
}
