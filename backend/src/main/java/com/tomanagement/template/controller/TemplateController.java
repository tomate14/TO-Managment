package com.tomanagement.template.controller;

import java.util.List;

import com.tomanagement.template.dto.ChangeActiveRequest;
import com.tomanagement.template.dto.TemplateDetailResponse;
import com.tomanagement.template.dto.TemplateRequest;
import com.tomanagement.template.dto.TemplateSummaryResponse;
import com.tomanagement.template.service.TemplateService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Evaluation templates; available to every signed-in user (therapists and admins).
 */
@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public List<TemplateSummaryResponse> list(@RequestParam(name = "segmentId", required = false) Long segmentId) {
        return templateService.list(segmentId);
    }

    @GetMapping("/{id}")
    public TemplateDetailResponse get(@PathVariable("id") Long id) {
        return templateService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TemplateDetailResponse create(@Valid @RequestBody TemplateRequest request) {
        return templateService.create(request);
    }

    @PutMapping("/{id}")
    public TemplateDetailResponse update(@PathVariable("id") Long id, @Valid @RequestBody TemplateRequest request) {
        return templateService.update(id, request);
    }

    @PatchMapping("/{id}/active")
    public TemplateDetailResponse changeActive(@PathVariable("id") Long id,
            @Valid @RequestBody ChangeActiveRequest request) {
        return templateService.changeActive(id, request.active());
    }
}
