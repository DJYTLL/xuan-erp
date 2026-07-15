<template>
  <section class="document-section">
    <header class="document-section-head">
      <div>
        <h2 class="document-section-title">{{ title }}</h2>
        <p v-if="description" class="document-section-description">{{ description }}</p>
      </div>
      <div v-if="$slots.extra" class="document-section-extra">
        <slot name="extra" />
      </div>
    </header>
    <slot />
  </section>
</template>

<script setup lang="ts">
defineProps<{
  title: string;
  description?: string;
}>();
</script>
