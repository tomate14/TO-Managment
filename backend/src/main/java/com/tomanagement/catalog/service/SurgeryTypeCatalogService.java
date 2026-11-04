package com.tomanagement.catalog.service;

import com.tomanagement.catalog.entity.SurgeryType;
import com.tomanagement.catalog.repository.SurgeryTypeRepository;

import org.springframework.stereotype.Service;

@Service
public class SurgeryTypeCatalogService extends AbstractCatalogService<SurgeryType> {

    public SurgeryTypeCatalogService(SurgeryTypeRepository repository) {
        super(repository);
    }

    @Override
    public CatalogType type() {
        return CatalogType.SURGERY_TYPES;
    }

    @Override
    protected SurgeryType newItem(String name) {
        return new SurgeryType(name);
    }
}
