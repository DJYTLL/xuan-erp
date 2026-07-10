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
          <template v-else>
            <el-button size="small">搜索</el-button>
            <el-button size="small">重置</el-button>
            <el-button size="small">列设置</el-button>
          </template>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import LanguageSwitcher from '@/components/app/LanguageSwitcher.vue';
import ThemeSwitcher from '@/components/app/ThemeSwitcher.vue';
import PermissionButton from '@/components/business/PermissionButton.vue';

defineOptions({ name: 'ComponentCenterView' });

const { t } = useI18n();

const cards = [
  { title: '查询表单', description: '复用在商品、供应商、订单列表页，字段布局与按钮区保持一致。', kind: 'query' },
  { title: '状态标签', description: '库存状态、订单状态、同步状态复用统一色板和尺寸。', kind: 'tags' },
  { title: '权限按钮', description: '按钮权限、列权限和未来权限提示都从这一层统一约束。', kind: 'buttons' },
  { title: '表格工具栏', description: '列设置、刷新、密度切换、批量操作集中在右上角。', kind: 'query' },
  { title: '抽屉与弹窗', description: '表单弹窗、详情抽屉、二次确认弹层保持同一节奏。', kind: 'query' },
  { title: '主题与语言', description: '直接验证主题色切换和中英文案是否同步影响组件。', kind: 'theme' },
];
</script>
