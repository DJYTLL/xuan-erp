<template>
  <AppState type="notFound" title="页面不存在" description="你访问的页面不存在，可能已被移动、删除，或当前菜单尚未开放。">
    <template #actions>
      <el-button type="primary" @click="router.push('/dashboard')">返回仪表盘</el-button>
    </template>
  </AppState>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import AppState from '@/components/business/AppState.vue';

defineOptions({ name: 'NotFoundView' });

const router = useRouter();
</script>
