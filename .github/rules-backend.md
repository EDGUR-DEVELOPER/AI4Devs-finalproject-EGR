# Reglas de desarrollo Backend

Estas reglas aplican a todos los servicios **Spring Boot** ubicados en la carpeta `backend/` (auditLog, broker, document-core, gateway, identity, vault).

## 1. Arquitectura y capas

- Seguir una arquitectura por capas: **controller → service → repository → domain/model**.
- Mantener las responsabilidades claras y cohesivas en cada capa.
- Evitar que los controladores contengan lógica de negocio; esta debe vivir en los servicios.
- Mantener DTOs separados de las entidades de persistencia cuando corresponda.

## 2. Estándares de código

- Usar Java 21 y Spring Boot 3.x conforme a la configuración del proyecto.
- Nombres descriptivos para clases, métodos y variables.
- Seguir las convenciones:
  - Clases: `PascalCase`.
  - Métodos y variables: `camelCase`.
  - Constantes: `MAYUSCULA_CON_GUIONES_BAJO`.
- Preferir **inyección por constructor** sobre inyección por campos.
- Centralizar configuración en `application.yml` y perfiles específicos (`application-dev.yml`, etc.).

## 3. APIs y contratos

- Diseñar endpoints REST claros, con nombres de recursos consistentes.
- Usar validaciones con anotaciones (`@Valid`, `@NotNull`, etc.) en DTOs de entrada.
- Documentar APIs con Springdoc OpenAPI según la configuración de cada servicio.

## 4. Manejo de errores y logging

- Usar excepciones específicas en lugar de excepciones genéricas.
- Manejar errores en puntos controlados (handlers globales, capas de servicio) y devolver respuestas claras.
- Usar logging con niveles adecuados (INFO, WARN, ERROR) y evitar logs ruidosos.
- No registrar datos sensibles.

## 5. Pruebas en backend

- Seguir TDD: **RED → GREEN → REFACTOR** siempre que sea posible.
- Escribir pruebas **unitarias** para la lógica de negocio de servicios y utilidades.
- Usar JUnit 5, Mockito y AssertJ según la configuración del proyecto.
- Reservar `@SpringBootTest` y pruebas de integración para casos donde realmente se necesite el contexto de Spring.
- Nombrar los métodos de prueba siguiendo el patrón: `should_DoSomething_When_Condition`.
- Incluir pruebas para escenarios felices, negativos y de borde.

## 6. Dependencias y versiones

- Respetar las versiones de Spring Boot, Java y librerías definidas en los `pom.xml` de cada servicio.
- Evitar introducir dependencias innecesarias; si se requiere una nueva librería, justificarla en el PR.

## 7. Referencias

- Índice general de reglas: [RULES.md](./RULES.md)
