/// <reference types="cypress" />

/**
 * E2E Tests: Descarga de documentos desde lista
 * US-DOC-007: Descarga de documento actual desde lista de documentos
 * 
 * Tests end-to-end para validar flujo completo de descarga
 */

describe('Document Download from List - E2E', () => {
  beforeEach(() => {
    // Configurar mock del token JWT en localStorage
    cy.window().then((win) => {
      win.localStorage.setItem('token', 'mock-jwt-token-for-testing');
      win.localStorage.setItem('user', JSON.stringify({
        id: 'user-123',
        nombre: 'Test User',
        email: 'user@example.com',
        rol: 'USER',
      }));
    });

    // Mockear endpoint de obtención de contenido de carpeta
    cy.intercept('GET', '/api/doc/carpetas/1/contenido*', {
      statusCode: 200,
      body: {
        subcarpetas: [],
        documentos: [
          {
            id: 'doc-1',
            nombre: 'contrato_2026.pdf',
            tipo: 'documento',
            tipo_mime: 'application/pdf',
            tamaño_bytes: 1024000,
            propietario_id: 'user-123',
            fecha_creacion: '2026-02-01T10:00:00Z',
            fecha_modificacion: '2026-02-10T15:30:00Z',
            version_actual: 1,
            capacidades: {
              puede_descargar: true,
              puede_leer: true,
              puede_escribir: false,
              puede_administrar: false,
            },
          },
          {
            id: 'doc-2',
            nombre: 'documento_importante.docx',
            tipo: 'documento',
            tipo_mime: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
            tamaño_bytes: 512000,
            propietario_id: 'user-123',
            fecha_creacion: '2026-02-05T14:20:00Z',
            fecha_modificacion: '2026-02-12T09:15:00Z',
            version_actual: 2,
            capacidades: {
              puede_descargar: true,
              puede_leer: true,
              puede_escribir: true,
              puede_administrar: false,
            },
          },
        ],
        permisos: {
          puede_leer: true,
          puede_escribir: true,
          puede_administrar: false,
        },
      },
    }).as('getFolderContent');

    // Navegar directamente a carpeta con ID 1
    cy.visit('/carpetas/1');
    cy.wait('@getFolderContent', { timeout: 10000 });
  });

  it('debería descargar documento al hacer click en botón', () => {
    // Mockear descarga exitosa
    cy.intercept('GET', '/api/doc/documentos/doc-1/download', {
      statusCode: 200,
      headers: {
        'content-type': 'application/pdf',
        'content-disposition': 'attachment; filename="contrato_2026.pdf"',
      },
      body: 'PDF fake content',
    }).as('download');

    // Validar que documento visible
    cy.get('[data-testid^="document-item-"]').should('have.length.greaterThan', 0);
    
    // Click en botón descargar (ícono de descarga)
    cy.get('[data-testid^="document-item-"]')
      .first()
      .within(() => {
        cy.get('button[aria-label*="descarg"], button[aria-label*="Descarg"]', { timeout: 5000 }).click();
      });

    cy.wait('@download');

    // Validar notificación de éxito (si existe)
    cy.get('[data-testid="notification-success"], .toast-success', { timeout: 5000 }).should('exist');
  });

  it('debería mostrar spinner durante descarga', () => {
    // Interceptar llamada de descarga con delay
    cy.intercept('GET', '/api/doc/documentos/doc-1/download', (req) => {
      req.reply((res) => {
        res.send({
          delay: 2000,
          statusCode: 200,
          headers: {
            'content-type': 'application/pdf',
            'content-disposition': 'attachment; filename="contrato_2026.pdf"',
          },
          body: 'PDF content',
        });
      });
    }).as('slowDownload');

    cy.get('[data-testid^="document-item-"]')
      .first()
      .within(() => {
        cy.get('button[aria-label*="descarg"], button[aria-label*="Descarg"]', { timeout: 5000 }).click();
      });

    // Validar que spinner está visible
    cy.get('svg[class*="animate-spin"], .spinner', { timeout: 5000 }).should('exist');

    // Esperar a que descarga termine
    cy.wait('@slowDownload');
  });

  it('debería manejar error 403 - sin permiso', () => {
    // Interceptar con 403
    cy.intercept('GET', '/api/doc/documentos/*/download', {
      statusCode: 403,
      body: { 
        error: 'Forbidden',
        message: 'No tienes permiso para descargar este documento',
      },
    }).as('forbiddenDownload');

    cy.get('[data-testid^="document-item-"]')
      .first()
      .within(() => {
        cy.get('button[aria-label*="descarg"], button[aria-label*="Descarg"]', { timeout: 5000 }).click();
      });

    cy.wait('@forbiddenDownload');

    // Validar que notificación de error se muestra
    cy.get('[data-testid="notification-error"], .toast-error, [role="alert"]', { timeout: 5000 })
      .should('exist');
  });

  it('debería manejar error 404 - documento no encontrado', () => {
    cy.intercept('GET', '/api/doc/documentos/*/download', {
      statusCode: 404,
      body: { 
        error: 'Not Found',
        message: 'Documento no encontrado',
      },
    }).as('notFoundDownload');

    cy.get('[data-testid^="document-item-"]')
      .first()
      .within(() => {
        cy.get('button[aria-label*="descarg"], button[aria-label*="Descarg"]', { timeout: 5000 }).click();
      });

    cy.wait('@notFoundDownload');

    cy.get('[data-testid="notification-error"], .toast-error, [role="alert"]', { timeout: 5000 })
      .should('exist');
  });

  it('debería manejar error 500 - error servidor', () => {
    cy.intercept('GET', '/api/doc/documentos/*/download', {
      statusCode: 500,
      body: { 
        error: 'Internal Server Error',
        message: 'Error de servidor',
      },
    }).as('serverErrorDownload');

    cy.get('[data-testid^="document-item-"]')
      .first()
      .within(() => {
        cy.get('button[aria-label*="descarg"], button[aria-label*="Descarg"]', { timeout: 5000 }).click();
      });

    cy.wait('@serverErrorDownload');

    cy.get('[data-testid="notification-error"], .toast-error, [role="alert"]', { timeout: 5000 })
      .should('exist');
  });

  it('debería deshabilitar botón si usuario NO tiene permiso LECTURA', () => {
    // Mock de carpeta con documentos sin permiso de descarga
    cy.intercept('GET', '/api/doc/carpetas/1/contenido*', {
      statusCode: 200,
      body: {
        subcarpetas: [],
        documentos: [
          {
            id: 'doc-no-perm',
            nombre: 'documento_restringido.pdf',
            tipo: 'documento',
            tipo_mime: 'application/pdf',
            tamaño_bytes: 1024000,
            propietario_id: 'other-user',
            fecha_creacion: '2026-02-01T10:00:00Z',
            fecha_modificacion: '2026-02-10T15:30:00Z',
            version_actual: 1,
            capacidades: {
              puede_descargar: false,
              puede_leer: true,
              puede_escribir: false,
              puede_administrar: false,
            },
          },
        ],
        permisos: {
          puede_leer: true,
          puede_escribir: false,
          puede_administrar: false,
        },
      },
    }).as('getRestrictedContent');

    cy.visit('/carpetas/1');
    cy.wait('@getRestrictedContent');

    // Validar botón deshabilitado
    cy.get('[data-testid^="document-item-"]')
      .first()
      .within(() => {
        cy.get('button[aria-label*="descarg"], button[aria-label*="Descarg"]', { timeout: 5000 })
          .should('be.disabled')
          .should('have.attr', 'aria-disabled', 'true');
      });
  });

  it('debería mostrar tooltip cuando botón está deshabilitado', () => {
    // Mock de carpeta con documentos sin permiso
    cy.intercept('GET', '/api/doc/carpetas/1/contenido*', {
      statusCode: 200,
      body: {
        subcarpetas: [],
        documentos: [
          {
            id: 'doc-no-perm',
            nombre: 'documento_restringido.pdf',
            tipo: 'documento',
            tipo_mime: 'application/pdf',
            tamaño_bytes: 1024000,
            propietario_id: 'other-user',
            fecha_creacion: '2026-02-01T10:00:00Z',
            fecha_modificacion: '2026-02-10T15:30:00Z',
            version_actual: 1,
            capacidades: {
              puede_descargar: false,
              puede_leer: true,
              puede_escribir: false,
              puede_administrar: false,
            },
          },
        ],
        permisos: {
          puede_leer: true,
          puede_escribir: false,
          puede_administrar: false,
        },
      },
    }).as('getRestrictedContent');

    cy.visit('/carpetas/1');
    cy.wait('@getRestrictedContent');

    cy.get('[data-testid^="document-item-"]').first().within(() => {
      // Hover sobre botón deshabilitado
      cy.get('button[aria-label*="descarg"], button[aria-label*="Descarg"]', { timeout: 5000 })
        .trigger('mouseenter');

      // Validar tooltip visible (si existe)
      cy.get('[role="tooltip"], .tooltip', { timeout: 3000 }).should('exist');
    });
  });

  it('debería permitir reintento después de error', () => {
    // Primera llamada: error 500
    cy.intercept('GET', '/api/doc/documentos/doc-1/download', {
      statusCode: 500,
      body: {},
    }).as('failDownload');

    cy.get('[data-testid^="document-item-"]')
      .first()
      .within(() => {
        cy.get('button[aria-label*="descarg"], button[aria-label*="Descarg"]', { timeout: 5000 }).click();
      });

    cy.wait('@failDownload');

    // Validar error visible
    cy.get('[data-testid="notification-error"], .toast-error, [role="alert"]', { timeout: 5000 })
      .should('exist');

    // Segunda llamada: éxito
    cy.intercept('GET', '/api/doc/documentos/doc-1/download', {
      statusCode: 200,
      headers: {
        'content-type': 'application/pdf',
        'content-disposition': 'attachment; filename="test.pdf"',
      },
      body: 'PDF content',
    }).as('successDownload');

    // Reintentar descarga
    cy.get('[data-testid^="document-item-"]')
      .first()
      .within(() => {
        cy.get('button[aria-label*="descarg"], button[aria-label*="Descarg"]', { timeout: 5000 }).click();
      });

    cy.wait('@successDownload');

    // Validar que botón vuelve a estado normal
    cy.get('[data-testid^="document-item-"]')
      .first()
      .within(() => {
        cy.get('button[aria-label*="descarg"], button[aria-label*="Descarg"]', { timeout: 5000 })
          .should('not.be.disabled');
      });
  });

  it('debería funcionar en responsive design (mobile)', () => {
    // Set mobile viewport
    cy.viewport('iphone-x');

    // Mockear descarga
    cy.intercept('GET', '/api/doc/documentos/*/download', {
      statusCode: 200,
      body: 'content',
    }).as('mobileDownload');

    // Botón debe ser visible en mobile
    cy.get('[data-testid^="document-item-"]')
      .first()
      .within(() => {
        cy.get('button[aria-label*="descarg"], button[aria-label*="Descarg"]', { timeout: 5000 })
          .should('be.visible')
          .click();
      });

    cy.wait('@mobileDownload');
  });

  it('debería ser accesible via teclado (tab + enter)', () => {
    // Mockear descarga
    cy.intercept('GET', '/api/doc/documentos/*/download', {
      statusCode: 200,
      body: 'content',
    }).as('keyboardDownload');

    // Focus en el primer botón de descarga usando tab
    cy.get('[data-testid^="document-item-"]')
      .first()
      .within(() => {
        cy.get('button[aria-label*="descarg"], button[aria-label*="Descarg"]', { timeout: 5000 })
          .focus()
          .type('{enter}');
      });

    cy.wait('@keyboardDownload');
  });
});
