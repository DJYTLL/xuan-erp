<template>
  <el-dialog
    :model-value="modelValue"
    :width="width"
    :class="['dynamic-form-dialog', { 'dynamic-form-dialog--workspace': variant === 'workspace' }]"
    :draggable="variant === 'workspace'"
    overflow
    destroy-on-close
    @closed="resetForm"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <template #header>
      <div class="dynamic-dialog-head">
        <h2>{{ title }}</h2>
        <p v-if="description">{{ description }}</p>
        <span v-if="helperText">{{ helperText }}</span>
      </div>
    </template>

    <div :class="variant === 'workspace' ? 'workspace-dialog-body' : ''">
      <el-form
        ref="formRef"
        :model="localModel"
        :rules="rules"
        :label-width="labelWidth"
        :label-position="labelPosition"
        class="dynamic-form"
      >
        <section
          v-for="section in displaySections"
          :key="section.title || section.fields.map((field) => field.key).join('-')"
          :class="['dynamic-form-section', { 'dynamic-form-section--plain': !section.title }]"
        >
          <div v-if="section.title || section.description" class="dynamic-form-section-head">
            <h3 v-if="section.title">{{ section.title }}</h3>
            <p v-if="section.description">{{ section.description }}</p>
          </div>
          <el-row :gutter="16">
            <el-col v-for="field in section.fields" :key="field.key" :span="field.span || 8">
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
        </section>
      </el-form>

      <section v-if="showCustomFields" class="dynamic-form-section custom-fields-section">
        <div class="dynamic-form-section-head custom-fields-head">
          <h3>自定义字段</h3>
          <el-button text :icon="SlidersHorizontal">列设置</el-button>
        </div>
        <el-table :data="customFieldRows" border height="92" class="custom-fields-table">
          <el-table-column label="字段名" min-width="180">
            <template #default="{ row }">
              <el-input v-model="row.name" placeholder="字段名" clearable />
            </template>
          </el-table-column>
          <el-table-column label="字段值" min-width="220">
            <template #default="{ row }">
              <el-input v-model="row.value" placeholder="字段值" clearable />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90">
            <template #default="{ $index }">
              <el-button link type="danger" @click="removeCustomField($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-button class="custom-field-add" size="small" type="primary" plain @click="addCustomField">添加字段</el-button>
      </section>
    </div>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">{{ cancelText }}</el-button>
      <el-button type="primary" :loading="loading" @click="submit">{{ confirmText }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { SlidersHorizontal } from 'lucide-vue-next';

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

export type DynamicFormSection = {
  title?: string;
  description?: string;
  fields: DynamicFormField[];
};

export type DynamicCustomField = {
  name: string;
  value: string;
};

const props = withDefaults(defineProps<{
  modelValue: boolean;
  title: string;
  fields: DynamicFormField[];
  sections?: DynamicFormSection[];
  customFields?: DynamicCustomField[];
  model: Record<string, unknown>;
  variant?: 'standard' | 'workspace';
  description?: string;
  helperText?: string;
  showCustomFields?: boolean;
  width?: string | number;
  labelWidth?: string;
  labelPosition?: 'left' | 'right' | 'top';
  confirmText?: string;
  cancelText?: string;
  loading?: boolean;
}>(), {
  width: 680,
  variant: 'standard',
  labelWidth: '92px',
  labelPosition: 'right',
  confirmText: '保存',
  cancelText: '取消',
  loading: false,
  showCustomFields: false,
  customFields: () => [],
  sections: () => [],
});

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void;
  (event: 'submit', value: Record<string, unknown>): void;
}>();

const formRef = ref<FormInstance>();
const localModel = reactive<Record<string, unknown>>({});
const customFieldRows = ref<DynamicCustomField[]>([]);

const displaySections = computed<DynamicFormSection[]>(() => {
  return props.sections.length ? props.sections : [{ fields: props.fields }];
});

const rules = computed<FormRules>(() => {
  return displaySections.value.flatMap((section) => section.fields).reduce<FormRules>((result, field) => {
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
    customFieldRows.value = props.customFields.map((field) => ({ ...field }));
  },
  { immediate: true, deep: true },
);

function resetForm() {
  formRef.value?.clearValidate();
}

function addCustomField() {
  customFieldRows.value.push({ name: '', value: '' });
}

function removeCustomField(index: number) {
  customFieldRows.value.splice(index, 1);
}

async function submit() {
  await formRef.value?.validate();
  emit('submit', { ...localModel, customFields: customFieldRows.value.map((field) => ({ ...field })) });
}
</script>
