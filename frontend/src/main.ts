import { createApp } from 'vue';
import { createPinia } from 'pinia';
import { ElButton } from 'element-plus/es/components/button/index';
import { ElCheckbox, ElCheckboxGroup } from 'element-plus/es/components/checkbox/index';
import { ElCol } from 'element-plus/es/components/col/index';
import { ElCollapse, ElCollapseItem } from 'element-plus/es/components/collapse/index';
import { ElDatePicker } from 'element-plus/es/components/date-picker/index';
import { ElDescriptions, ElDescriptionsItem } from 'element-plus/es/components/descriptions/index';
import { ElDialog } from 'element-plus/es/components/dialog/index';
import { ElDrawer } from 'element-plus/es/components/drawer/index';
import { ElDropdown, ElDropdownItem, ElDropdownMenu } from 'element-plus/es/components/dropdown/index';
import { ElEmpty } from 'element-plus/es/components/empty/index';
import { ElForm, ElFormItem } from 'element-plus/es/components/form/index';
import { ElInput } from 'element-plus/es/components/input/index';
import { ElInputNumber } from 'element-plus/es/components/input-number/index';
import { ElLoading } from 'element-plus/es/components/loading/index';
import { ElOption, ElSelect } from 'element-plus/es/components/select/index';
import { ElPagination } from 'element-plus/es/components/pagination/index';
import { ElPopover } from 'element-plus/es/components/popover/index';
import { ElRadio } from 'element-plus/es/components/radio/index';
import { ElRow } from 'element-plus/es/components/row/index';
import { ElSegmented } from 'element-plus/es/components/segmented/index';
import { ElSwitch } from 'element-plus/es/components/switch/index';
import { ElTable, ElTableColumn } from 'element-plus/es/components/table/index';
import { ElTag } from 'element-plus/es/components/tag/index';
import { ElTimeline, ElTimelineItem } from 'element-plus/es/components/timeline/index';
import { ElTooltip } from 'element-plus/es/components/tooltip/index';
import { ElTree } from 'element-plus/es/components/tree/index';
import { ElTreeSelect } from 'element-plus/es/components/tree-select/index';
import 'element-plus/dist/index.css';
import App from './App.vue';
import { installHttpErrorHandler } from './api/http-error';
import { getUserPreference, saveUserPreference } from './api/preferences';
import { appFrameworkConfig } from './app/frameworkConfig';
import { frameworkPermissionCheckerKey } from './framework/auth/permissionChecker';
import { listenAuthSessionRefreshed } from './framework/auth/sessionEvents';
import { frameworkPreferenceAdapterKey } from './framework/preferences/preferenceAdapter';
import { i18n } from './i18n';
import { router } from './router';
import { useAuthorizationStore } from './stores/authorization';
import { useAuthStore } from './stores/auth';
import './styles/global.css';
import './styles/login.css';
import './styles/shell.css';

const pinia = createPinia();
const app = createApp(App);

const elementPlusComponents = [
  ElButton,
  ElCheckbox,
  ElCheckboxGroup,
  ElCol,
  ElCollapse,
  ElCollapseItem,
  ElDatePicker,
  ElDescriptions,
  ElDescriptionsItem,
  ElDialog,
  ElDrawer,
  ElDropdown,
  ElDropdownItem,
  ElDropdownMenu,
  ElEmpty,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElOption,
  ElPagination,
  ElPopover,
  ElRadio,
  ElRow,
  ElSegmented,
  ElSelect,
  ElSwitch,
  ElTable,
  ElTableColumn,
  ElTag,
  ElTimeline,
  ElTimelineItem,
  ElTooltip,
  ElTree,
  ElTreeSelect,
];

app
  .use(pinia)
  .use(router)
  .use(i18n)
  .use(ElLoading);

for (const component of elementPlusComponents) {
  app.use(component);
}

app.provide(frameworkPermissionCheckerKey, (permission) =>
  useAuthorizationStore(pinia).hasButtonPermission(permission));
app.provide(frameworkPreferenceAdapterKey, {
  getPreference: getUserPreference,
  savePreference: saveUserPreference,
});

installHttpErrorHandler({
  translateErrorCode: translateApiErrorCode,
  onUnauthorized: () => {
    const authStore = useAuthStore(pinia);
    const currentRoute = router.currentRoute.value;
    const redirect = currentRoute.name === appFrameworkConfig.routes.loginRouteName ? undefined : currentRoute.fullPath;
    void authStore.logout();
    if (currentRoute.name !== appFrameworkConfig.routes.loginRouteName) {
      router.push({ name: appFrameworkConfig.routes.loginRouteName, query: redirect ? { redirect } : undefined });
    }
  },
  onForbidden: () => {
    if (router.currentRoute.value.name !== 'forbidden') {
      router.push({ name: 'forbidden' });
    }
  },
});

listenAuthSessionRefreshed(async (session) => {
  const authStore = useAuthStore(pinia);
  const authorizationStore = useAuthorizationStore(pinia);
  authStore.applySession(session);
  try {
    await authorizationStore.loadPermissionSnapshot();
  } catch {
    authorizationStore.clear();
  }
});

app.mount('#app');

function translateApiErrorCode(code: string) {
  const key = `apiError.${code}`;
  const translated = i18n.global.t(key);
  return translated === key ? '' : translated;
}
