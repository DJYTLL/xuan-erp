<template>
  <el-dropdown trigger="click" @command="setLocale">
    <button class="shell-icon-button" type="button" :aria-label="t('shell.language')">
      <Languages :size="17" />
    </button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="zh-CN">中文</el-dropdown-item>
        <el-dropdown-item command="en-US">English</el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
import { Languages } from 'lucide-vue-next';
import { useI18n } from 'vue-i18n';
import { useSettingsStore, type LocaleCode } from '@/stores/settings';

const { t, locale } = useI18n();
const settings = useSettingsStore();

function setLocale(value: string) {
  const nextLocale = value as LocaleCode;
  settings.setLocale(nextLocale);
  locale.value = nextLocale;
}
</script>
