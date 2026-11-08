package com.tomanagement.segmentmetric.controller;

import java.util.List;

import com.tomanagement.segmentmetric.dto.ChangeSegmentMetricActiveRequest;
import com.tomanagement.segmentmetric.dto.SegmentMetricOrderRequest;
import com.tomanagement.segmentmetric.dto.SegmentMetricRequest;
import com.tomanagement.segmentmetric.dto.SegmentMetricResponse;
import com.tomanagement.segmentmetric.service.SegmentMetricService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * ROM metrics of a segment: every signed-in user reads them; only admins define them.
 */
@RestController
@RequestMapping("/api/segments/{segmentId}/metrics")
public class SegmentMetricController {

    private final SegmentMetricService service;

    public SegmentMetricController(SegmentMetricService service) {
        this.service = service;
    }

    @GetMapping
    public List<SegmentMetricResponse> list(@PathVariable("segmentId") Long segmentId) {
        return service.list(segmentId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public SegmentMetricResponse create(@PathVariable("segmentId") Long segmentId,
            @Valid @RequestBody SegmentMetricRequest request) {
        return service.create(segmentId, request);
    }

    @PutMapping("/order")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public List<SegmentMetricResponse> reorder(@PathVariable("segmentId") Long segmentId,
            @Valid @RequestBody SegmentMetricOrderRequest request) {
        return service.reorder(segmentId, request.ids());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public SegmentMetricResponse update(@PathVariable("segmentId") Long segmentId, @PathVariable("id") Long id,
            @Valid @RequestBody SegmentMetricRequest request) {
        return service.update(segmentId, id, request);
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public SegmentMetricResponse changeActive(@PathVariable("segmentId") Long segmentId,
            @PathVariable("id") Long id, @Valid @RequestBody ChangeSegmentMetricActiveRequest request) {
        return service.changeActive(segmentId, id, request.active());
    }
}
