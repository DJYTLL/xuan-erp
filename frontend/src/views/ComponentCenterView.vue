<template>
  <section>
    <h1 class="page-title">{{ t('nav.components') }}</h1>

    <div class="component-grid">
      <article v-for="card in cards" :key="card.title" class="component-card">
        <h3>{{ card.title }}</h3>
        <p>{{ card.description }}</p>
        <div class="demo-row">
          <template v-if="card.kind === 'buttons'">
            <PermissionButton type="primary" permission="demo:add">新增</PermissionButton>
            <PermissionButton permission="demo:import">导入</PermissionButton>
            <PermissionButton permission="demo:export">导出</PermissionButton>
          </template>
          <template v-else-if="card.kind === 'tags'">
            <el-tag>普通商品</el-tag>
            <el-tag type="success">启用</el-tag>
            <el-tag type="warning">待审核</el-tag>
          </template>
          <template v-else-if="card.kind === 'theme'">
            <ThemeSwitcher />
            <LanguageSwitcher />
          </template>
          <template v-else-if="card.kind === 'dialogs'">
            <el-button type="primary" @click="formVisible = true">表单弹窗</el-button>
            <el-button @click="drawerVisible = true">详情抽屉</el-button>
            <el-button type="success" @click="approvalVisible = true">审核确认</el-button>
            <el-button type="danger" @click="batchVisible = true">批量确认</el-button>
          </template>
          <template v-else>
            <el-button size="small">搜索</el-button>
            <el-button size="small">重置</el-button>
            <el-button size="small">列设置</el-button>
          </template>
        </div>
      </article>
    </div>

    <DynamicFormDialog
      v-model="formVisible"
      title="新增供应商"
      :fields="demoFields"
      :model="demoForm"
      @submit="submitDemoForm"
    />
    <DetailDrawer
      v-model="drawerVisible"
      title="供应商详情"
      :items="demoDetailItems"
      :record="demoRecord"
    />
    <ApprovalConfirmDialog
      v-model="approvalVisible"
      action="approve"
      target-name="PO-DRAFT-0001"
      description="确认后该采购单进入已审核状态，可继续生成入库单。"
      @confirm="confirmApproval"
    />
    <BatchConfirmDialog
      v-model="batchVisible"
      title="批量停用确认"
      action-name="批量停用"
      tone="warning"
      :selected-count="3"
      :items="['华东供应商', '华南供应商', '默认仓库供应商']"
      @confirm="confirmBatch"
    />
  </section>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import LanguageSwitcher from '@/components/app/LanguageSwitcher.vue';
import ThemeSwitcher from '@/components/app/ThemeSwitcher.vue';
import ApprovalConfirmDialog from '@/components/business/ApprovalConfirmDialog.vue';
import BatchConfirmDialog from '@/components/business/BatchConfirmDialog.vue';
import DetailDrawer from '@/components/business/DetailDrawer.vue';
import DynamicFormDialog from '@/components/business/DynamicFormDialog.vue';
import type { DynamicFormField } from '@/components/business/DynamicFormDialog.vue';
import PermissionButton from '@/components/business/PermissionButton.vue';

defineOptions({ name: 'ComponentCenterView' });

const { t } = useI18n();

const cards = [
  { title: '查询表单', description: '复用在商品、供应商、订单列表页，字段布局与按钮区保持一致。', kind: 'query' },
  { title: '状态标签', description: '库存状态、订单状态、同步状态复用统一色板和尺寸。', kind: 'tags' },
  { title: '权限按钮', description: '按钮权限、列权限和未来权限提示都从这一层统一约束。', kind: 'buttons' },
  { title: '表格工具栏', description: '列设置、刷新、密度切换、批量操作集中在右上角。', kind: 'query' },
  { title: '抽屉与弹窗', description: '表单弹窗、详情抽屉、审核确认、批量确认保持同一节奏。', kind: 'dialogs' },
  { title: '主题与语言', description: '直接验证主题色切换和中英文案是否同步影响组件。', kind: 'theme' },
];

const formVisible = ref(false);
const drawerVisible = ref(false);
const approvalVisible = ref(false);
const batchVisible = ref(false);

const demoForm = reactive({
  name: '华东供应商',
  code: 'SUP-001',
  type: 'company',
  contact: '张三',
  remark: '',
});

const demoFields: DynamicFormField[] = [
  { key: 'name', label: '名称', required: true, placeholder: '请输入供应商名称' },
  { key: 'code', label: '编码', required: true, placeholder: '请输入编码' },
  {
    key: 'type',
    label: '类型',
    component: 'select',
    required: true,
    options: [
      { label: '企业', value: 'company' },
      { label: '个人', value: 'person' },
    ],
  },
  { key: 'contact', label: '联系人', placeholder: '请输入联系人' },
  { key: 'remark', label: '备注', component: 'textarea', span: 24 },
];

const demoRecord = {
  name: '华东供应商',
  code: 'SUP-001',
  type: '企业',
  contact: '张三',
  status: '启用',
};

const demoDetailItems = [
  { key: 'name', label: '名称' },
  { key: 'code', label: '编码' },
  { key: 'type', label: '类型' },
  { key: 'contact', label: '联系人' },
  { key: 'status', label: '状态' },
];

function submitDemoForm(value: Record<string, unknown>) {
  Object.assign(demoForm, value);
  formVisible.value = false;
  ElMessage.success('表单弹窗提交成功');
}

function confirmApproval() {
  approvalVisible.value = false;
  ElMessage.success('审核确认已提交');
}

function confirmBatch() {
  batchVisible.value = false;
  ElMessage.success('批量操作已确认');
}
</script>
