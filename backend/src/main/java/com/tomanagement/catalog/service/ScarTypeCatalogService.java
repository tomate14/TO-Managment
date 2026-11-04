package com.tomanagement.catalog.service;

import com.tomanagement.catalog.entity.ScarType;
import com.tomanagement.catalog.repository.ScarTypeRepository;

import org.springframework.stereotype.Service;

@Service
public class ScarTypeCatalogService extends AbstractCatalogService<ScarType> {

    public ScarTypeCatalogService(ScarTypeRepository repository) {
        super(repository);
    }

    @Override
    public CatalogType type() {
        return CatalogType.SCAR_TYPES;
    }

    @Override
    protected ScarType newItem(String name) {
        return new ScarType(name);
    }
}
