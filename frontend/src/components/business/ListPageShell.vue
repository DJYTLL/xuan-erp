<template>
  <section class="list-page-shell">
    <header class="list-page-header">
      <div class="list-page-heading">
        <h1 v-if="title || $slots.title" class="list-page-title">
          <slot name="title">{{ title }}</slot>
        </h1>
        <p v-if="description || $slots.description" class="list-page-description">
          <slot name="description">{{ description }}</slot>
        </p>
      </div>

      <div v-if="$slots.actions" class="list-page-actions">
        <slot name="actions" />
      </div>
    </header>

    <section v-if="$slots.query" class="list-page-query">
      <slot name="query" />
    </section>

    <section class="list-page-table">
      <slot />
    </section>

    <footer v-if="$slots.pagination" class="list-page-pagination">
      <slot name="pagination" />
    </footer>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  title?: string;
  description?: string;
}>();
</script>

<style scoped>
.list-page-shell {
  min-width: 0;
  display: grid;
  gap: 16px;
}

.list-page-header {
  min-width: 0;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.list-page-heading {
  min-width: 0;
}

.list-page-title {
  margin: 0;
  color: var(--xuan-text);
  font-size: 24px;
  font-weight: 700;
  line-height: 1.35;
}

.list-page-description {
  margin: 6px 0 0;
  color: var(--xuan-muted);
  font-size: 14px;
  line-height: 1.6;
}

.list-page-actions,
.list-page-pagination {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.list-page-query,
.list-page-table {
  min-width: 0;
}

@media (max-width: 720px) {
  .list-page-header {
    flex-direction: column;
  }

  .list-page-actions,
  .list-page-pagination {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
