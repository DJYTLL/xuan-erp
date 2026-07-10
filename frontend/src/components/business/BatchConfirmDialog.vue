<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    :width="width"
    class="batch-confirm-dialog"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div class="confirm-summary" :class="`confirm-summary-${tone}`">
      <strong>{{ actionName }}</strong>
      <span>当前已选择 {{ selectedCount }} 条数据，请确认是否继续。</span>
    </div>
    <ul v-if="items.length" class="batch-item-list">
      <li v-for="item in items" :key="item">{{ item }}</li>
    </ul>
    <el-input v-if="confirmKeyword" v-model="keyword" :placeholder="`输入 ${confirmKeyword} 后继续`" />

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button :type="tone" :disabled="confirmDisabled" :loading="loading" @click="confirm">{{ actionName }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';

const props = withDefaults(defineProps<{
  modelValue: boolean;
  title: string;
  actionName: string;
  selectedCount: number;
  items?: string[];
  tone?: 'primary' | 'success' | 'warning' | 'danger';
  confirmKeyword?: string;
  width?: string | number;
  loading?: boolean;
}>(), {
  items: () => [],
  tone: 'danger',
  confirmKeyword: '',
  width: 500,
  loading: false,
});

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void;
  (event: 'confirm'): void;
}>();

const keyword = ref('');
const confirmDisabled = computed(() => props.selectedCount < 1 || Boolean(props.confirmKeyword && keyword.value !== props.confirmKeyword));

watch(() => props.modelValue, (visible) => {
  if (visible) {
    keyword.value = '';
  }
});

function confirm() {
  if (!confirmDisabled.value) {
    emit('confirm');
  }
}
</script>

