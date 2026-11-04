package com.tomanagement.catalog.repository;

import java.util.List;

import com.tomanagement.catalog.entity.CatalogItem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Queries shared by every catalog; each entity gets its own concrete repository.
 */
@NoRepositoryBean
public interface CatalogRepository<E extends CatalogItem> extends JpaRepository<E, Long> {

    List<E> findAllByOrderByNameAsc();

    List<E> findAllByActiveTrueOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
