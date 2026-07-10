<template>
  <section class="app-state">
    <div class="app-state-icon">
      <component :is="iconComponent" :size="34" />
    </div>
    <h2>{{ title }}</h2>
    <p>{{ description }}</p>
    <div v-if="$slots.actions" class="app-state-actions">
      <slot name="actions" />
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Ban, Inbox, SearchX, ShieldAlert } from 'lucide-vue-next';

const props = withDefaults(defineProps<{
  type?: 'empty' | 'error' | 'forbidden' | 'notFound';
  title: string;
  description: string;
}>(), {
  type: 'empty',
});

const iconComponent = computed(() => {
  if (props.type === 'forbidden') {
    return ShieldAlert;
  }
  if (props.type === 'notFound') {
    return SearchX;
  }
  if (props.type === 'error') {
    return Ban;
  }
  return Inbox;
});
</script>

<style scoped>
.app-state {
  min-height: 360px;
  display: grid;
  align-content: center;
  justify-items: center;
  gap: 10px;
  color: var(--xuan-muted);
  text-align: center;
}

.app-state-icon {
  width: 64px;
  height: 64px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  color: var(--xuan-primary);
  background: color-mix(in srgb, var(--xuan-primary) 12%, transparent);
}

.app-state h2 {
  margin: 8px 0 0;
  color: var(--xuan-text);
  font-size: 22px;
}

.app-state p {
  max-width: 420px;
  margin: 0;
  line-height: 1.7;
}

.app-state-actions {
  margin-top: 8px;
  display: flex;
  gap: 10px;
}
</style>
