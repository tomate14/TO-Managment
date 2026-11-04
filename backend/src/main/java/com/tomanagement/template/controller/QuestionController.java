package com.tomanagement.template.controller;

import com.tomanagement.template.dto.ChangeActiveRequest;
import com.tomanagement.template.dto.QuestionRequest;
import com.tomanagement.template.dto.TemplateDetailResponse;
import com.tomanagement.template.service.QuestionService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Questions of a template. Mutations answer with the refreshed template.
 */
@RestController
@RequestMapping("/api/templates/{templateId}/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TemplateDetailResponse create(@PathVariable("templateId") Long templateId,
            @Valid @RequestBody QuestionRequest request) {
        return questionService.create(templateId, request);
    }

    @PutMapping("/{questionId}")
    public TemplateDetailResponse update(@PathVariable("templateId") Long templateId,
            @PathVariable("questionId") Long questionId, @Valid @RequestBody QuestionRequest request) {
        return questionService.update(templateId, questionId, request);
    }

    @PatchMapping("/{questionId}/active")
    public TemplateDetailResponse changeActive(@PathVariable("templateId") Long templateId,
            @PathVariable("questionId") Long questionId, @Valid @RequestBody ChangeActiveRequest request) {
        return questionService.changeActive(templateId, questionId, request.active());
    }

    @DeleteMapping("/{questionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("templateId") Long templateId, @PathVariable("questionId") Long questionId) {
        questionService.delete(templateId, questionId);
    }
}
