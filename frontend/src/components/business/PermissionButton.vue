<template>
  <el-tooltip :content="disabledReason" :disabled="!disabledReason">
    <el-button :type="type" :disabled="Boolean(disabledReason)">
      <slot />
    </el-button>
  </el-tooltip>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  permission?: string;
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info' | '';
  disabledReason?: string;
}>(), {
  type: '',
  disabledReason: '',
});
</script>
