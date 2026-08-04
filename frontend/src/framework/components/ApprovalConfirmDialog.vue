<template>
  <el-dialog
    :model-value="modelValue"
    :title="copy.title"
    :width="width"
    class="approval-confirm-dialog"
    align-center
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div class="confirm-summary" :class="`confirm-summary-${copy.type}`">
      <strong>{{ copy.heading }}</strong>
      <span>{{ description || copy.description }}</span>
    </div>
    <el-form label-width="78px" class="confirm-form">
      <el-form-item label="对象">
        <el-input :model-value="targetName || '-'" disabled />
      </el-form-item>
      <el-form-item :label="remarkLabel">
        <el-input v-model="remark" type="textarea" :rows="3" :placeholder="remarkPlaceholder" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button :type="copy.type" :loading="loading" @click="confirm">{{ copy.confirmText }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { ElMessage } from 'element-plus/es/components/message/index';

type ApprovalAction = 'approve' | 'reject' | 'submit';

const props = withDefaults(defineProps<{
  modelValue: boolean;
  action: ApprovalAction;
  targetName?: string;
  description?: string;
  requireRemark?: boolean;
  width?: string | number;
  loading?: boolean;
}>(), {
  targetName: '',
  description: '',
  requireRemark: false,
  width: 520,
  loading: false,
});

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void;
  (event: 'confirm', value: { action: ApprovalAction; remark: string }): void;
}>();

const actionCopy: Record<ApprovalAction, { title: string; heading: string; description: string; confirmText: string; type: 'primary' | 'success' | 'danger' }> = {
  approve: { title: '审核确认', heading: '确认审核通过？', description: '审核通过后单据将进入已审核状态。', confirmText: '审核通过', type: 'success' },
  reject: { title: '驳回确认', heading: '确认驳回？', description: '驳回后单据将回到待处理状态。', confirmText: '确认驳回', type: 'danger' },
  submit: { title: '提交确认', heading: '确认提交？', description: '提交后单据将进入审核流程。', confirmText: '确认提交', type: 'primary' },
};

const remark = ref('');
const copy = computed(() => actionCopy[props.action]);
const remarkLabel = computed(() => (props.action === 'reject' ? '驳回原因' : '备注'));
const remarkPlaceholder = computed(() => (props.requireRemark ? '请输入处理说明' : '可填写处理说明'));

watch(() => props.modelValue, (visible) => {
  if (visible) {
    remark.value = '';
  }
});

function confirm() {
  if (props.requireRemark && !remark.value.trim()) {
    ElMessage.warning('请填写处理说明');
    return;
  }
  emit('confirm', { action: props.action, remark: remark.value.trim() });
}
</script>
