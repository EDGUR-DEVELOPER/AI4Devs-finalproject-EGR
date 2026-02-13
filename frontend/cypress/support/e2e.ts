/**
 * Cypress support file for E2E tests
 * This file is run before each spec file
 */

import './commands';

// Disable uncaught exception handling to avoid test failures from unrelated errors
Cypress.on('uncaught:exception', (err, runnable) => {
  // Return false to prevent Cypress from failing the test
  return false;
});
