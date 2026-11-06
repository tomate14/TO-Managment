package com.tomanagement.establishment.repository;

import java.util.List;

import com.tomanagement.establishment.entity.Establishment;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EstablishmentRepository extends JpaRepository<Establishment, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    List<Establishment> findAllByOrderByNameAsc();
}
