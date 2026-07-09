<template>
  <el-dropdown trigger="click" @command="setTheme">
    <button class="shell-icon-button" type="button" :aria-label="t('shell.color')">
      <Palette :size="17" />
    </button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-for="theme in themes"
          :key="theme.value"
          :command="theme.value"
        >
          <span class="theme-option">
            <span class="theme-swatch" :style="{ backgroundColor: theme.color }" />
            {{ theme.label }}
          </span>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import { Palette } from 'lucide-vue-next';
import { useI18n } from 'vue-i18n';
import { useSettingsStore, type ThemeColor } from '@/stores/settings';

const { t } = useI18n();
const settings = useSettingsStore();
const themes: Array<{ value: ThemeColor; label: string; color: string }> = [
  { value: 'blue', label: 'Blue', color: '#1677ff' },
  { value: 'green', label: 'Green', color: '#0f9d76' },
  { value: 'orange', label: 'Orange', color: '#f59e0b' },
  { value: 'indigo', label: 'Indigo', color: '#6366f1' },
];

function setTheme(value: string) {
  settings.setThemeColor(value as ThemeColor);
}

onMounted(() => settings.applyTheme());
</script>
