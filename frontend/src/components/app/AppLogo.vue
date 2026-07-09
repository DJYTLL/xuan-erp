<template>
  <RouterLink class="app-logo" to="/dashboard">
    <span class="app-logo-mark">X</span>
    <span class="app-logo-text">{{ compact ? 'Xuan' : 'Xuan ERP' }}</span>
  </RouterLink>
</template>

<script setup lang="ts">
defineProps<{
  compact?: boolean;
}>();
</script>
