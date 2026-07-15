<template>
  <RouterLink class="app-logo" :to="appFrameworkConfig.routes.homePath">
    <span class="app-logo-mark">X</span>
    <span class="app-logo-text">{{ compact ? shortAppName : appFrameworkConfig.shell.appName }}</span>
  </RouterLink>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { appFrameworkConfig } from '@/app/frameworkConfig';

const props = defineProps<{
  compact?: boolean;
}>();

const shortAppName = computed(() => props.compact
  ? appFrameworkConfig.shell.appName.split(/\s+/)[0] || appFrameworkConfig.shell.appName
  : appFrameworkConfig.shell.appName);
</script>
