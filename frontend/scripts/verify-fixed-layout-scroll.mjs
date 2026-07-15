import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

const currentDir = dirname(fileURLToPath(import.meta.url));
const globalSource = readFileSync(resolve(currentDir, '../src/styles/global.css'), 'utf8');
const shellSource = readFileSync(resolve(currentDir, '../src/styles/shell.css'), 'utf8');
const listPageShellSource = readFileSync(resolve(currentDir, '../src/framework/components/ListPageShell.vue'), 'utf8');

function normalize(source) {
  return source.replace(/\s+/g, ' ');
}

function assertIncludes(source, marker, message) {
  if (!source.includes(marker)) {
    throw new Error(message);
  }
}

function assertRule(source, selector, markers) {
  const normalized = normalize(source);
  const escapedSelector = selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&').replace(/\s+/g, '\\s*');
  const ruleMatch = normalized.match(new RegExp(`${escapedSelector}\\s*\\{([^}]*)\\}`));

  if (!ruleMatch) {
    throw new Error(`Missing CSS rule: ${selector}`);
  }

  const body = ruleMatch[1];
  for (const marker of markers) {
    assertIncludes(body, marker, `Rule ${selector} is missing: ${marker}`);
  }
}

assertIncludes(
  normalize(globalSource),
  'html, body, #app { width: 100%; height: 100%; margin: 0; overflow: hidden;',
  'Root app nodes must fill the viewport and prevent page-level scrolling.',
);

assertRule(shellSource, '.app-shell', ['height: 100vh;', 'overflow: hidden;']);
assertRule(shellSource, '.app-sidebar', ['height: 100vh;', 'overflow: hidden;']);
assertRule(shellSource, '.app-main', ['min-height: 0;', 'height: 100vh;', 'overflow: hidden;']);
assertRule(shellSource, '.app-topbar', ['flex: 0 0 48px;']);
assertRule(shellSource, '.tab-strip', ['flex: 0 0 calc(40px * var(--xuan-density-scale));']);
assertRule(shellSource, '.route-loading-bar', ['flex: 0 0 2px;']);
assertRule(shellSource, '.app-content', ['flex: 1 1 0;', 'min-height: 0;', 'overflow-y: auto;', 'overflow-x: hidden;']);
assertRule(shellSource, '.nav-scroll-area', ['overflow-y: auto;', 'overscroll-behavior: contain;']);

assertRule(listPageShellSource, '.list-page-shell', ['height: 100%;', 'grid-template-rows: auto auto minmax(0, 1fr) auto;']);
assertRule(listPageShellSource, '.list-page-table', ['min-height: 0;', 'display: flex;', 'flex-direction: column;']);

console.log('Verified fixed shell layout keeps scrolling inside content/sidebar overflow areas.');
