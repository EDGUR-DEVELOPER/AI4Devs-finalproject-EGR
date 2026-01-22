/**
 * Modelo de dominio para Document Core Service.
 * 
 * Define las entidades principales y la configuración de Hibernate para filtrado multi-tenant.
 */
@org.hibernate.annotations.FilterDef(
    name = "tenantFilter",
    parameters = @org.hibernate.annotations.ParamDef(
        name = "tenantId",
        type = Integer.class
    )
)
package com.docflow.documentcore.domain.model;

import org.hibernate.annotations.ParamDef;
