<template>
  <AppState type="forbidden" title="无权访问" description="当前账号没有访问该页面的权限，请联系租户管理员开通权限。">
    <template #actions>
      <el-button type="primary" @click="router.push('/dashboard')">返回仪表盘</el-button>
    </template>
  </AppState>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import AppState from '@/components/business/AppState.vue';

defineOptions({ name: 'ForbiddenView' });

const router = useRouter();
</script>
