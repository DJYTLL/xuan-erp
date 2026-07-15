import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

const currentDir = dirname(fileURLToPath(import.meta.url));
const layoutSource = readFileSync(resolve(currentDir, '../src/layouts/AppLayout.vue'), 'utf8');
const styleSource = readFileSync(resolve(currentDir, '../src/styles/shell.css'), 'utf8');

const requiredLayoutMarkers = [
  'class="menu-root"',
  'class="menu-label l1"',
  'class="submenu-l2"',
  'class="menu-label l2"',
  'class="submenu-l3"',
  'class="menu-label l3"',
  'handleMenuClick',
  'isMenuItemActive',
];

const requiredStyleMarkers = [
  '.menu-root',
  '.menu-label',
  '.menu-label.l1',
  '.submenu-l2',
  '.submenu-l3',
  '.menu-dot',
  '.slide-down-enter-active',
  '.sidebar-footer',
];

const missingLayoutMarkers = requiredLayoutMarkers.filter((marker) => !layoutSource.includes(marker));
const missingStyleMarkers = requiredStyleMarkers.filter((marker) => !styleSource.includes(marker));
const staleRouterLinks = /<RouterLink[\s\S]*?class="side-item/.test(layoutSource);

if (missingLayoutMarkers.length || missingStyleMarkers.length || staleRouterLinks) {
  if (missingLayoutMarkers.length) {
    console.error(`Missing sidebar layout markers: ${missingLayoutMarkers.join(', ')}`);
  }
  if (missingStyleMarkers.length) {
    console.error(`Missing sidebar style markers: ${missingStyleMarkers.join(', ')}`);
  }
  if (staleRouterLinks) {
    console.error('Sidebar still uses the old side-item RouterLink pattern.');
  }
  process.exit(1);
}

const l2PaddingMatch = styleSource.match(/\.menu-label\.l2\s*\{[\s\S]*?padding-left:\s*(\d+)px;/);
const l3PaddingMatch = styleSource.match(/\.menu-label\.l3\s*\{[\s\S]*?padding-left:\s*(\d+)px;/);

if (!l2PaddingMatch || !l3PaddingMatch) {
  console.error('Sidebar nested menu levels should define explicit padding-left values.');
  process.exit(1);
}

const l2Padding = Number(l2PaddingMatch[1]);
const l3Padding = Number(l3PaddingMatch[1]);

if (!(l2Padding >= 24)) {
  console.error(`Sidebar level-2 padding is too small: ${l2Padding}px`);
  process.exit(1);
}

if (!(l3Padding > l2Padding)) {
  console.error(`Sidebar level-3 padding should be deeper than level-2: l2=${l2Padding}px, l3=${l3Padding}px`);
  process.exit(1);
}

console.log('Verified sidebar navigation follows the reference menu pattern.');
