<template>
  <el-date-picker
    :model-value="normalizedModelValue"
    :class="['xuan-date-time-range-picker', 'table-date-range', 'table-date-range--compact', customClass]"
    :type="type"
    :format="format"
    :value-format="valueFormat"
    :start-placeholder="resolvedStartPlaceholder"
    :end-placeholder="resolvedEndPlaceholder"
    :range-separator="resolvedRangeSeparator"
    :clearable="clearable"
    :disabled="disabled"
    :unlink-panels="unlinkPanels"
    :single-panel="singlePanel"
    :popper-class="resolvedPopperClass"
    :shortcuts="showShortcuts ? shortcuts : []"
    @update:model-value="emitDateRangeValue('update:modelValue', $event as DateRangeValue)"
    @change="emitDateRangeValue('change', $event as DateRangeValue)"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';

export type DateRangeValue = [string, string] | [number, number] | null;
type DateRangeShortcut = {
  text: string;
  value: () => [Date, Date];
};

const props = withDefaults(defineProps<{
  modelValue: DateRangeValue;
  type?: 'datetimerange' | 'daterange';
  format?: string;
  valueFormat?: string;
  startPlaceholder?: string;
  endPlaceholder?: string;
  rangeSeparator?: string;
  clearable?: boolean;
  disabled?: boolean;
  unlinkPanels?: boolean;
  singlePanel?: boolean;
  popperClass?: string;
  customClass?: string;
  showShortcuts?: boolean;
}>(), {
  type: 'datetimerange',
  format: 'YYYY-MM-DD HH:mm:ss',
  valueFormat: 'x',
  clearable: true,
  disabled: false,
  unlinkPanels: false,
  singlePanel: true,
  popperClass: '',
  customClass: '',
  showShortcuts: true,
});

const emit = defineEmits<{
  (event: 'update:modelValue', value: DateRangeValue): void;
  (event: 'change', value: DateRangeValue): void;
}>();

const { locale } = useI18n();

const isEnglish = computed(() => locale.value === 'en-US');
const resolvedStartPlaceholder = computed(() => props.startPlaceholder || (isEnglish.value ? 'Start time' : '开始时间'));
const resolvedEndPlaceholder = computed(() => props.endPlaceholder || (isEnglish.value ? 'End time' : '结束时间'));
const resolvedRangeSeparator = computed(() => props.rangeSeparator || (isEnglish.value ? 'to' : '至'));
const resolvedPopperClass = computed(() => ['xuan-date-range-picker-popper', props.popperClass].filter(Boolean).join(' '));
const normalizedModelValue = computed(() => {
  if (props.valueFormat !== 'x' || !props.modelValue) {
    return props.modelValue;
  }

  return props.modelValue.map(normalizeTimestampValue) as DateRangeValue;
});
const shortcuts = computed(() => buildDateRangeShortcuts(isEnglish.value));

function normalizeTimestampValue(value: string | number) {
  if (typeof value === 'string' && /^\d{12,}$/.test(value)) {
    return Number(value);
  }

  return value;
}

function emitDateRangeValue(event: 'update:modelValue' | 'change', value: DateRangeValue) {
  const normalizedValue = normalizeSameDayFullDayRange(value);
  if (event === 'update:modelValue') {
    emit('update:modelValue', normalizedValue);
    return;
  }

  emit('change', normalizedValue);
}

function normalizeSameDayFullDayRange(value: DateRangeValue): DateRangeValue {
  if (props.type !== 'datetimerange' || !value) {
    return value;
  }

  const [startValue, endValue] = value;
  const endDate = createDateFromRangeValue(endValue);
  if (!endDate || !isStartOfDay(endDate)) {
    return value;
  }

  return [startValue, formatRangeValue(endOfDay(endDate), endValue)] as DateRangeValue;
}

function createDateFromRangeValue(value: string | number) {
  if (typeof value === 'number') {
    return new Date(value);
  }

  if (/^\d{12,}$/.test(value)) {
    return new Date(Number(value));
  }

  const normalized = value.includes(' ') ? value.replace(' ', 'T') : value;
  const date = new Date(normalized);
  return Number.isNaN(date.getTime()) ? null : date;
}

function formatRangeValue(date: Date, originalValue: string | number) {
  if (typeof originalValue === 'number') {
    return date.getTime();
  }

  if (/^\d{12,}$/.test(originalValue)) {
    return String(date.getTime());
  }

  if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(originalValue)) {
    return formatDateTime(date);
  }

  return date.getTime();
}

function isStartOfDay(date: Date) {
  return date.getHours() === 0 && date.getMinutes() === 0 && date.getSeconds() === 0 && date.getMilliseconds() === 0;
}

function formatDateTime(date: Date) {
  return `${date.getFullYear()}-${padDatePart(date.getMonth() + 1)}-${padDatePart(date.getDate())} ${padDatePart(date.getHours())}:${padDatePart(date.getMinutes())}:${padDatePart(date.getSeconds())}`;
}

function padDatePart(value: number) {
  return String(value).padStart(2, '0');
}

function buildDateRangeShortcuts(english: boolean): DateRangeShortcut[] {
  const labels = english
    ? ['Today', 'This Week', 'This Month', 'This Quarter', 'This Year']
    : ['今日', '本周', '本月', '本季', '本年'];

  return [
    { text: labels[0], value: () => getCurrentRange('today') },
    { text: labels[1], value: () => getCurrentRange('week') },
    { text: labels[2], value: () => getCurrentRange('month') },
    { text: labels[3], value: () => getCurrentRange('quarter') },
    { text: labels[4], value: () => getCurrentRange('year') },
  ];
}

function getCurrentRange(unit: 'today' | 'week' | 'month' | 'quarter' | 'year'): [Date, Date] {
  const now = new Date();

  if (unit === 'today') {
    return [startOfDay(now), endOfDay(now)];
  }

  if (unit === 'week') {
    const weekday = now.getDay();
    const mondayOffset = weekday === 0 ? -6 : 1 - weekday;
    const monday = addDays(now, mondayOffset);
    return [startOfDay(monday), endOfDay(addDays(monday, 6))];
  }

  if (unit === 'month') {
    return [new Date(now.getFullYear(), now.getMonth(), 1, 0, 0, 0, 0), new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59, 999)];
  }

  if (unit === 'quarter') {
    const quarterStartMonth = Math.floor(now.getMonth() / 3) * 3;
    return [new Date(now.getFullYear(), quarterStartMonth, 1, 0, 0, 0, 0), new Date(now.getFullYear(), quarterStartMonth + 3, 0, 23, 59, 59, 999)];
  }

  return [new Date(now.getFullYear(), 0, 1, 0, 0, 0, 0), new Date(now.getFullYear(), 11, 31, 23, 59, 59, 999)];
}

function addDays(date: Date, days: number) {
  const nextDate = new Date(date);
  nextDate.setDate(nextDate.getDate() + days);
  return nextDate;
}

function startOfDay(date: Date) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate(), 0, 0, 0, 0);
}

function endOfDay(date: Date) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate(), 23, 59, 59, 999);
}
</script>

<style scoped>
.xuan-date-time-range-picker {
  max-width: 100%;
}

.table-date-range {
  width: 380px;
  max-width: 100%;
}

.table-date-range--compact {
  flex: 0 0 380px;
}

.table-date-range--compact.el-range-editor {
  width: 380px !important;
  min-width: 380px !important;
}

.table-date-range--compact :deep(.el-range-input) {
  width: 132px;
}

@media (max-width: 768px) {
  .table-date-range,
  .table-date-range--compact,
  .table-date-range--compact.el-range-editor {
    width: 100% !important;
    min-width: 0 !important;
    flex-basis: 100%;
  }
}
</style>

<style>
.xuan-date-range-picker-popper .el-date-range-picker.single-panel.has-sidebar.has-time,
.xuan-date-range-picker-popper .el-date-range-picker.single-panel.has-sidebar {
  width: 548px;
}

.xuan-date-range-picker-popper .el-date-range-picker.single-panel .el-picker-panel__body {
  min-width: 430px;
}

.xuan-date-range-picker-popper .el-date-range-picker__time-header {
  min-width: 430px;
  padding-left: 8px;
  padding-right: 8px;
}

.xuan-date-range-picker-popper .el-date-range-picker.single-panel .el-date-range-picker__content {
  width: 400px;
}

.xuan-date-range-picker-popper .el-date-range-picker__time-picker-wrap {
  width: 76px;
  padding: 0 4px;
}

.xuan-date-range-picker-popper .el-date-range-picker__time-picker-wrap:nth-child(1),
.xuan-date-range-picker-popper .el-date-range-picker__time-picker-wrap:nth-child(4) {
  width: 100px;
}

.xuan-date-range-picker-popper .el-date-range-picker__time-picker-wrap .el-input {
  width: 100%;
}
</style>
