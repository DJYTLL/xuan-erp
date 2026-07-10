<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    :width="width"
    class="dynamic-form-dialog"
    destroy-on-close
    @closed="resetForm"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form ref="formRef" :model="localModel" :rules="rules" :label-width="labelWidth" class="dynamic-form">
      <el-row :gutter="14">
        <el-col v-for="field in fields" :key="field.key" :span="field.span || 12">
          <el-form-item :label="field.label" :prop="field.key">
            <el-input
              v-if="field.component === 'textarea'"
              v-model="localModel[field.key]"
              :placeholder="field.placeholder"
              type="textarea"
              :rows="field.rows || 3"
              clearable
            />
            <el-input-number
              v-else-if="field.component === 'number'"
              v-model="localModel[field.key]"
              :min="field.min"
              :max="field.max"
              :placeholder="field.placeholder"
              controls-position="right"
              class="dynamic-form-number"
            />
            <el-select
              v-else-if="field.component === 'select'"
              v-model="localModel[field.key]"
              :placeholder="field.placeholder"
              clearable
              class="dynamic-form-control"
            >
              <el-option v-for="option in field.options || []" :key="option.value" :label="option.label" :value="option.value" />
            </el-select>
            <el-date-picker
              v-else-if="field.component === 'date'"
              v-model="localModel[field.key]"
              :placeholder="field.placeholder"
              class="dynamic-form-control"
              type="date"
              value-format="YYYY-MM-DD"
            />
            <el-input v-else v-model="localModel[field.key]" :placeholder="field.placeholder" clearable />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">{{ cancelText }}</el-button>
      <el-button type="primary" :loading="loading" @click="submit">{{ confirmText }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';

export type DynamicFormField = {
  key: string;
  label: string;
  component?: 'input' | 'textarea' | 'number' | 'select' | 'date';
  placeholder?: string;
  required?: boolean;
  options?: Array<{ label: string; value: string | number }>;
  span?: number;
  rows?: number;
  min?: number;
  max?: number;
};

const props = withDefaults(defineProps<{
  modelValue: boolean;
  title: string;
  fields: DynamicFormField[];
  model: Record<string, unknown>;
  width?: string | number;
  labelWidth?: string;
  confirmText?: string;
  cancelText?: string;
  loading?: boolean;
}>(), {
  width: 680,
  labelWidth: '92px',
  confirmText: '保存',
  cancelText: '取消',
  loading: false,
});

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void;
  (event: 'submit', value: Record<string, unknown>): void;
}>();

const formRef = ref<FormInstance>();
const localModel = reactive<Record<string, unknown>>({});

const rules = computed<FormRules>(() => {
  return props.fields.reduce<FormRules>((result, field) => {
    if (field.required) {
      result[field.key] = [{ required: true, message: `请输入${field.label}`, trigger: ['blur', 'change'] }];
    }
    return result;
  }, {});
});

watch(
  () => [props.modelValue, props.model],
  () => {
    Object.keys(localModel).forEach((key) => delete localModel[key]);
    Object.assign(localModel, props.model);
  },
  { immediate: true, deep: true },
);

function resetForm() {
  formRef.value?.clearValidate();
}

async function submit() {
  await formRef.value?.validate();
  emit('submit', { ...localModel });
}
</script>

