package com.docflow.identity.application.services;

import com.docflow.identity.application.dto.RoleSummaryDto;
import com.docflow.identity.application.mapper.UserDtoMapper;
import com.docflow.identity.domain.model.Rol;
import com.docflow.identity.domain.repository.RolRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de aplicación para gestión y consulta de roles.
 * Maneja la lógica de visualización de roles respetando el aislamiento
 * multi-tenant.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService {

    private final RolRepository rolRepository;
    private final UserDtoMapper userDtoMapper;

    /**
     * Lista los roles disponibles para una organización específica.
     * Incluye roles globales del sistema (organizacionId = 0) y roles 
     * personalizados de la organización.
     * 
     * @param organizacionId ID de la organización
     * @return Lista de summaries de roles disponibles
     */
    @Transactional(readOnly = true)
    public List<RoleSummaryDto> listAvailableRoles(Integer organizacionId) {
        log.debug("Listando roles disponibles para organización: {}", organizacionId);

        // Obtener roles mediante query optimizada que filtra directamente en BD
        List<Rol> roles = rolRepository.findAvailableRolesByOrganizacionId(organizacionId);

        return roles.stream()
                .map(userDtoMapper::toRoleSummaryDto)
                .toList();
    }
}
