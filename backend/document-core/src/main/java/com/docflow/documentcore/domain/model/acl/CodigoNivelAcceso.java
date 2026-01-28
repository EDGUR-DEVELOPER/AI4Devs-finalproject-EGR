package com.docflow.documentcore.domain.model.acl;

/**
 * Value Object representing the invariable code of an access level.
 * Defines standard access level codes: LECTURA, ESCRITURA, ADMINISTRACION
 */
public enum CodigoNivelAcceso {
    LECTURA("LECTURA", "Lectura / Consulta"),
    ESCRITURA("ESCRITURA", "Escritura / Modificación"),
    ADMINISTRACION("ADMINISTRACION", "Administración / Control Total");

    private final String codigo;
    private final String displayName;

    CodigoNivelAcceso(String codigo, String displayName) {
        this.codigo = codigo;
        this.displayName = displayName;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static CodigoNivelAcceso fromCodigo(String codigo) {
        for (CodigoNivelAcceso nivel : values()) {
            if (nivel.codigo.equals(codigo)) {
                return nivel;
            }
        }
        throw new IllegalArgumentException("Invalid codigo nivel acceso: " + codigo);
    }
}
