<template>
  <el-config-provider :locale="elementLocale">
    <RouterView />
  </el-config-provider>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElConfigProvider } from 'element-plus/es/components/config-provider/index';
import zhCn from 'element-plus/es/locale/lang/zh-cn';
import en from 'element-plus/es/locale/lang/en';
import 'dayjs/locale/zh-cn';
import { useSettingsStore } from '@/stores/settings';

const { locale } = useI18n();
const settings = useSettingsStore();
const xuanZhCnLocale = {
  ...zhCn,
  el: {
    ...zhCn.el,
    datepicker: {
      ...zhCn.el.datepicker,
      month1: '一月',
      month2: '二月',
      month3: '三月',
      month4: '四月',
      month5: '五月',
      month6: '六月',
      month7: '七月',
      month8: '八月',
      month9: '九月',
      month10: '十月',
      month11: '十一月',
      month12: '十二月',
    },
  },
};

watch(
  () => settings.locale,
  (nextLocale) => {
    if (locale.value !== nextLocale) {
      locale.value = nextLocale;
    }
  },
  { immediate: true },
);

const elementLocale = computed(() => (locale.value === 'en-US' ? en : xuanZhCnLocale));
</script>
