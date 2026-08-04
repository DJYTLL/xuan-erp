<template>
  <el-dialog
    :model-value="modelValue"
    :width="dialogWidth"
    :style="dialogStyle"
    :class="[
      'dynamic-form-dialog',
      {
        'dynamic-form-dialog--workspace': variant === 'workspace',
        'dynamic-form-dialog--slot-only': !renderForm,
      },
    ]"
    align-center
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

    <div :class="[
      variant === 'workspace' ? 'workspace-dialog-body' : '',
      !renderForm ? 'dynamic-form-dialog-slot-body' : '',
    ]">
      <el-form
        v-if="renderForm"
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
                  :disabled="field.disabled"
                  type="textarea"
                  :rows="field.rows || 3"
                  clearable
                />
                <XuanDecimalInput
                  v-else-if="field.component === 'number'"
                  :model-value="decimalFieldValue(field.key)"
                  :placeholder="field.placeholder"
                  :disabled="field.disabled"
                  :clearable="field.clearable"
                  :scale="field.scale ?? 0"
                  :input-mode="field.inputMode || 'numeric'"
                  :align="field.align || 'right'"
                  :prefix="field.prefix"
                  :suffix="field.suffix"
                  class="dynamic-form-number"
                  @update:model-value="localModel[field.key] = $event"
                />
                <el-input
                  v-else-if="field.component === 'password'"
                  v-model="localModel[field.key]"
                  :placeholder="field.placeholder"
                  :disabled="field.disabled"
                  type="password"
                  show-password
                  clearable
                />
                <el-switch
                  v-else-if="field.component === 'switch'"
                  v-model="localModel[field.key]"
                  :disabled="field.disabled"
                  :active-text="field.activeText"
                  :inactive-text="field.inactiveText"
                />
                <el-select
                  v-else-if="field.component === 'select'"
                  v-model="localModel[field.key]"
                  :placeholder="field.placeholder"
                  :disabled="field.disabled"
                  clearable
                  class="dynamic-form-control"
                >
                  <el-option v-for="option in field.options || []" :key="option.value" :label="option.label" :value="option.value" />
                </el-select>
                <el-radio-group
                  v-else-if="field.component === 'radio-group'"
                  v-model="localModel[field.key]"
                  :disabled="field.disabled"
                  class="dynamic-form-control"
                >
                  <el-radio-button v-if="field.radioStyle === 'button'" v-for="option in field.options || []" :key="option.value" :label="option.value">
                    {{ option.label }}
                  </el-radio-button>
                  <el-radio v-else v-for="option in field.options || []" :key="option.value" :label="option.value">
                    {{ option.label }}
                  </el-radio>
                </el-radio-group>
                <el-checkbox-group
                  v-else-if="field.component === 'checkbox-group'"
                  v-model="localModel[field.key]"
                  :disabled="field.disabled"
                  :class="[
                    'dynamic-form-checkbox-group',
                    { 'dynamic-form-checkbox-group--cards': field.optionStyle === 'card' },
                  ]"
                >
                  <el-checkbox v-for="option in field.options || []" :key="option.value" :label="option.value">
                    <span class="dynamic-form-option-label">{{ option.label }}</span>
                    <span v-if="option.description" class="dynamic-form-option-description">{{ option.description }}</span>
                  </el-checkbox>
                </el-checkbox-group>
                <el-tree-select
                  v-else-if="field.component === 'tree-select'"
                  v-model="localModel[field.key]"
                  :data="field.treeOptions || []"
                  :props="field.treeProps || defaultTreeSelectProps"
                  :node-key="field.nodeKey || 'value'"
                  :placeholder="field.placeholder"
                  :disabled="field.disabled"
                  :clearable="field.clearable ?? true"
                  :check-strictly="field.checkStrictly ?? true"
                  :filterable="field.filterable ?? true"
                  :render-after-expand="field.renderAfterExpand ?? false"
                  :filter-node-method="field.filterNodeMethod"
                  class="dynamic-form-control"
                />
                <el-date-picker
                  v-else-if="field.component === 'date'"
                  v-model="localModel[field.key]"
                  :placeholder="field.placeholder"
                  :disabled="field.disabled"
                  class="dynamic-form-control"
                  type="date"
                  value-format="YYYY-MM-DD"
                />
                <el-date-picker
                  v-else-if="field.component === 'datetime'"
                  v-model="localModel[field.key]"
                  :placeholder="field.placeholder"
                  :disabled="field.disabled"
                  class="dynamic-form-control"
                  type="datetime"
                  value-format="YYYY-MM-DDTHH:mm:ssZ"
                />
                <el-input v-else v-model="localModel[field.key]" :placeholder="field.placeholder" :disabled="field.disabled" clearable />
              </el-form-item>
              <p v-if="field.description" class="dynamic-form-field-description">{{ field.description }}</p>
            </el-col>
          </el-row>
        </section>
      </el-form>

      <div v-if="$slots.body" class="dynamic-form-body-slot">
        <slot name="body" :model="localModel" :form="localModel" />
      </div>

      <section v-if="renderForm && showCustomFields" class="dynamic-form-section custom-fields-section">
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
      <slot name="footer" :close="closeDialog" :submit="submit">
        <el-button @click="closeDialog">{{ cancelText }}</el-button>
        <PermissionButton
          type="primary"
          :permission="confirmPermission"
          :disabled-reason="confirmDisabledReason"
          no-permission-mode="disable"
          :loading="loading"
          @click="submit"
        >
          {{ confirmText }}
        </PermissionButton>
      </slot>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { SlidersHorizontal } from 'lucide-vue-next';
import PermissionButton from '@/framework/components/PermissionButton.vue';
import XuanDecimalInput from '@/framework/components/XuanDecimalInput.vue';

export type DynamicFormField = {
  key: string;
  label: string;
  component?: 'input' | 'textarea' | 'number' | 'password' | 'switch' | 'select' | 'radio-group' | 'checkbox-group' | 'tree-select' | 'date' | 'datetime';
  placeholder?: string;
  required?: boolean;
  description?: string;
  options?: Array<{ label: string; value: string | number; description?: string }>;
  optionStyle?: 'default' | 'card';
  treeOptions?: DynamicTreeSelectOption[];
  treeProps?: {
    label?: string;
    value?: string;
    children?: string;
    disabled?: string;
  };
  nodeKey?: string;
  span?: number;
  rows?: number;
  min?: number;
  max?: number;
  disabled?: boolean;
  clearable?: boolean;
  scale?: number;
  inputMode?: 'decimal' | 'numeric' | 'text';
  align?: 'left' | 'center' | 'right';
  prefix?: string;
  suffix?: string;
  activeText?: string;
  inactiveText?: string;
  radioStyle?: 'default' | 'button';
  checkStrictly?: boolean;
  filterable?: boolean;
  renderAfterExpand?: boolean;
  filterNodeMethod?: (query: string, data: any) => boolean;
};

export type DynamicTreeSelectOption = {
  label: string;
  value: string | number;
  disabled?: boolean;
  children?: DynamicTreeSelectOption[];
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
  fields?: DynamicFormField[];
  sections?: DynamicFormSection[];
  customFields?: DynamicCustomField[];
  model?: Record<string, unknown>;
  variant?: 'standard' | 'workspace';
  size?: 'sm' | 'md' | 'lg';
  workspaceSize?: 'sm' | 'md' | 'lg';
  renderForm?: boolean;
  description?: string;
  helperText?: string;
  showCustomFields?: boolean;
  width?: string | number;
  labelWidth?: string;
  labelPosition?: 'left' | 'right' | 'top';
  confirmText?: string;
  cancelText?: string;
  confirmPermission?: string;
  confirmDisabledReason?: string;
  loading?: boolean;
}>(), {
  variant: 'standard',
  labelWidth: '92px',
  labelPosition: 'right',
  confirmText: '保存',
  cancelText: '取消',
  confirmPermission: '',
  confirmDisabledReason: '',
  loading: false,
  size: 'md',
  workspaceSize: 'lg',
  renderForm: true,
  fields: () => [],
  model: () => ({}),
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
const sizeWidthMap = {
  sm: '520px',
  md: '680px',
  lg: '760px',
} as const;
const workspaceSizeMap = {
  sm: {
    width: 'min(66vw, calc(100vw - 24px))',
    height: 'min(72vh, calc(100vh - 48px))',
    minWidth: 'min(548px, calc(100vw - 24px))',
    minHeight: 'min(374px, calc(100vh - 48px))',
  },
  md: {
    width: 'min(80vw, calc(100vw - 24px))',
    height: 'min(87vh, calc(100vh - 48px))',
    minWidth: 'min(661px, calc(100vw - 24px))',
    minHeight: 'min(452px, calc(100vh - 48px))',
  },
  lg: {
    width: 'min(92vw, calc(100vw - 24px))',
    height: 'calc(100vh - 48px)',
    minWidth: 'min(760px, calc(100vw - 24px))',
    minHeight: 'min(520px, calc(100vh - 48px))',
  },
} as const;
const defaultTreeSelectProps = {
  label: 'label',
  value: 'value',
  children: 'children',
  disabled: 'disabled',
};

const dialogWidth = computed(() => {
  if (props.width !== undefined && props.width !== null && props.width !== '') {
    return props.width;
  }
  if (props.variant === 'workspace') {
    return 'var(--dynamic-form-workspace-width)';
  }
  return `min(${sizeWidthMap[props.size]}, calc(100vw - 24px))`;
});

const dialogStyle = computed(() => {
  if (props.variant !== 'workspace') {
    return {};
  }
  const layout = workspaceSizeMap[props.workspaceSize];
  return {
    '--dynamic-form-workspace-width': layout.width,
    '--dynamic-form-workspace-height': layout.height,
    '--dynamic-form-workspace-min-width': layout.minWidth,
    '--dynamic-form-workspace-min-height': layout.minHeight,
  };
});

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

function closeDialog() {
  emit('update:modelValue', false);
}

function addCustomField() {
  customFieldRows.value.push({ name: '', value: '' });
}

function removeCustomField(index: number) {
  customFieldRows.value.splice(index, 1);
}

function decimalFieldValue(key: string) {
  const value = localModel[key];
  if (typeof value === 'string' || typeof value === 'number' || value == null) {
    return value;
  }
  return '';
}

async function submit() {
  await formRef.value?.validate();
  emit('submit', { ...localModel, customFields: customFieldRows.value.map((field) => ({ ...field })) });
}
</script>
