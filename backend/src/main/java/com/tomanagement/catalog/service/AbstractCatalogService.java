package com.tomanagement.catalog.service;

import java.util.List;

import com.tomanagement.catalog.dto.CatalogItemResponse;
import com.tomanagement.catalog.entity.CatalogItem;
import com.tomanagement.catalog.repository.CatalogRepository;
import com.tomanagement.common.exception.DuplicateResourceException;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.common.exception.ResourceNotFoundException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

/**
 * Template Method: every catalog shares these rules; subclasses only say which catalog they are and how to
 * build a new item (factory method {@link #newItem(String)}).
 */
public abstract class AbstractCatalogService<E extends CatalogItem> implements CatalogService {

    private final CatalogRepository<E> repository;

    protected AbstractCatalogService(CatalogRepository<E> repository) {
        this.repository = repository;
    }

    protected abstract E newItem(String name);

    @Override
    @Transactional(readOnly = true)
    public List<CatalogItemResponse> list(boolean activeOnly) {
        List<E> items = activeOnly ? repository.findAllByActiveTrueOrderByNameAsc() : repository.findAllByOrderByNameAsc();
        return items.stream().map(CatalogItemResponse::from).toList();
    }

    @Override
    @Transactional
    public CatalogItemResponse create(String name) {
        if (repository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException(ErrorCode.CATALOG_ITEM_DUPLICATE);
        }
        return save(newItem(name));
    }

    @Override
    @Transactional
    public CatalogItemResponse rename(Long id, String name) {
        E item = find(id);
        if (repository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateResourceException(ErrorCode.CATALOG_ITEM_DUPLICATE);
        }
        item.rename(name);
        return save(item);
    }

    @Override
    @Transactional
    public CatalogItemResponse changeActive(Long id, boolean active) {
        E item = find(id);
        item.changeActive(active);
        return CatalogItemResponse.from(item);
    }

    private CatalogItemResponse save(E item) {
        try {
            return CatalogItemResponse.from(repository.saveAndFlush(item));
        } catch (DataIntegrityViolationException raceOnUniqueName) {
            throw new DuplicateResourceException(ErrorCode.CATALOG_ITEM_DUPLICATE);
        }
    }

    private E find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CATALOG_ITEM_NOT_FOUND));
    }
}
