package com.tomanagement.catalog.service;

import com.tomanagement.catalog.entity.BodySegment;
import com.tomanagement.catalog.repository.BodySegmentRepository;

import org.springframework.stereotype.Service;

@Service
public class BodySegmentCatalogService extends AbstractCatalogService<BodySegment> {

    public BodySegmentCatalogService(BodySegmentRepository repository) {
        super(repository);
    }

    @Override
    public CatalogType type() {
        return CatalogType.BODY_SEGMENTS;
    }

    @Override
    protected BodySegment newItem(String name) {
        return new BodySegment(name);
    }
}
