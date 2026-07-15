<template>
  <section class="search-action-bar">
    <div class="search-action-fields">
      <slot />
    </div>

    <div class="search-action-buttons">
      <el-tooltip v-if="showReset" :content="resetTooltip" placement="top">
        <el-button :icon="RefreshCw" circle :disabled="disabled" @click="emit('reset')" />
      </el-tooltip>
      <el-button v-if="showSearch" type="primary" :loading="loading" :disabled="disabled" @click="emit('search')">
        {{ searchText }}
      </el-button>
      <slot name="actions" />
    </div>
  </section>
</template>

<script setup lang="ts">
import { RefreshCw } from 'lucide-vue-next';

withDefaults(defineProps<{
  searchText?: string;
  resetTooltip?: string;
  showReset?: boolean;
  showSearch?: boolean;
  loading?: boolean;
  disabled?: boolean;
}>(), {
  searchText: '搜索',
  resetTooltip: '重置',
  showReset: true,
  showSearch: true,
  loading: false,
  disabled: false,
});

const emit = defineEmits<{
  'reset': [];
  'search': [];
}>();
</script>

<style scoped>
.search-action-bar {
  min-width: 0;
  border: 1px solid var(--xuan-border);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 18px;
  background: #fff;
}

.search-action-fields {
  min-width: 0;
  display: flex;
  flex: 1 1 auto;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 12px 14px;
}

.search-action-buttons {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

:global(.search-action-field) {
  min-width: 0;
  display: flex;
  flex: 0 1 auto;
  flex-direction: column;
  gap: 6px;
}

:global(.search-action-field__label) {
  color: var(--xuan-muted);
  font-size: 12px;
  font-weight: 500;
  line-height: 18px;
}

:global(.search-action-field > .el-input),
:global(.search-action-field > .el-select),
:global(.search-action-field > .el-date-editor),
:global(.search-action-field > .xuan-decimal-input) {
  width: 100%;
}

:global(:root.xuan-dark) .search-action-bar {
  background: var(--xuan-panel);
}

@media (max-width: 960px) {
  .search-action-bar {
    align-items: stretch;
    flex-direction: column;
  }

  .search-action-buttons {
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  :global(.search-action-field) {
    width: 100% !important;
    flex-basis: 100% !important;
  }
}
</style>
