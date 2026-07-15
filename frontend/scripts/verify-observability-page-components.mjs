import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

const currentDir = dirname(fileURLToPath(import.meta.url));
const frontendRoot = resolve(currentDir, '..');

function read(relativePath) {
  return readFileSync(resolve(frontendRoot, relativePath), 'utf8');
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

const interfaceCostSource = read('src/views/InterfaceCostView.vue');
const sqlRankingSource = read('src/views/SqlRankingView.vue');
const searchActionBarSource = read('src/framework/components/SearchActionBar.vue');

assertIncludes(searchActionBarSource, 'search-action-field', 'SearchActionBar should style labeled query fields.');
assertIncludes(searchActionBarSource, 'search-action-field__label', 'SearchActionBar should style query field labels.');

assertIncludes(interfaceCostSource, '<SearchActionBar', 'InterfaceCostView should use the project SearchActionBar.');
assertIncludes(interfaceCostSource, "import SearchActionBar from '@/framework/components/SearchActionBar.vue';", 'InterfaceCostView should import SearchActionBar.');
assertIncludes(interfaceCostSource, '<XuanDateTimeRangePicker', 'InterfaceCostView should use the project date range component.');
assertIncludes(interfaceCostSource, "import XuanDateTimeRangePicker", 'InterfaceCostView should import XuanDateTimeRangePicker.');
assertIncludes(interfaceCostSource, '<XuanDecimalInput', 'InterfaceCostView should use the project decimal input for limit.');
assertIncludes(interfaceCostSource, "import XuanDecimalInput", 'InterfaceCostView should import XuanDecimalInput.');
assertIncludes(interfaceCostSource, '<label class="search-action-field interface-service-field">', 'InterfaceCostView should label the service filter.');
assertIncludes(interfaceCostSource, '<span class="search-action-field__label">服务</span>', 'InterfaceCostView should show the service filter label.');
assertIncludes(interfaceCostSource, '<label class="search-action-field interface-endpoint-field">', 'InterfaceCostView should label the endpoint filter.');
assertIncludes(interfaceCostSource, '<span class="search-action-field__label">接口</span>', 'InterfaceCostView should show the endpoint filter label.');
assertIncludes(interfaceCostSource, '<label class="search-action-field interface-time-field">', 'InterfaceCostView should label the time range filter.');
assertIncludes(interfaceCostSource, '<span class="search-action-field__label">时间区间</span>', 'InterfaceCostView should show the time range label.');
assertIncludes(interfaceCostSource, '<label class="search-action-field interface-limit-field">', 'InterfaceCostView should label the limit filter.');
assertIncludes(interfaceCostSource, '<span class="search-action-field__label">条数</span>', 'InterfaceCostView should show the limit label.');
assertIncludes(interfaceCostSource, "const serviceName = ref('');", 'InterfaceCostView service filter should not have a default value.');
assertIncludes(interfaceCostSource, "serviceName.value = '';", 'InterfaceCostView reset should clear the service filter.');
assertNotIncludes(interfaceCostSource, "const serviceName = ref('xuan-iam');", 'InterfaceCostView should not default service to xuan-iam.');
assertNotIncludes(interfaceCostSource, "serviceName.value = 'xuan-iam';", 'InterfaceCostView reset should not restore xuan-iam.');
assertNotIncludes(interfaceCostSource, "import QueryToolbar from '@/framework/components/QueryToolbar.vue';", 'InterfaceCostView should not use QueryToolbar.');
assertNotIncludes(interfaceCostSource, '<el-date-picker', 'InterfaceCostView should not use raw Element Plus date picker.');
assertNotIncludes(interfaceCostSource, '<el-input-number', 'InterfaceCostView should not use raw Element Plus input number.');

assertIncludes(sqlRankingSource, '<SearchActionBar', 'SqlRankingView should use the project SearchActionBar.');
assertIncludes(sqlRankingSource, "import SearchActionBar from '@/framework/components/SearchActionBar.vue';", 'SqlRankingView should import SearchActionBar.');
assertIncludes(sqlRankingSource, '<XuanDecimalInput', 'SqlRankingView should use the project decimal input for limit.');
assertIncludes(sqlRankingSource, "import XuanDecimalInput", 'SqlRankingView should import XuanDecimalInput.');
assertIncludes(sqlRankingSource, '<label class="search-action-field sql-database-field">', 'SqlRankingView should label the database filter.');
assertIncludes(sqlRankingSource, '<span class="search-action-field__label">数据库</span>', 'SqlRankingView should show the database label.');
assertIncludes(sqlRankingSource, '<label class="search-action-field sql-sort-field">', 'SqlRankingView should label the sort filter.');
assertIncludes(sqlRankingSource, '<span class="search-action-field__label">排序</span>', 'SqlRankingView should show the sort label.');
assertIncludes(sqlRankingSource, '<label class="search-action-field sql-limit-field">', 'SqlRankingView should label the limit filter.');
assertIncludes(sqlRankingSource, '<span class="search-action-field__label">条数</span>', 'SqlRankingView should show the limit label.');
assertIncludes(sqlRankingSource, 'const databaseNames = ref<string[]>([]);', 'SqlRankingView database filter should not have default selected databases.');
assertIncludes(sqlRankingSource, 'databaseNames.value = [];', 'SqlRankingView reset should clear database selection.');
assertIncludes(sqlRankingSource, 'databaseNames: normalizeDatabaseNames(databaseNames.value)', 'SqlRankingView should omit empty databaseNames from requests.');
assertIncludes(sqlRankingSource, 'function normalizeDatabaseNames', 'SqlRankingView should normalize empty database selection.');
assertNotIncludes(sqlRankingSource, "const databaseNames = ref(['xuan_iam', 'xuan_tenant', 'xuan_audit']);", 'SqlRankingView should not default selected databases.');
assertNotIncludes(sqlRankingSource, "databaseNames.value = ['xuan_iam', 'xuan_tenant', 'xuan_audit'];", 'SqlRankingView reset should not restore default databases.');
assertNotIncludes(sqlRankingSource, "import QueryToolbar from '@/framework/components/QueryToolbar.vue';", 'SqlRankingView should not use QueryToolbar.');
assertNotIncludes(sqlRankingSource, '<el-input-number', 'SqlRankingView should not use raw Element Plus input number.');

console.log('Observability page component usage verification passed.');
