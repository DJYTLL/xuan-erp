import { existsSync, readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

const currentDir = dirname(fileURLToPath(import.meta.url));
const frontendRoot = resolve(currentDir, '..');

function read(relativePath) {
  return readFileSync(resolve(frontendRoot, relativePath), 'utf8');
}

function assertFile(relativePath) {
  if (!existsSync(resolve(frontendRoot, relativePath))) {
    throw new Error(`Missing file: ${relativePath}`);
  }
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

assertFile('src/framework/components/XuanDateTimeRangePicker.vue');

const packageSource = read('package.json');
const appSource = read('src/App.vue');
const pickerSource = read('src/framework/components/XuanDateTimeRangePicker.vue');
const componentCenterSource = read('src/views/ComponentCenterView.vue');

assertIncludes(packageSource, 'verify:date-range-component', 'package.json should expose date range component verification.');
assertIncludes(appSource, 'el-config-provider', 'App should provide Element Plus locale globally.');
assertIncludes(appSource, 'ElConfigProvider', 'App should import/register ElConfigProvider so the locale provider is active.');
assertIncludes(appSource, 'elementLocale', 'App should compute Element Plus locale from current app locale.');
assertIncludes(appSource, "dayjs/locale/zh-cn", 'App should load Day.js zh-cn locale so date panel month names are Chinese.');
assertIncludes(appSource, "month1: '一月'", 'Element Plus Chinese date picker should display January as 一月.');
assertIncludes(appSource, "month12: '十二月'", 'Element Plus Chinese date picker should display December as 十二月.');
assertIncludes(pickerSource, "type: 'datetimerange'", 'Date range component should default to Element Plus datetimerange.');
assertIncludes(pickerSource, 'startPlaceholder', 'Date range component should expose start placeholder.');
assertIncludes(pickerSource, 'endPlaceholder', 'Date range component should expose end placeholder.');
assertIncludes(pickerSource, 'valueFormat', 'Date range component should expose backend-friendly value format.');
assertIncludes(pickerSource, ':single-panel="singlePanel"', 'Date range component should render range picker with one calendar panel by default.');
assertIncludes(pickerSource, 'singlePanel?: boolean', 'Date range component should expose singlePanel override.');
assertIncludes(pickerSource, 'singlePanel: true', 'Date range component should default to a single calendar panel.');
assertIncludes(pickerSource, ':popper-class="resolvedPopperClass"', 'Date range component should attach a dedicated popper class for panel layout fixes.');
assertIncludes(pickerSource, 'xuan-date-range-picker-popper', 'Date range component should use a stable popper class.');
assertIncludes(pickerSource, '.xuan-date-range-picker-popper .el-date-range-picker__time-header', 'Date range picker popper should widen the time header.');
assertIncludes(pickerSource, '.xuan-date-range-picker-popper .el-date-range-picker__time-picker-wrap', 'Date range picker popper should size date/time inputs.');
assertIncludes(pickerSource, 'normalizedModelValue', 'Date range component should normalize model values before passing them to Element Plus.');
assertIncludes(pickerSource, 'normalizeTimestampValue', 'Date range component should parse millisecond timestamp strings as numbers.');
assertIncludes(pickerSource, 'normalizeSameDayFullDayRange', 'Date range component should expand same-day datetime selection to a full day.');
assertIncludes(pickerSource, 'emitDateRangeValue', 'Date range component should normalize date range values before emitting.');
assertIncludes(pickerSource, 'showShortcuts', 'Date range component should allow range shortcuts to be toggled.');
assertIncludes(pickerSource, 'buildDateRangeShortcuts', 'Date range component should build locale-aware date range shortcuts.');
assertIncludes(pickerSource, '今日', 'Date range component should include a today shortcut label.');
assertIncludes(pickerSource, '本周', 'Date range component should include a this-week shortcut label.');
assertIncludes(pickerSource, '本月', 'Date range component should include a this-month shortcut label.');
assertIncludes(pickerSource, '本季', 'Date range component should include a this-quarter shortcut label.');
assertIncludes(pickerSource, '本年', 'Date range component should include a this-year shortcut label.');
assertIncludes(pickerSource, 'This Quarter', 'Date range component should include English shortcut labels.');
assertIncludes(pickerSource, 'table-date-range--compact', 'Date range component should preserve compact WMS date range styling.');
assertIncludes(componentCenterSource, 'XuanDateTimeRangePicker', 'Component center should preview XuanDateTimeRangePicker.');
assertIncludes(componentCenterSource, 'dateRangeDemo', 'Component center should hold date range demo state.');
assertIncludes(componentCenterSource, '日期组件', 'Component center should include a date component panel.');
assertIncludes(componentCenterSource, 'dateTimeRange: null', 'Date time range demo should not prefill a default date.');
assertIncludes(componentCenterSource, 'dateRange: null', 'Date range demo should not prefill a default date.');
assertIncludes(componentCenterSource, 'disabledRange: null', 'Disabled date range demo should not prefill a default date.');
assertNotIncludes(componentCenterSource, '1783670400000', 'Component center should not prefill timestamp demo dates.');

console.log('Date range component verification passed.');
