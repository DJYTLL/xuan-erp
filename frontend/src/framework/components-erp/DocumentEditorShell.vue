<template>
  <section class="document-editor-shell">
    <header class="document-editor-header">
      <div class="document-editor-heading">
        <div class="document-editor-title-row">
          <h1>{{ title }}</h1>
          <el-tag v-if="status" :type="statusType" size="small">{{ status }}</el-tag>
        </div>
        <p v-if="description">{{ description }}</p>
      </div>
      <div class="document-editor-actions">
        <slot name="actions" />
      </div>
    </header>

    <main class="document-editor-body">
      <slot />
    </main>

    <footer v-if="summaryItems.length || $slots.summaryActions" class="document-summary-bar" :class="{ 'is-sticky': stickySummary, 'sticky-summary': stickySummary }">
      <div class="document-summary-items">
        <span v-for="item in summaryItems" :key="item.label" class="document-summary-item" :class="{ strong: item.strong }">
          {{ item.label }}: <strong>{{ item.value }}</strong>
        </span>
      </div>
      <div v-if="$slots.summaryActions" class="document-summary-actions">
        <slot name="summaryActions" />
      </div>
    </footer>
  </section>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  title: string;
  description?: string;
  status?: string;
  statusType?: '' | 'success' | 'warning' | 'info' | 'danger';
  summaryItems?: Array<{ label: string; value: string; strong?: boolean }>;
  stickySummary?: boolean;
}>(), {
  description: '',
  status: '',
  statusType: 'warning',
  summaryItems: () => [],
  stickySummary: true,
});
</script>
