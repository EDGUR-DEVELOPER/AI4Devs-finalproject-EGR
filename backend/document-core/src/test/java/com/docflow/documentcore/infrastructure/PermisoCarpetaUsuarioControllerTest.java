package com.docflow.documentcore.infrastructure;

import com.docflow.documentcore.application.mapper.NivelAccesoDtoMapper;
import com.docflow.documentcore.application.mapper.PermisoCarpetaUsuarioMapper;
import com.docflow.documentcore.application.service.PermisoCarpetaUsuarioService;
import com.docflow.documentcore.infrastructure.adapter.controller.PermisoCarpetaUsuarioController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PermisoCarpetaUsuarioController.class)
@DisplayName("PermisoCarpetaUsuarioController - Tests Unitarios")
class PermisoCarpetaUsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PermisoCarpetaUsuarioService service;

    @MockitoBean
    private PermisoCarpetaUsuarioMapper permisoMapper;

    @MockitoBean
    private NivelAccesoDtoMapper nivelAccesoMapper;

    @Test
    @DisplayName("Debería revocar permiso y retornar 204")
    void should_RevokePermission_ReturnNoContent() throws Exception {
        doNothing().when(service).revocarPermiso(12L, 5L, 1L, 99L);

        mockMvc.perform(delete("/api/carpetas/12/permisos/5")
                        .header("X-Organization-Id", "1")
                        .header("X-User-Id", "99"))
                .andExpect(status().isNoContent());
    }
}
