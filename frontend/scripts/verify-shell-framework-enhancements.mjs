import { existsSync, readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

const currentDir = dirname(fileURLToPath(import.meta.url));
const root = resolve(currentDir, '..');

function read(relativePath) {
  return readFileSync(resolve(root, relativePath), 'utf8');
}

function assertIncludes(source, marker, message) {
  if (!source.includes(marker)) {
    throw new Error(message);
  }
}

function assertFile(relativePath) {
  const absolutePath = resolve(root, relativePath);
  if (!existsSync(absolutePath)) {
    throw new Error(`Missing file: ${relativePath}`);
  }
}

const routerSource = read('src/router/index.ts');
const layoutSource = read('src/layouts/AppLayout.vue');
const shellStyleSource = read('src/styles/shell.css');
const messagesSource = read('src/i18n/messages.ts');
const httpSource = read('src/api/http.ts');
const mainSource = read('src/main.ts');

assertFile('src/api/http-error.ts');
const httpErrorSource = read('src/api/http-error.ts');

assertIncludes(routerSource, 'titleKey:', 'Routes should expose titleKey for locale-aware titles.');
assertIncludes(routerSource, 'breadcrumb: [{ titleKey:', 'Route breadcrumbs should use titleKey entries.');
assertIncludes(routerSource, 'createRouteCacheComponent', 'Routes should use named cache wrappers for reused views.');
assertIncludes(routerSource, "cacheName: 'PurchaseOrderDraftView'", 'Purchase draft route needs a unique cache name.');
assertIncludes(routerSource, "cacheName: 'PurchaseReturnApprovedView'", 'Purchase return approved route needs a unique cache name.');

assertIncludes(layoutSource, 'resolveRouteTitle', 'Layout should resolve route titles through i18n.');
assertIncludes(layoutSource, 'resolveBreadcrumbItems', 'Layout should resolve breadcrumb items through i18n.');
assertIncludes(layoutSource, 'tabTitle(tab)', 'Tab labels should update when locale changes.');
assertIncludes(layoutSource, 'viewRoute.path', 'Route component keys should not remount on query-only changes.');
assertIncludes(layoutSource, 'sidebar-flyout', 'Collapsed sidebar should expose a flyout menu.');
assertIncludes(shellStyleSource, '.sidebar-flyout', 'Collapsed sidebar flyout needs styling.');

assertIncludes(messagesSource, 'route:', 'i18n messages should include route labels.');
assertIncludes(messagesSource, 'purchaseOrderDraft', 'i18n messages should include purchase route labels.');

assertIncludes(httpSource, 'handleHttpError', 'Axios response interceptor should delegate to the global HTTP error handler.');
assertIncludes(httpErrorSource, 'installHttpErrorHandler', 'HTTP error handler should be installable from main.ts.');
assertIncludes(httpErrorSource, 'onUnauthorized', 'HTTP error handler should expose unauthorized handling.');
assertIncludes(httpErrorSource, 'onForbidden', 'HTTP error handler should expose forbidden handling.');
assertIncludes(mainSource, 'installHttpErrorHandler', 'main.ts should install the global HTTP error handler.');

console.log('Verified shell framework enhancements for i18n routes, tab cache, collapsed menus, and HTTP errors.');
