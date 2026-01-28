package com.docflow.documentcore.infrastructure.adapter.persistence;

import com.docflow.documentcore.domain.model.acl.CodigoNivelAcceso;
import com.docflow.documentcore.domain.model.acl.NivelAcceso;
import com.docflow.documentcore.infrastructure.adapter.persistence.entity.NivelAccesoEntity;
import com.docflow.documentcore.infrastructure.adapter.persistence.jpa.NivelAccesoJpaRepository;
import com.docflow.documentcore.infrastructure.adapter.persistence.mapper.NivelAccesoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NivelAccesoRepositoryAdapter
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NivelAccesoRepositoryAdapter Unit Tests")
class NivelAccesoRepositoryAdapterTest {

    @Mock
    private NivelAccesoJpaRepository jpaRepository;

    @Mock
    private NivelAccesoMapper mapper;

    @InjectMocks
    private NivelAccesoRepositoryAdapter adapter;

    private UUID testId;
    private NivelAccesoEntity testEntity;
    private NivelAcceso testDomain;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        testEntity = new NivelAccesoEntity();
        testEntity.setId(testId);
        testEntity.setCodigo("LECTURA");
        testEntity.setNombre("Lectura");
        testEntity.setActivo(true);

        testDomain = NivelAcceso.builder()
                .id(testId)
                .codigo(CodigoNivelAcceso.LECTURA)
                .nombre("Lectura")
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("should_ReturnDomainObject_When_FindByIdFindsEntity")
    void should_ReturnDomainObject_When_FindByIdFindsEntity() {
        // Given
        when(jpaRepository.findById(testId)).thenReturn(Optional.of(testEntity));
        when(mapper.toDomain(testEntity)).thenReturn(testDomain);

        // When
        Optional<NivelAcceso> result = adapter.findById(testId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(testId);
        verify(jpaRepository, times(1)).findById(testId);
        verify(mapper, times(1)).toDomain(testEntity);
    }

    @Test
    @DisplayName("should_ReturnEmpty_When_FindByIdDoesNotFindEntity")
    void should_ReturnEmpty_When_FindByIdDoesNotFindEntity() {
        // Given
        UUID nonExistingId = UUID.randomUUID();
        when(jpaRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        // When
        Optional<NivelAcceso> result = adapter.findById(nonExistingId);

        // Then
        assertThat(result).isEmpty();
        verify(jpaRepository, times(1)).findById(nonExistingId);
        verify(mapper, never()).toDomain(any());
    }

    @Test
    @DisplayName("should_ReturnDomainObject_When_FindByCodigoFindsEntity")
    void should_ReturnDomainObject_When_FindByCodigoFindsEntity() {
        // Given
        when(jpaRepository.findByCodigo("LECTURA")).thenReturn(Optional.of(testEntity));
        when(mapper.toDomain(testEntity)).thenReturn(testDomain);

        // When
        Optional<NivelAcceso> result = adapter.findByCodigo(CodigoNivelAcceso.LECTURA);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getCodigo()).isEqualTo(CodigoNivelAcceso.LECTURA);
        verify(jpaRepository, times(1)).findByCodigo("LECTURA");
    }

    @Test
    @DisplayName("should_ReturnActiveList_When_FindAllActiveOrderByOrden")
    void should_ReturnActiveList_When_FindAllActiveOrderByOrden() {
        // Given
        List<NivelAccesoEntity> entities = Arrays.asList(testEntity);
        when(jpaRepository.findAllActiveOrderByOrden()).thenReturn(entities);
        when(mapper.toDomain(testEntity)).thenReturn(testDomain);

        // When
        List<NivelAcceso> result = adapter.findAllActiveOrderByOrden();

        // Then
        assertThat(result).hasSize(1);
        verify(jpaRepository, times(1)).findAllActiveOrderByOrden();
        verify(mapper, times(1)).toDomain(testEntity);
    }

    @Test
    @DisplayName("should_ReturnAllList_When_FindAllOrderByOrden")
    void should_ReturnAllList_When_FindAllOrderByOrden() {
        // Given
        List<NivelAccesoEntity> entities = Arrays.asList(testEntity);
        when(jpaRepository.findAllOrderByOrden()).thenReturn(entities);
        when(mapper.toDomain(testEntity)).thenReturn(testDomain);

        // When
        List<NivelAcceso> result = adapter.findAllOrderByOrden();

        // Then
        assertThat(result).hasSize(1);
        verify(jpaRepository, times(1)).findAllOrderByOrden();
    }

    @Test
    @DisplayName("should_ReturnTrue_When_ExistsByCodigoFindsEntity")
    void should_ReturnTrue_When_ExistsByCodigoFindsEntity() {
        // Given
        when(jpaRepository.existsByCodigo("LECTURA")).thenReturn(true);

        // When
        boolean result = adapter.existsByCodigo(CodigoNivelAcceso.LECTURA);

        // Then
        assertThat(result).isTrue();
        verify(jpaRepository, times(1)).existsByCodigo("LECTURA");
    }

    @Test
    @DisplayName("should_ReturnFalse_When_ExistsByCodigoDoesNotFindEntity")
    void should_ReturnFalse_When_ExistsByCodigoDoesNotFindEntity() {
        // Given
        when(jpaRepository.existsByCodigo("ADMINISTRACION")).thenReturn(false);

        // When
        boolean result = adapter.existsByCodigo(CodigoNivelAcceso.ADMINISTRACION);

        // Then
        assertThat(result).isFalse();
        verify(jpaRepository, times(1)).existsByCodigo("ADMINISTRACION");
    }
}
