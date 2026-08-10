<template>
  <div class="state-action-matrix">
    <el-table :data="states" row-key="stateCode" border size="small">
      <el-table-column prop="stateName" label="状态" min-width="150">
        <template #default="{ row }">
          <div class="state-cell">
            <span class="state-name">{{ row.stateName }}</span>
            <span class="state-code">{{ row.stateCode }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column
        v-for="action in actions"
        :key="action.actionCode"
        :label="action.actionName"
        min-width="110"
        align="center"
      >
        <template #default="{ row }">
          <el-checkbox
            :model-value="isChecked(row.resourceKey, row.stateCode, action.actionCode)"
            :disabled="readonly || row.resourceKey !== action.resourceKey"
            @change="(checked: boolean) => toggleRule(row.resourceKey, row.stateCode, action.actionCode, checked)"
          />
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
export interface StateActionMatrixState {
  resourceKey: string;
  stateCode: string;
  stateName: string;
}

export interface StateActionMatrixAction {
  resourceKey: string;
  actionCode: string;
  actionName: string;
}

export interface StateActionMatrixRule {
  resourceKey: string;
  stateCode: string;
  actionCode: string;
}

const props = withDefaults(defineProps<{
  states: StateActionMatrixState[];
  actions: StateActionMatrixAction[];
  modelValue: StateActionMatrixRule[];
  readonly?: boolean;
}>(), {
  readonly: false,
});

const emit = defineEmits<{
  'update:modelValue': [value: StateActionMatrixRule[]];
}>();

function normalizeResource(value: string) {
  return value.trim().toLowerCase();
}

function normalizeState(value: string) {
  return value.trim().toUpperCase();
}

function normalizeAction(value: string) {
  return value.trim().toLowerCase();
}

function ruleKey(resourceKey: string, stateCode: string, actionCode: string) {
  return `${normalizeResource(resourceKey)}:${normalizeState(stateCode)}:${normalizeAction(actionCode)}`;
}

function isChecked(resourceKey: string, stateCode: string, actionCode: string) {
  const key = ruleKey(resourceKey, stateCode, actionCode);
  return props.modelValue.some((rule) => ruleKey(rule.resourceKey, rule.stateCode, rule.actionCode) === key);
}

function toggleRule(resourceKey: string, stateCode: string, actionCode: string, checked: boolean) {
  const key = ruleKey(resourceKey, stateCode, actionCode);
  const current = props.modelValue.filter((rule) => ruleKey(rule.resourceKey, rule.stateCode, rule.actionCode) !== key);
  if (!checked) {
    emit('update:modelValue', current);
    return;
  }
  emit('update:modelValue', [
    ...current,
    {
      resourceKey: normalizeResource(resourceKey),
      stateCode: normalizeState(stateCode),
      actionCode: normalizeAction(actionCode),
    },
  ]);
}
</script>

<style scoped>
.state-action-matrix {
  width: 100%;
  overflow: auto;
}

.state-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.state-name {
  color: #111827;
  font-size: 13px;
}

.state-code {
  color: #6b7280;
  font-size: 12px;
  word-break: break-all;
}
</style>
