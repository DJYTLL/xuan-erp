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
import { useFrameworkStateActionChecker } from '@/framework/auth/stateActionChecker';

defineOptions({ inheritAttrs: false });

const props = withDefaults(defineProps<{
  permission?: string;
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info' | '';
  disabledReason?: string;
  disabled?: boolean;
  noPermissionMode?: 'hide' | 'disable';
  stateResource?: string;
  stateCode?: string | number | null;
  stateAction?: string;
  stateNoPermissionReason?: string;
}>(), {
  type: '',
  disabledReason: '',
  disabled: false,
  noPermissionMode: 'hide',
  stateResource: '',
  stateCode: null,
  stateAction: '',
  stateNoPermissionReason: '当前状态不可执行该动作',
});

const hasButtonPermission = useFrameworkPermissionChecker();
const hasStateActionAllowed = useFrameworkStateActionChecker();

const hasPermission = computed(() => hasButtonPermission(props.permission));
const hasStateActionPermission = computed(() => {
  if (!props.stateResource || !props.stateAction) {
    return true;
  }
  return hasStateActionAllowed(props.stateResource, props.stateCode, props.stateAction);
});
const visible = computed(() => (hasPermission.value && hasStateActionPermission.value) || props.noPermissionMode === 'disable');
const isDisabled = computed(() => Boolean(props.disabled) || Boolean(props.disabledReason) || !hasPermission.value || !hasStateActionPermission.value);
const tooltipReason = computed(() => {
  if (props.disabledReason) {
    return props.disabledReason;
  }
  if (!hasPermission.value) {
    return '无按钮权限';
  }
  return hasStateActionPermission.value ? '' : props.stateNoPermissionReason;
});
</script>
