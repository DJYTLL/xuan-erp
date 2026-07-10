<template>
  <section>
    <h1 class="page-title">{{ t('nav.dashboard') }}</h1>

    <div class="metric-grid">
      <article v-for="metric in metrics" :key="metric.label" class="metric-card">
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
      </article>
    </div>

    <div class="dashboard-grid">
      <section class="page-panel">
        <h3>业务趋势</h3>
        <div class="trend-list">
          <div v-for="item in trends" :key="item.label" class="trend-item">
            <span>{{ item.label }}</span>
            <span class="trend-track"><span class="trend-fill" :style="{ width: `${item.value}%` }" /></span>
            <span>{{ item.value }}%</span>
          </div>
        </div>
      </section>

      <section class="page-panel">
        <h3>系统状态</h3>
        <el-table :data="services" size="small">
          <el-table-column prop="name" label="服务" />
          <el-table-column prop="status" label="状态" />
          <el-table-column prop="note" label="备注" />
        </el-table>
      </section>
    </div>
  </section>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n';

defineOptions({ name: 'DashboardView' });

const { t } = useI18n();

const metrics = [
  { label: '今日订单', value: 128 },
  { label: '待入库单', value: 43 },
  { label: '低库存预警', value: 12 },
  { label: '活跃租户', value: 8 },
];

const trends = [
  { label: '采购', value: 72 },
  { label: '销售', value: 88 },
  { label: '出库', value: 64 },
  { label: '回款', value: 55 },
];

const services = [
  { name: 'Gateway', status: '正常', note: '路由与鉴权入口' },
  { name: 'IAM', status: '正常', note: '登录与用户上下文' },
  { name: 'Tenant', status: '稳定', note: '租户基础资料' },
  { name: 'RocketMQ', status: '关注', note: '等待完整事件链路' },
];
</script>
