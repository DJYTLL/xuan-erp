<template>
  <el-tooltip v-if="visible" :content="tooltipReason" :disabled="!tooltipReason">
    <el-button :type="type" :disabled="isDisabled" v-bind="$attrs">
      <slot />
    </el-button>
  </el-tooltip>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useFrameworkPermissionChecker } from '@/framework/auth/permissionChecker';

defineOptions({ inheritAttrs: false });

const props = withDefaults(defineProps<{
  permission?: string;
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info' | '';
  disabledReason?: string;
  noPermissionMode?: 'hide' | 'disable';
}>(), {
  type: '',
  disabledReason: '',
  noPermissionMode: 'hide',
});

const hasButtonPermission = useFrameworkPermissionChecker();

const hasPermission = computed(() => hasButtonPermission(props.permission));
const visible = computed(() => hasPermission.value || props.noPermissionMode === 'disable');
const isDisabled = computed(() => Boolean(props.disabledReason) || !hasPermission.value);
const tooltipReason = computed(() => {
  if (props.disabledReason) {
    return props.disabledReason;
  }
  return hasPermission.value ? '' : '无按钮权限';
});
</script>
