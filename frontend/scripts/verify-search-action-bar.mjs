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

function assertNotIncludes(source, marker, message) {
  if (source.includes(marker)) {
    throw new Error(message);
  }
}

const componentPath = 'src/framework/components/SearchActionBar.vue';
if (!existsSync(resolve(root, componentPath))) {
  throw new Error(`Missing reusable search bar component: ${componentPath}`);
}

const componentSource = read(componentPath);
const productSource = read('src/views/ProductManagementView.vue');
const componentCenterSource = read('src/views/ComponentCenterView.vue');

assertIncludes(componentSource, 'class="search-action-bar"', 'SearchActionBar should own the toolbar shell.');
assertIncludes(componentSource, 'defineEmits', 'SearchActionBar should emit reset/search actions.');
assertIncludes(componentSource, "'reset'", 'SearchActionBar should expose reset action.');
assertIncludes(componentSource, "'search'", 'SearchActionBar should expose search action.');
assertIncludes(componentSource, '<slot name="actions"', 'SearchActionBar should expose right-side actions slot.');

assertIncludes(productSource, '<SearchActionBar', 'ProductManagementView should use SearchActionBar.');
assertIncludes(productSource, '@reset="resetFilters"', 'ProductManagementView should wire reset through SearchActionBar.');
assertIncludes(productSource, '@search="search"', 'ProductManagementView should wire search through SearchActionBar.');
assertIncludes(productSource, "import SearchActionBar from '@/framework/components/SearchActionBar.vue';", 'ProductManagementView should import SearchActionBar.');
assertNotIncludes(productSource, "import QueryToolbar from '@/framework/components/QueryToolbar.vue';", 'ProductManagementView should no longer depend on QueryToolbar for this bar.');
assertNotIncludes(productSource, "import { RefreshCw } from 'lucide-vue-next';", 'ProductManagementView should not own the refresh icon after extraction.');

assertIncludes(componentCenterSource, '<SearchActionBar', 'ComponentCenterView should preview SearchActionBar.');
assertIncludes(componentCenterSource, "import SearchActionBar from '@/framework/components/SearchActionBar.vue';", 'ComponentCenterView should import SearchActionBar.');
assertIncludes(componentCenterSource, '@reset="resetDemoSearch"', 'ComponentCenterView should wire the SearchActionBar reset demo.');
assertIncludes(componentCenterSource, '@search="submitDemoSearch"', 'ComponentCenterView should wire the SearchActionBar search demo.');

console.log('Verified reusable SearchActionBar extraction and product page usage.');
