<template>
  <el-input
    v-model="localValue"
    :class="['xuan-decimal-input', `xuan-decimal-input--${align}`]"
    :placeholder="placeholder"
    :disabled="disabled"
    :clearable="clearable"
    :inputmode="inputMode"
    :size="size"
    @blur="emit('blur')"
    @focus="emit('focus')"
  >
    <template v-if="prefix" #prefix>{{ prefix }}</template>
    <template v-if="suffix" #suffix>{{ suffix }}</template>
  </el-input>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = withDefaults(defineProps<{
  modelValue?: string | number | null;
  scale?: number;
  placeholder?: string;
  disabled?: boolean;
  clearable?: boolean;
  allowNegative?: boolean;
  inputMode?: 'decimal' | 'numeric' | 'text';
  size?: 'large' | 'default' | 'small';
  align?: 'left' | 'center' | 'right';
  prefix?: string;
  suffix?: string;
}>(), {
  scale: 2,
  placeholder: '',
  disabled: false,
  clearable: false,
  allowNegative: false,
  inputMode: 'decimal',
  size: 'default',
  align: 'center',
  prefix: '',
  suffix: '',
});

const emit = defineEmits<{
  (event: 'update:modelValue', value: string): void;
  (event: 'blur'): void;
  (event: 'focus'): void;
}>();

function normalizeDecimalInput(value: string, scale: number, allowNegative = false) {
  const trimmed = value.trim();
  const hasMinus = allowNegative && trimmed.startsWith('-');
  const cleaned = trimmed.replace(/[^\d.]/g, '');
  if (!cleaned) {
    return hasMinus ? '-' : '';
  }

  const firstDot = cleaned.indexOf('.');
  let integerPart = cleaned;
  let decimalPart = '';
  if (firstDot !== -1) {
    integerPart = cleaned.slice(0, firstDot);
    decimalPart = cleaned.slice(firstDot + 1).replace(/\./g, '');
  }
  if (integerPart === '' && firstDot !== -1) {
    integerPart = '0';
  }
  if (!decimalPart) {
    const normalized = trimmed.endsWith('.') ? `${integerPart}.` : integerPart;
    return hasMinus ? `-${normalized}` : normalized;
  }

  const normalized = `${integerPart}.${decimalPart.slice(0, Math.max(0, scale))}`;
  return hasMinus ? `-${normalized}` : normalized;
}

const localValue = computed({
  get: () => (props.modelValue == null ? '' : String(props.modelValue)),
  set: (value: string) => {
    emit('update:modelValue', normalizeDecimalInput(value, props.scale, props.allowNegative));
  },
});
</script>

<style scoped>
.xuan-decimal-input {
  width: 100%;
}

.xuan-decimal-input--right :deep(.el-input__inner) {
  text-align: right;
}

.xuan-decimal-input--center :deep(.el-input__inner) {
  text-align: center;
}

.xuan-decimal-input--left :deep(.el-input__inner) {
  text-align: left;
}
</style>
