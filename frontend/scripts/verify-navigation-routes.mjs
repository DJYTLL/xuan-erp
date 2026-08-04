import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

const currentDir = dirname(fileURLToPath(import.meta.url));
const routerSource = readFileSync(resolve(currentDir, '../src/router/index.ts'), 'utf8');

const expectedMenuPaths = [
  '/workbench',
  '/product',
  '/party',
  '/warehouse',
  '/inventory',
  '/procurement',
  '/sales',
  '/finance',
  '/document',
  '/manufacturing',
  '/report',
  '/system',
  '/system/iam/menus',
  '/system/iam/permissions',
  '/system/iam/roles',
  '/system/iam/users',
  '/system/audit/logs',
  '/system/audit/interface-costs',
  '/system/audit/sql-rankings',
];

function hasRouteFor(path) {
  const childPath = path.slice(1);
  return routerSource.includes(`path: '${childPath}'`) || routerSource.includes(`redirect: '${path}'`);
}

const missing = expectedMenuPaths.filter((path) => !hasRouteFor(path));

if (missing.length > 0) {
  console.error(`Missing frontend routes for IAM menu paths: ${missing.join(', ')}`);
  process.exit(1);
}

const placeholderMarkers = [
  'ModulePlaceholderView',
  "name: 'party-placeholder'",
  "name: 'warehouse-placeholder'",
  "name: 'inventory-placeholder'",
  "name: 'sales-placeholder'",
  "name: 'finance-placeholder'",
  "name: 'document-placeholder'",
  "name: 'manufacturing-placeholder'",
  "name: 'report-placeholder'",
];
const remainingPlaceholderMarkers = placeholderMarkers.filter((marker) => routerSource.includes(marker));

if (remainingPlaceholderMarkers.length > 0) {
  console.error([
    'Frontend placeholder navigation components should be removed.',
    remainingPlaceholderMarkers.length > 0 ? `Remaining placeholder markers: ${remainingPlaceholderMarkers.join(', ')}` : '',
  ].filter(Boolean).join('\n'));
  process.exit(1);
}

console.log(`Verified ${expectedMenuPaths.length} IAM menu paths are covered by real frontend routes.`);
