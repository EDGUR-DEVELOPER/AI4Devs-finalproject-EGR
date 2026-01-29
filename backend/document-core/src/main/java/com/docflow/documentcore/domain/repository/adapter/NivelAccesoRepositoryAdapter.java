package com.docflow.documentcore.domain.repository.adapter;

import com.docflow.documentcore.application.mapper.NivelAccesoMapper;
import com.docflow.documentcore.domain.model.acl.CodigoNivelAcceso;
import com.docflow.documentcore.domain.model.acl.NivelAcceso;
import com.docflow.documentcore.domain.repository.INivelAccesoRepository;
import com.docflow.documentcore.domain.repository.NivelAccesoJpaRepository;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter implementation of INivelAccesoRepository
 * Bridges domain repository interface with JPA infrastructure
 */
@Repository
public class NivelAccesoRepositoryAdapter implements INivelAccesoRepository {
    
    private final NivelAccesoJpaRepository jpaRepository;
    private final NivelAccesoMapper mapper;

    public NivelAccesoRepositoryAdapter(
            NivelAccesoJpaRepository jpaRepository,
            NivelAccesoMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<NivelAcceso> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<NivelAcceso> findByCodigo(CodigoNivelAcceso codigo) {
        return jpaRepository.findByCodigo(codigo.getCodigo())
                .map(mapper::toDomain);
    }

    @Override
    public List<NivelAcceso> findAllActiveOrderByOrden() {
        return jpaRepository.findAllActiveOrderByOrden().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<NivelAcceso> findAllOrderByOrden() {
        return jpaRepository.findAllOrderByOrden().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByCodigo(CodigoNivelAcceso codigo) {
        return jpaRepository.existsByCodigo(codigo.getCodigo());
    }
}
