package com.tomanagement.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.tomanagement.catalog.dto.CatalogItemResponse;
import com.tomanagement.catalog.entity.BodySegment;
import com.tomanagement.catalog.repository.BodySegmentRepository;
import com.tomanagement.common.exception.ApplicationException;
import com.tomanagement.common.exception.DuplicateResourceException;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.common.exception.ResourceNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Shared catalog rules, exercised through the body-segment catalog.
 */
class AbstractCatalogServiceTest {

    private BodySegmentRepository repository;
    private BodySegmentCatalogService service;

    @BeforeEach
    void setUp() {
        repository = mock(BodySegmentRepository.class);
        service = new BodySegmentCatalogService(repository);
    }

    @Test
    void declaresItsCatalogType() {
        assertThat(service.type()).isEqualTo(CatalogType.BODY_SEGMENTS);
    }

    @Test
    void listsAllOrOnlyActiveItems() {
        when(repository.findAllByOrderByNameAsc()).thenReturn(List.of(segment(1L, "Codo", true), segment(2L, "Dedos", false)));
        when(repository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of(segment(1L, "Codo", true)));

        assertThat(service.list(false)).extracting(CatalogItemResponse::name).containsExactly("Codo", "Dedos");
        assertThat(service.list(true)).extracting(CatalogItemResponse::name).containsExactly("Codo");
    }

    @Test
    void createsAnActiveItem() {
        when(repository.existsByNameIgnoreCase("Antebrazo")).thenReturn(false);
        when(repository.saveAndFlush(any(BodySegment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CatalogItemResponse created = service.create("Antebrazo");

        assertThat(created.name()).isEqualTo("Antebrazo");
        assertThat(created.active()).isTrue();
    }

    @Test
    void rejectsADuplicateNameIgnoringCase() {
        when(repository.existsByNameIgnoreCase("muñeca")).thenReturn(true);

        assertThatThrownBy(() -> service.create("muñeca"))
                .isInstanceOf(DuplicateResourceException.class)
                .extracting(error -> ((ApplicationException) error).getErrorCode())
                .isEqualTo(ErrorCode.CATALOG_ITEM_DUPLICATE);
        verify(repository, never()).saveAndFlush(any(BodySegment.class));
    }

    @Test
    void translatesAUniqueIndexRaceIntoADuplicate() {
        when(repository.existsByNameIgnoreCase("Antebrazo")).thenReturn(false);
        when(repository.saveAndFlush(any(BodySegment.class))).thenThrow(new DataIntegrityViolationException("dup"));

        assertThatThrownBy(() -> service.create("Antebrazo")).isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void renamesExcludingItselfFromTheDuplicateCheck() {
        BodySegment wrist = segment(3L, "Muñeca", true);
        when(repository.findById(3L)).thenReturn(Optional.of(wrist));
        when(repository.existsByNameIgnoreCaseAndIdNot("MUÑECA", 3L)).thenReturn(false);
        when(repository.saveAndFlush(wrist)).thenReturn(wrist);

        assertThat(service.rename(3L, "MUÑECA").name()).isEqualTo("MUÑECA");
    }

    @Test
    void rejectsRenamingToAnotherItemsName() {
        when(repository.findById(3L)).thenReturn(Optional.of(segment(3L, "Muñeca", true)));
        when(repository.existsByNameIgnoreCaseAndIdNot("codo", 3L)).thenReturn(true);

        assertThatThrownBy(() -> service.rename(3L, "codo")).isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void deactivatesAndReactivates() {
        BodySegment fingers = segment(5L, "Dedos", true);
        when(repository.findById(5L)).thenReturn(Optional.of(fingers));

        assertThat(service.changeActive(5L, false).active()).isFalse();
        assertThat(service.changeActive(5L, true).active()).isTrue();
    }

    @Test
    void unknownItemsAreNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeActive(99L, false))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting(error -> ((ApplicationException) error).getErrorCode())
                .isEqualTo(ErrorCode.CATALOG_ITEM_NOT_FOUND);
    }

    private static BodySegment segment(Long id, String name, boolean active) {
        BodySegment segment = new BodySegment(name);
        segment.changeActive(active);
        ReflectionTestUtils.setField(segment, "id", id);
        return segment;
    }
}
