package com.tomanagement.followup.repository;

import com.tomanagement.followup.entity.Answer;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    boolean existsByQuestion_Id(Long questionId);
}
