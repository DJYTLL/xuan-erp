<template>
  <el-drawer
    :model-value="modelValue"
    :title="title"
    :size="width"
    class="detail-drawer"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <slot name="header" />
    <el-descriptions :column="column" border>
      <el-descriptions-item v-for="item in items" :key="item.key" :label="item.label" :span="item.span || 1">
        <slot :name="item.key" :record="record" :value="record[item.key]">
          {{ formatValue(item) }}
        </slot>
      </el-descriptions-item>
    </el-descriptions>
    <slot />
  </el-drawer>
</template>

<script setup lang="ts">
export type DetailItem = {
  key: string;
  label: string;
  span?: number;
  formatter?: (value: unknown, record: Record<string, unknown>) => string;
};

const props = withDefaults(defineProps<{
  modelValue: boolean;
  title: string;
  items: DetailItem[];
  record: Record<string, unknown>;
  width?: string | number;
  column?: number;
}>(), {
  width: '520px',
  column: 1,
});

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void;
}>();

function formatValue(item: DetailItem) {
  const value = props.record[item.key];
  if (item.formatter) {
    return item.formatter(value, props.record);
  }
  return value === undefined || value === null || value === '' ? '-' : String(value);
}
</script>

