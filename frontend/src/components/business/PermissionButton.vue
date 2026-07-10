<template>
  <el-tooltip :content="disabledReason" :disabled="!disabledReason">
    <el-button :type="type" :disabled="Boolean(disabledReason)" v-bind="$attrs">
      <slot />
    </el-button>
  </el-tooltip>
</template>

<script setup lang="ts">
defineOptions({ inheritAttrs: false });

withDefaults(defineProps<{
  permission?: string;
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info' | '';
  disabledReason?: string;
}>(), {
  type: '',
  disabledReason: '',
});
</script>
