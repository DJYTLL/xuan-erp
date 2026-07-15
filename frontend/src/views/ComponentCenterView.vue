<template>
  <section>
    <h1 class="page-title">{{ t('nav.components') }}</h1>

    <el-collapse v-model="componentOverviewPanels" class="component-preview-collapse component-overview-collapse">
      <el-collapse-item name="overview-query" class="component-collapse-panel">
        <template #title>
          <div class="component-panel-title">
            <strong>查询表单</strong>
            <span>复用在商品、供应商、订单列表页，字段布局与按钮区保持一致。</span>
          </div>
        </template>
        <section class="component-preview-panel" :data-demo-kind="renderOverviewDemo('query')">
          <div class="demo-row">
            <el-button size="small">搜索</el-button>
            <el-button size="small">重置</el-button>
            <el-button size="small">列设置</el-button>
          </div>
        </section>
      </el-collapse-item>

      <el-collapse-item name="overview-tags" class="component-collapse-panel">
        <template #title>
          <div class="component-panel-title">
            <strong>状态标签</strong>
            <span>库存状态、订单状态、同步状态复用统一色板和尺寸。</span>
          </div>
        </template>
        <section class="component-preview-panel" :data-demo-kind="renderOverviewDemo('tags')">
          <div class="demo-row">
            <el-tag>普通商品</el-tag>
            <el-tag type="success">启用</el-tag>
            <el-tag type="warning">待审核</el-tag>
          </div>
        </section>
      </el-collapse-item>

      <el-collapse-item name="overview-permission-buttons" class="component-collapse-panel">
        <template #title>
          <div class="component-panel-title">
            <strong>权限按钮</strong>
            <span>按钮权限、列权限和未来权限提示都从这一层统一约束。</span>
          </div>
        </template>
        <section class="component-preview-panel" :data-demo-kind="renderOverviewDemo('buttons')">
          <div class="demo-row">
            <PermissionButton type="primary" permission="demo:add">新增</PermissionButton>
            <PermissionButton permission="demo:import">导入</PermissionButton>
            <PermissionButton permission="demo:export">导出</PermissionButton>
          </div>
        </section>
      </el-collapse-item>

      <el-collapse-item name="overview-table-toolbar" class="component-collapse-panel">
        <template #title>
          <div class="component-panel-title">
            <strong>表格工具栏</strong>
            <span>列设置、刷新、密度切换、批量操作集中在右上角。</span>
          </div>
        </template>
        <section class="component-preview-panel" :data-demo-kind="renderOverviewDemo('toolbar')">
          <div class="demo-row">
            <el-button size="small">搜索</el-button>
            <el-button size="small">重置</el-button>
            <el-button size="small">列设置</el-button>
          </div>
        </section>
      </el-collapse-item>

      <el-collapse-item name="overview-dialogs" class="component-collapse-panel">
        <template #title>
          <div class="component-panel-title">
            <strong>抽屉与弹窗</strong>
            <span>表单弹窗、详情抽屉、审核确认、批量确认保持同一节奏。</span>
          </div>
        </template>
        <section class="component-preview-panel" :data-demo-kind="renderOverviewDemo('dialogs')">
          <div class="demo-row">
            <el-button type="primary" @click="formVisible = true">表单弹窗</el-button>
            <el-button @click="drawerVisible = true">详情抽屉</el-button>
            <el-button type="success" @click="approvalVisible = true">审核确认</el-button>
            <el-button type="danger" @click="batchVisible = true">批量确认</el-button>
          </div>
        </section>
      </el-collapse-item>

      <el-collapse-item name="overview-theme" class="component-collapse-panel">
        <template #title>
          <div class="component-panel-title">
            <strong>主题与语言</strong>
            <span>直接验证主题色切换和中英文案是否同步影响组件。</span>
          </div>
        </template>
        <section class="component-preview-panel" :data-demo-kind="renderOverviewDemo('theme')">
          <div class="demo-row">
            <ThemeSwitcher />
            <LanguageSwitcher />
          </div>
        </section>
      </el-collapse-item>
    </el-collapse>

    <el-collapse v-model="componentCenterActivePanels" class="component-preview-collapse">
      <el-collapse-item name="search-action-bar" class="component-collapse-panel">
        <template #title>
          <div class="component-panel-title">
            <strong>搜索操作栏</strong>
            <span>列表页顶部查询条件和右侧操作按钮的统一入口。</span>
          </div>
        </template>
        <section class="component-preview-panel">
          <SearchActionBar @reset="resetDemoSearch" @search="submitDemoSearch">
            <el-input v-model="demoSearch.name" class="query-input" placeholder="名称" clearable />
            <el-input v-model="demoSearch.code" class="query-input" placeholder="编码" clearable />
            <el-input v-model="demoSearch.shortName" class="query-input" placeholder="简称" clearable />
            <el-input v-model="demoSearch.barcode" class="query-input" placeholder="条码" clearable />
            <el-select v-model="demoSearch.category" class="query-input" placeholder="分类" clearable>
              <el-option label="分类1" value="分类1" />
              <el-option label="分类2" value="分类2" />
            </el-select>
            <el-select v-model="demoSearch.status" class="query-input" placeholder="全部" clearable>
              <el-option label="全部" value="all" />
              <el-option label="启用" value="enabled" />
              <el-option label="停用" value="disabled" />
            </el-select>

            <template #actions>
              <el-button>导入</el-button>
              <el-button>导入结果</el-button>
              <PermissionButton type="primary" permission="demo:add">新增</PermissionButton>
            </template>
          </SearchActionBar>
        </section>
      </el-collapse-item>

      <el-collapse-item name="login-recent-account-select" class="component-collapse-panel">
        <template #title>
          <div class="component-panel-title">
            <strong>登录最近账号下拉框</strong>
            <span>登录页复用的最近账号选择组件，椭圆输入框自然展开为下拉列表。</span>
          </div>
        </template>
        <section class="component-preview-panel">
          <div class="login-recent-account-demo">
            <RecentAccountSearchSelect
              v-model="selectedLoginRecentAccountKey"
              :profiles="loginRecentAccountDemoProfiles"
              placeholder="选择最近登录账号"
              remove-label="删除当前记录"
              empty-label="未找到匹配账号"
              @select="handleLoginRecentAccountSelect"
              @remove="handleLoginRecentAccountRemove"
            />
            <div class="login-recent-account-demo-state">
              <span>当前选择</span>
              <strong>{{ selectedLoginRecentAccountLabel }}</strong>
            </div>
          </div>
        </section>
      </el-collapse-item>

      <el-collapse-item name="decimal-input" class="component-collapse-panel">
        <template #title>
          <div class="component-panel-title">
            <strong>数字输入框</strong>
            <span>参考 WMS 数字输入：保留输入过程、限制小数位，适合数量、金额和调整值。</span>
          </div>
        </template>
        <section class="component-preview-panel">
          <div class="decimal-input-demo-grid">
            <label class="decimal-input-demo-item">
              <span>数量（4 位小数）</span>
              <XuanDecimalInput v-model="decimalInputDemo.quantity" :scale="4" suffix="件" placeholder="0.0000" />
            </label>
            <label class="decimal-input-demo-item">
              <span>金额（2 位小数）</span>
              <XuanDecimalInput v-model="decimalInputDemo.amount" :scale="2" prefix="¥" placeholder="0.00" />
            </label>
            <label class="decimal-input-demo-item">
              <span>库存调整（允许负数）</span>
              <XuanDecimalInput
                v-model="decimalInputDemo.adjustment"
                :scale="3"
                allow-negative
                suffix="箱"
                placeholder="-0.000"
              />
            </label>
          </div>
        </section>
      </el-collapse-item>

      <el-collapse-item name="date-range-picker" class="component-collapse-panel">
        <template #title>
          <div class="component-panel-title">
            <strong>日期组件</strong>
            <span>参考 WMS 日期范围筛选：开始时间至结束时间，适合列表页时间筛选。</span>
          </div>
        </template>
        <section class="component-preview-panel">
          <div class="date-range-demo-grid">
            <label class="date-range-demo-item">
              <span>日期时间范围</span>
              <XuanDateTimeRangePicker v-model="dateRangeDemo.dateTimeRange" />
            </label>
            <label class="date-range-demo-item">
              <span>仅日期范围</span>
              <XuanDateTimeRangePicker
                v-model="dateRangeDemo.dateRange"
                type="daterange"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
              />
            </label>
            <label class="date-range-demo-item">
              <span>禁用态</span>
              <XuanDateTimeRangePicker v-model="dateRangeDemo.disabledRange" disabled />
            </label>
          </div>
        </section>
      </el-collapse-item>

      <el-collapse-item name="browse-table" class="component-collapse-panel">
        <template #title>
          <div class="component-panel-title">
            <strong>浏览表格</strong>
            <span>列设置、列宽拖动、列顺序和分页大小按用户维度保存。</span>
          </div>
        </template>
        <section class="component-preview-panel">
          <XuanBrowseTable
            v-model:current-page="browseCurrentPage"
            v-model:page-size="browsePageSize"
            v-model:density="browseDensity"
            page-code="component-center"
            table-code="browse-table-demo"
            :user-id="browseUserId"
            :tenant-id="browseTenantId"
            :data="pagedDemoRows"
            :columns="browseColumns"
            :total="browseRows.length"
            :selected-count="browseSelectedRows.length"
            :actions-width="160"
            @selection-change="browseSelectedRows = $event"
          >
            <template #cell-status="{ row }">
              <el-tag size="small" :type="row.status === '启用' ? 'success' : 'warning'">{{ row.status }}</el-tag>
            </template>
            <template #toolbar-actions>
              <el-button text @click="browseSelectedRows = []">刷新</el-button>
            </template>
            <template #actions="{ row }">
              <el-button link type="primary">编辑</el-button>
              <el-button link type="danger">{{ row.status === '启用' ? '停用' : '启用' }}</el-button>
            </template>
          </XuanBrowseTable>
        </section>
      </el-collapse-item>

      <el-collapse-item name="navigation-menu-tree" class="component-collapse-panel">
        <template #title>
          <div class="component-panel-title">
            <strong>导航菜单树</strong>
            <span>菜单管理和权限管理共用的导航分组树，统一展开、选中和计数反馈。</span>
          </div>
        </template>
        <section class="component-preview-panel">
          <div class="navigation-tree-demo">
            <NavigationMenuTree
              v-model="navigationTreeDemoSelectedKey"
              title="导航菜单树"
              all-node-title="全部菜单"
              :menus="permissionDemoMenus"
              :keyword="navigationTreeDemoKeyword"
              :context-menu-enabled="navigationTreeDemoContextMenuEnabled"
              :context-menu-actions-resolver="resolveNavigationTreeDemoContextActions"
              @node-select="navigationTreeDemoSelectedNode = $event"
            />
            <div class="navigation-tree-demo-side">
              <el-input v-model="navigationTreeDemoKeyword" placeholder="搜索菜单" clearable />
              <div class="navigation-tree-demo-current">
                <span>当前节点</span>
                <strong>{{ navigationTreeDemoSelectedNode?.title || '全部菜单' }}</strong>
                <code>{{ navigationTreeDemoSelectedKey }}</code>
              </div>
              <div class="navigation-tree-demo-current">
                <span>最近触发</span>
                <strong>{{ navigationTreeDemoLastContextAction }}</strong>
                <code>{{ navigationTreeDemoContextMenuEnabled ? 'context-menu:on' : 'context-menu:off' }}</code>
              </div>
            </div>
          </div>

          <el-collapse class="component-demo-settings-collapse">
            <el-collapse-item name="navigation-tree-context-menu">
              <template #title>
                <div class="component-panel-title">
                  <strong>右键菜单实现</strong>
                  <span>复制下面的用法到具体页面，菜单项和点击行为都由调用方决定。</span>
                </div>
              </template>
              <div class="navigation-tree-context-settings">
                <label class="navigation-tree-context-switch">
                  <span>预览右键菜单</span>
                  <el-switch v-model="navigationTreeDemoContextMenuEnabled" />
                </label>

                <div class="component-demo-code-blocks">
                  <div class="component-demo-code-block">
                    <span>模板接入</span>
                    <pre><code>{{ navigationTreeDemoTemplateCode }}</code></pre>
                  </div>
                  <div class="component-demo-code-block">
                    <span>菜单项与 onClick</span>
                    <pre><code>{{ navigationTreeDemoScriptCode }}</code></pre>
                  </div>
                </div>
              </div>
            </el-collapse-item>
          </el-collapse>
        </section>
      </el-collapse-item>

      <el-collapse-item name="permission-assignment" class="component-collapse-panel">
        <template #title>
          <div class="component-panel-title">
            <strong>权限分配组件</strong>
            <span>左侧按菜单树定位页面，右侧处理按钮权限，后续扩展列权限和字段设置。</span>
          </div>
        </template>
        <section class="component-preview-panel">
          <MenuPermissionAssignment
            v-model="permissionDemoSelectedCodes"
            :menus="permissionDemoMenus"
            :permissions="permissionDemoPermissions"
            :page-required-permission-map="permissionDemoRequiredPermissionMap"
          />
        </section>
      </el-collapse-item>

      <el-collapse-item name="document-editor" class="component-collapse-panel">
        <template #title>
          <div class="component-panel-title">
            <strong>开单据页面</strong>
            <span>复用单据容器、单据头、明细表格和结算区。</span>
          </div>
        </template>
        <section class="component-preview-panel">
          <DocumentEditorShell
            class="component-document-preview"
            title="新增采购单"
            description="组件管理页预览数据，不触发真实后端提交。"
            status="草稿"
            status-type="warning"
            :summary-items="documentSummaryItems"
            :sticky-summary="false"
          >
            <template #actions>
              <el-button @click="resetDocumentDemo">重置</el-button>
              <el-button type="primary" plain @click="saveDocumentDemo">保存草稿</el-button>
              <el-button type="success" @click="submitDocumentDemo">提交审核</el-button>
            </template>

            <DocumentBasicInfoCard
              v-model="documentBasicInfo"
              :supplier-options="documentSupplierOptions"
              :warehouse-options="documentWarehouseOptions"
              :purchaser-options="documentPurchaserOptions"
              :settlement-account-options="documentAccountOptions"
            />

            <DocumentLineItemsTable
              v-model="documentLineItems"
              :product-options="documentProductOptions"
              :warehouse-options="documentWarehouseOptions"
              :unit-options="documentUnitOptions"
              :default-warehouse="documentBasicInfo.warehouse"
            />

            <DocumentSettlementCard
              v-model="documentSettlementInfo"
              :payable-amount="documentPayableAmount"
              :settlement-method-options="documentSettlementMethodOptions"
              :account-options="documentAccountOptions"
            />

            <template #summaryActions>
              <el-button @click="saveDocumentDemo">保存草稿</el-button>
              <el-button type="success" @click="submitDocumentDemo">提交审核</el-button>
            </template>
          </DocumentEditorShell>
        </section>
      </el-collapse-item>
    </el-collapse>

    <DynamicFormDialog
      v-model="formVisible"
      title="新增供应商"
      description="按业务使用顺序维护基础资料、结算信息和扩展字段"
      helper-text="拖动标题栏移动，拖动四角调整大小"
      :fields="demoFields"
      :sections="demoSections"
      :model="demoForm"
      :custom-fields="demoCustomFields"
      variant="workspace"
      width="96vw"
      label-width="0"
      label-position="top"
      show-custom-fields
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
import { computed, reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus/es/components/message/index';
import LanguageSwitcher from '@/components/app/LanguageSwitcher.vue';
import ThemeSwitcher from '@/components/app/ThemeSwitcher.vue';
import ApprovalConfirmDialog from '@/framework/components/ApprovalConfirmDialog.vue';
import BatchConfirmDialog from '@/framework/components/BatchConfirmDialog.vue';
import DetailDrawer from '@/framework/components/DetailDrawer.vue';
import DocumentBasicInfoCard, {
  type DocumentBasicInfo,
  type DocumentSelectOption,
} from '@/framework/components-erp/DocumentBasicInfoCard.vue';
import DocumentEditorShell from '@/framework/components-erp/DocumentEditorShell.vue';
import DocumentLineItemsTable, {
  type DocumentLineItem,
  type DocumentProductOption,
} from '@/framework/components-erp/DocumentLineItemsTable.vue';
import DocumentSettlementCard, {
  type DocumentSettlementInfo,
} from '@/framework/components-erp/DocumentSettlementCard.vue';
import DynamicFormDialog from '@/framework/components/DynamicFormDialog.vue';
import type { DynamicCustomField, DynamicFormField, DynamicFormSection } from '@/framework/components/DynamicFormDialog.vue';
import MenuPermissionAssignment from '@/framework/components/MenuPermissionAssignment.vue';
import NavigationMenuTree, {
  type NavigationMenuTreeContextMenuAction,
  type NavigationMenuTreeNode,
} from '@/framework/components/NavigationMenuTree.vue';
import PermissionButton from '@/framework/components/PermissionButton.vue';
import RecentAccountSearchSelect from '@/components/login/RecentAccountSearchSelect.vue';
import SearchActionBar from '@/framework/components/SearchActionBar.vue';
import XuanBrowseTable, { type XuanBrowseTableColumn } from '@/framework/components/XuanBrowseTable.vue';
import XuanDateTimeRangePicker, { type DateRangeValue } from '@/framework/components/XuanDateTimeRangePicker.vue';
import XuanDecimalInput from '@/framework/components/XuanDecimalInput.vue';
import type { BrowseTableDensity } from '@/framework/components/browseTablePreferences';
import { useAuthStore } from '@/stores/auth';
import type { IamMenu, IamPermission } from '@/types/iamAdmin';
import type { LoginProfile } from '@/utils/loginProfiles';

defineOptions({ name: 'ComponentCenterView' });

const { t } = useI18n();
const authStore = useAuthStore();

const formVisible = ref(false);
const drawerVisible = ref(false);
const approvalVisible = ref(false);
const batchVisible = ref(false);
const componentOverviewPanels = ref(['overview-query', 'overview-permission-buttons']);
const componentCenterActivePanels = ref([
  'search-action-bar',
  'login-recent-account-select',
  'decimal-input',
  'date-range-picker',
  'browse-table',
  'navigation-menu-tree',
  'permission-assignment',
]);
const browseCurrentPage = ref(1);
const browsePageSize = ref(10);
const browseDensity = ref<BrowseTableDensity>('default');
const browseSelectedRows = ref<BrowseDemoRow[]>([]);
const permissionDemoSelectedCodes = ref([
  'erp-sale-draft:view',
  'erp-sale-draft:edit',
  'erp-sale-draft:add',
  'erp-sale-approved:view',
  'erp-purchase-draft:view',
  'erp-warehouse-stock:view',
]);
const navigationTreeDemoKeyword = ref('');
const navigationTreeDemoSelectedKey = ref('__all__');
const navigationTreeDemoSelectedNode = ref<NavigationMenuTreeNode | null>(null);
const navigationTreeDemoContextMenuEnabled = ref(true);
const navigationTreeDemoLastContextAction = ref('尚未触发');
const navigationTreeDemoContextActions = ref<NavigationTreeDemoContextAction[]>([
  {
    id: 1,
    key: 'edit',
    label: '编辑菜单',
    clickMessage: '编辑',
    enabled: true,
  },
  {
    id: 2,
    key: 'copy',
    label: '复制编码',
    clickMessage: '复制编码',
    enabled: true,
  },
]);
const navigationTreeDemoTemplateCode = `<NavigationMenuTree
  v-model="selectedMenuCode"
  :menus="menus"
  context-menu-enabled
  :context-menu-actions-resolver="resolveMenuContextActions"
  @context-menu-command="handleMenuContextCommand"
/>`;
const navigationTreeDemoScriptCode = `function resolveMenuContextActions(node: NavigationMenuTreeNode) {
  return [
    {
      key: 'edit',
      label: '编辑菜单',
      onClick: ({ node }) => openEditByNode(node),
    },
    {
      key: 'disable',
      label: '停用菜单',
      disabled: node.key === '__all__',
      onClick: ({ node }) => setMenuEnabled(node, false),
    },
  ];
}

function handleMenuContextCommand({ actionKey, node }) {
  console.log('右键菜单已触发', actionKey, node.key);
}`;
const browseTenantId = computed(() => String(authStore.tenantId ?? '0'));
const browseUserId = computed(() => authStore.currentUser?.username || 'anonymous');
const documentBasicInfo = ref<DocumentBasicInfo>(createDefaultDocumentBasicInfo());
const documentLineItems = ref<DocumentLineItem[]>(createDefaultDocumentLineItems());
const documentSettlementInfo = ref<DocumentSettlementInfo>(createDefaultDocumentSettlementInfo());
const demoSearch = reactive({
  name: '',
  code: '',
  shortName: '',
  barcode: '',
  category: '',
  status: '',
});
const decimalInputDemo = reactive({
  quantity: '12.3456',
  amount: '1288.50',
  adjustment: '-2.125',
});
const dateRangeDemo = reactive<{
  dateTimeRange: DateRangeValue;
  dateRange: DateRangeValue;
  disabledRange: DateRangeValue;
}>({
  dateTimeRange: null,
  dateRange: null,
  disabledRange: null,
});
const selectedLoginRecentAccountKey = ref('0::super_admin');
const loginRecentAccountDemoProfiles = ref<LoginProfile[]>([
  {
    key: '0::super_admin',
    tenantId: '0',
    username: 'super_admin',
    password: 'demo-password',
    updatedAt: Date.now(),
  },
  {
    key: '1001::tenant_admin',
    tenantId: '1001',
    username: 'tenant_admin',
    updatedAt: Date.now() - 1000,
  },
  {
    key: '1002::warehouse_user',
    tenantId: '1002',
    username: 'warehouse_user',
    updatedAt: Date.now() - 2000,
  },
]);
const selectedLoginRecentAccountLabel = computed(() => {
  const profile = loginRecentAccountDemoProfiles.value.find((item) => item.key === selectedLoginRecentAccountKey.value);
  return profile ? `${profile.tenantId} / ${profile.username}` : '未选择';
});

type BrowseDemoRow = {
  index: number;
  code: string;
  name: string;
  factoryCode: string;
  factoryModel: string;
  factoryName: string;
  supplier: string;
  status: string;
  category: string;
  unit: string;
};

type NavigationTreeDemoContextAction = {
  id: number;
  key: string;
  label: string;
  clickMessage: string;
  enabled: boolean;
};

const browseColumns: Array<XuanBrowseTableColumn<BrowseDemoRow>> = [
  { key: 'index', title: '序号', width: 70 },
  { key: 'code', title: '编码', width: 130 },
  { key: 'name', title: '名称', width: 130 },
  { key: 'factoryCode', title: '厂家编码', width: 150 },
  { key: 'factoryModel', title: '厂家型号', width: 150 },
  { key: 'factoryName', title: '厂家名称', width: 150 },
  { key: 'supplier', title: '来源供应商', width: 150 },
  { key: 'status', title: '状态', width: 110 },
  { key: 'category', title: '分类', width: 110 },
  { key: 'unit', title: '单位', width: 90 },
];

const browseRows: BrowseDemoRow[] = Array.from({ length: 18 }, (_, index) => {
  const number = index + 1;
  return {
    index: number,
    code: `PROD-${String(number).padStart(4, '0')}`,
    name: `组件商品${number}`,
    factoryCode: number % 2 ? `FC-${number}` : '',
    factoryModel: number % 3 ? `M-${number}` : '',
    factoryName: number % 2 ? '华东制造' : '默认厂家',
    supplier: number % 2 ? '来源供应商' : '-',
    status: number % 4 ? '启用' : '停用',
    category: number % 2 ? '分类1' : '分类2',
    unit: number % 2 ? '件' : '箱',
  };
});

const pagedDemoRows = computed(() => {
  const start = (browseCurrentPage.value - 1) * browsePageSize.value;
  return browseRows.slice(start, start + browsePageSize.value);
});

const permissionDemoMenus: IamMenu[] = [
  { id: 1, code: 'inventory-root', parentId: null, title: '进销存', i18nKey: null, path: null, icon: 'PackageOpen', permissionCode: null, sortNo: 1, enabled: true },
  { id: 2, code: 'base-data', parentId: 1, title: '基础资料', i18nKey: null, path: null, icon: null, permissionCode: null, sortNo: 1, enabled: true },
  { id: 3, code: 'purchase-management', parentId: 1, title: '采购管理', i18nKey: null, path: null, icon: null, permissionCode: null, sortNo: 2, enabled: true },
  { id: 4, code: 'sales-management', parentId: 1, title: '销售管理', i18nKey: null, path: null, icon: null, permissionCode: null, sortNo: 3, enabled: true },
  { id: 5, code: 'warehouse-management', parentId: 1, title: '仓库管理', i18nKey: null, path: null, icon: null, permissionCode: null, sortNo: 4, enabled: true },
  { id: 6, code: 'erp-sale-draft', parentId: 4, title: '销售单（草稿）', i18nKey: null, path: '/sales/orders/draft', icon: null, permissionCode: 'erp-sale-draft:view', sortNo: 1, enabled: true },
  { id: 7, code: 'erp-sale-approved', parentId: 4, title: '销售单（已审核）', i18nKey: null, path: '/sales/orders/approved', icon: null, permissionCode: 'erp-sale-approved:view', sortNo: 2, enabled: true },
  { id: 8, code: 'erp-purchase-draft', parentId: 3, title: '采购单（草稿）', i18nKey: null, path: '/purchase/orders/draft', icon: null, permissionCode: 'erp-purchase-draft:view', sortNo: 1, enabled: true },
  { id: 9, code: 'erp-product', parentId: 2, title: '商品资料', i18nKey: null, path: '/inventory/products', icon: null, permissionCode: 'erp-product:view', sortNo: 1, enabled: true },
  { id: 10, code: 'erp-warehouse-stock', parentId: 5, title: '库存查询', i18nKey: null, path: '/warehouse/stock', icon: null, permissionCode: 'erp-warehouse-stock:view', sortNo: 1, enabled: true },
];

const permissionDemoPermissions: IamPermission[] = [
  { id: 1, code: 'erp-sale-draft:view', name: '销售单草稿-查看', serviceName: 'xuan-sales', menuCode: 'erp-sale-draft', description: null, enabled: true },
  { id: 2, code: 'erp-sale-draft:edit', name: '销售单草稿-编辑', serviceName: 'xuan-sales', menuCode: 'erp-sale-draft', description: null, enabled: true },
  { id: 3, code: 'erp-sale-draft:add', name: '销售单草稿-新增', serviceName: 'xuan-sales', menuCode: 'erp-sale-draft', description: null, enabled: true },
  { id: 4, code: 'erp-sale-draft:approve', name: '销售单草稿-审核', serviceName: 'xuan-sales', menuCode: 'erp-sale-draft', description: null, enabled: true },
  { id: 5, code: 'erp-sale-draft:delete', name: '销售单草稿-删除', serviceName: 'xuan-sales', menuCode: 'erp-sale-draft', description: null, enabled: true },
  { id: 6, code: 'erp-sale-draft:print', name: '销售单草稿-打印', serviceName: 'xuan-sales', menuCode: 'erp-sale-draft', description: null, enabled: true },
  { id: 7, code: 'erp-sale-approved:view', name: '销售单已审核-查看', serviceName: 'xuan-sales', menuCode: 'erp-sale-approved', description: null, enabled: true },
  { id: 8, code: 'erp-sale-approved:print', name: '销售单已审核-打印', serviceName: 'xuan-sales', menuCode: 'erp-sale-approved', description: null, enabled: true },
  { id: 9, code: 'erp-purchase-draft:view', name: '采购单草稿-查看', serviceName: 'xuan-procurement', menuCode: 'erp-purchase-draft', description: null, enabled: true },
  { id: 10, code: 'erp-purchase-draft:edit', name: '采购单草稿-编辑', serviceName: 'xuan-procurement', menuCode: 'erp-purchase-draft', description: null, enabled: true },
  { id: 11, code: 'erp-product:view', name: '商品资料-查看', serviceName: 'xuan-product', menuCode: 'erp-product', description: null, enabled: true },
  { id: 12, code: 'erp-product:edit', name: '商品资料-编辑', serviceName: 'xuan-product', menuCode: 'erp-product', description: null, enabled: true },
  { id: 13, code: 'erp-warehouse-stock:view', name: '库存查询-查看', serviceName: 'xuan-warehouse', menuCode: 'erp-warehouse-stock', description: null, enabled: true },
];

const permissionDemoRequiredPermissionMap: Record<string, string[]> = {
  'erp-sale-draft': ['erp-product:view'],
  'erp-sale-approved': ['erp-product:view'],
  'erp-purchase-draft': ['erp-product:view'],
};

const documentSupplierOptions: DocumentSelectOption[] = [
  { label: '华东配件供应商', value: 'supplier-east' },
  { label: '华南耗材供应商', value: 'supplier-south' },
  { label: '默认供应商', value: 'supplier-default' },
];

const documentWarehouseOptions: DocumentSelectOption[] = [
  { label: '主仓 / A-01', value: 'main-a01' },
  { label: '主仓 / B-02', value: 'main-b02' },
  { label: '质检仓 / Q-01', value: 'qc-q01' },
];

const documentPurchaserOptions: DocumentSelectOption[] = [
  { label: '张三', value: 'zhangsan' },
  { label: '李四', value: 'lisi' },
  { label: '王五', value: 'wangwu' },
];

const documentAccountOptions: DocumentSelectOption[] = [
  { label: '应付挂账', value: 'payable' },
  { label: '建设银行 8899', value: 'ccb-8899' },
  { label: '支付宝采购账户', value: 'alipay-purchase' },
];

const documentSettlementMethodOptions: DocumentSelectOption[] = [
  { label: '挂账', value: 'credit' },
  { label: '现结', value: 'cash' },
  { label: '月结', value: 'monthly' },
];

const documentUnitOptions: DocumentSelectOption[] = [
  { label: '件', value: '件' },
  { label: '箱', value: '箱' },
  { label: '套', value: '套' },
];

const documentProductOptions: DocumentProductOption[] = [
  { label: '机油滤芯 A100', value: 'prod-a100', sku: 'A100', spec: '适配 1.6L 发动机', unit: '件', price: 48 },
  { label: '刹车片 B210', value: 'prod-b210', sku: 'B210', spec: '前轮陶瓷片', unit: '套', price: 186 },
  { label: '空气滤芯 C330', value: 'prod-c330', sku: 'C330', spec: '标准型', unit: '件', price: 38 },
];

const documentCommodityAmount = computed(() => documentLineItems.value.reduce((total, line) => total + line.quantity * line.price, 0));
const documentLineDiscountAmount = computed(() => documentLineItems.value.reduce((total, line) => total + line.discount, 0));
const documentTaxAmount = computed(() => documentLineItems.value.reduce((total, line) => {
  const taxableAmount = Math.max(line.quantity * line.price - line.discount, 0);
  return total + taxableAmount * (line.taxRate / 100);
}, 0));
const documentPayableAmount = computed(() => Math.max(
  documentCommodityAmount.value - documentLineDiscountAmount.value - documentSettlementInfo.value.discountAmount + documentTaxAmount.value,
  0,
));
const documentArrearsAmount = computed(() => Math.max(documentPayableAmount.value - documentSettlementInfo.value.paidAmount, 0));

const documentSummaryItems = computed(() => [
  { label: '商品合计', value: formatCurrency(documentCommodityAmount.value) },
  { label: '明细优惠', value: formatCurrency(documentLineDiscountAmount.value) },
  { label: '税额', value: formatCurrency(documentTaxAmount.value) },
  { label: '整单优惠', value: formatCurrency(documentSettlementInfo.value.discountAmount) },
  { label: '应付金额', value: formatCurrency(documentPayableAmount.value), strong: true },
  { label: '未付金额', value: formatCurrency(documentArrearsAmount.value), strong: true },
]);

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

const demoSections: DynamicFormSection[] = [
  {
    title: '供应商资料',
    fields: [
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
    ],
  },
  {
    title: '客户类别售价',
    fields: [
      { key: 'vip2Price', label: 'vip2', placeholder: '' },
      { key: 'retailPrice', label: '零售客户', placeholder: '' },
      { key: 'wholesalePrice', label: '批发客户', placeholder: '' },
      { key: 'vipPrice', label: 'VIP客户', placeholder: '' },
    ],
  },
];

const demoCustomFields = ref<DynamicCustomField[]>([]);

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

function createDefaultDocumentBasicInfo(): DocumentBasicInfo {
  return {
    documentNo: 'PO202607100002',
    documentTime: formatCurrentDateTime(),
    supplier: 'supplier-east',
    warehouse: 'main-a01',
    purchaser: 'zhangsan',
    settlementAccount: 'payable',
    creator: authStore.username || 'admin',
    lastModifier: authStore.username || 'admin',
    expectedArrivalDate: '2026-07-15',
    externalNo: 'HT-20260710-01',
    remark: '组件管理页示例单据',
  };
}

function formatCurrentDateTime() {
  const now = new Date();
  return `${now.getFullYear()}-${padDatePart(now.getMonth() + 1)}-${padDatePart(now.getDate())} ${padDatePart(now.getHours())}:${padDatePart(now.getMinutes())}:${padDatePart(now.getSeconds())}`;
}

function padDatePart(value: number) {
  return String(value).padStart(2, '0');
}

function createDefaultDocumentLineItems(): DocumentLineItem[] {
  return [
    {
      id: 1,
      productCode: 'prod-a100',
      productName: '机油滤芯 A100',
      productSpec: '适配 1.6L 发动机',
      warehouse: 'main-a01',
      unit: '件',
      quantity: 10,
      price: 48,
      discount: 20,
      taxRate: 13,
      remark: '常用备件',
    },
    {
      id: 2,
      productCode: 'prod-b210',
      productName: '刹车片 B210',
      productSpec: '前轮陶瓷片',
      warehouse: 'main-b02',
      unit: '套',
      quantity: 4,
      price: 186,
      discount: 0,
      taxRate: 13,
      remark: '',
    },
  ];
}

function createDefaultDocumentSettlementInfo(): DocumentSettlementInfo {
  return {
    settlementMethod: 'credit',
    paymentAccount: 'payable',
    discountAmount: 30,
    paidAmount: 300,
    invoiceType: 'normal',
    contact: '王经理',
    contactPhone: '13800000000',
    deliveryAddress: '上海市浦东新区默认收货地址',
  };
}

function formatCurrency(value: number) {
  return `¥ ${value.toFixed(2)}`;
}

function renderOverviewDemo(kind: string) {
  return kind;
}

function submitDemoForm(value: Record<string, unknown>) {
  Object.assign(demoForm, value);
  demoCustomFields.value = Array.isArray(value.customFields) ? value.customFields as DynamicCustomField[] : [];
  formVisible.value = false;
  ElMessage.success('表单弹窗提交成功');
}

function resetDemoSearch() {
  Object.assign(demoSearch, {
    name: '',
    code: '',
    shortName: '',
    barcode: '',
    category: '',
    status: '',
  });
  ElMessage.success('搜索条件已重置');
}

function submitDemoSearch() {
  ElMessage.success('搜索操作栏已触发查询');
}

function handleLoginRecentAccountSelect(profileKey: string) {
  const profile = loginRecentAccountDemoProfiles.value.find((item) => item.key === profileKey);
  if (profile) {
    ElMessage.success(`已选择 ${profile.tenantId} / ${profile.username}`);
  }
}

function handleLoginRecentAccountRemove(profileKey: string) {
  loginRecentAccountDemoProfiles.value = loginRecentAccountDemoProfiles.value.filter((item) => item.key !== profileKey);
  if (selectedLoginRecentAccountKey.value === profileKey) {
    selectedLoginRecentAccountKey.value = loginRecentAccountDemoProfiles.value[0]?.key || '';
  }
  ElMessage.success('演示账号已移除');
}

function resolveNavigationTreeDemoContextActions(node: NavigationMenuTreeNode): NavigationMenuTreeContextMenuAction[] {
  return navigationTreeDemoContextActions.value
    .filter((action) => action.enabled && action.key.trim() && action.label.trim())
    .map((action) => ({
      key: action.key.trim(),
      label: action.label.trim(),
      onClick: () => handleNavigationTreeDemoContextAction(action, node),
    }));
}

function handleNavigationTreeDemoContextAction(action: NavigationTreeDemoContextAction, node: NavigationMenuTreeNode) {
  const message = `${action.clickMessage || action.label}: ${node.title}`;
  navigationTreeDemoLastContextAction.value = message;
  ElMessage.success(message);
}

function confirmApproval() {
  approvalVisible.value = false;
  ElMessage.success('审核确认已提交');
}

function confirmBatch() {
  batchVisible.value = false;
  ElMessage.success('批量操作已确认');
}

function resetDocumentDemo() {
  documentBasicInfo.value = createDefaultDocumentBasicInfo();
  documentLineItems.value = createDefaultDocumentLineItems();
  documentSettlementInfo.value = createDefaultDocumentSettlementInfo();
  ElMessage.success('开单据预览已重置');
}

function saveDocumentDemo() {
  ElMessage.success('开单据预览草稿已保存');
}

function submitDocumentDemo() {
  ElMessage.success('开单据预览已提交审核');
}
</script>

<style scoped>
.component-preview-collapse {
  display: flex;
  flex-direction: column;
  gap: 12px;
  border: 0;
}

.component-overview-collapse {
  margin-bottom: 12px;
}

.component-preview-collapse :deep(.el-collapse-item) {
  overflow: hidden;
  border: 1px solid var(--xuan-border);
  border-radius: 8px;
  background: var(--xuan-panel);
}

.component-preview-collapse :deep(.el-collapse-item__header) {
  height: auto;
  min-height: 58px;
  border-bottom: 1px solid transparent;
  padding: 0 16px;
  background: var(--xuan-panel);
}

.component-preview-collapse :deep(.el-collapse-item__header.is-active) {
  border-bottom-color: var(--xuan-border);
}

.component-preview-collapse :deep(.el-collapse-item__wrap) {
  border-bottom: 0;
  background: var(--xuan-panel);
}

.component-preview-collapse :deep(.el-collapse-item__content) {
  padding: 0;
}

.component-panel-title {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
  line-height: 1.4;
}

.component-panel-title strong {
  color: var(--xuan-text);
  font-size: 15px;
}

.component-panel-title span {
  overflow: hidden;
  color: var(--xuan-muted);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.component-collapse-panel .component-preview-panel {
  margin: 0;
  border: 0;
  border-radius: 0;
  box-shadow: none;
}

.navigation-tree-demo {
  display: grid;
  grid-template-columns: minmax(260px, 320px) minmax(220px, 1fr);
  gap: 16px;
  min-height: 360px;
}

.navigation-tree-demo-side {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 12px;
}

.navigation-tree-demo-current {
  border: 1px solid var(--xuan-border);
  border-radius: 8px;
  display: grid;
  gap: 8px;
  padding: 14px;
  background: var(--xuan-panel);
}

.navigation-tree-demo-current span {
  color: var(--xuan-muted);
  font-size: 12px;
}

.navigation-tree-demo-current strong {
  color: var(--xuan-text);
  font-size: 15px;
}

.navigation-tree-demo-current code {
  width: fit-content;
  border-radius: 4px;
  padding: 2px 6px;
  color: #0f766e;
  background: #ecfdf5;
  font-size: 12px;
}

.component-demo-settings-collapse {
  margin-top: 14px;
  border: 1px solid var(--xuan-border);
  border-radius: 8px;
  overflow: hidden;
}

.component-demo-settings-collapse :deep(.el-collapse-item__header) {
  min-height: 52px;
}

.component-demo-settings-collapse :deep(.el-collapse-item__content) {
  padding: 14px;
}

.navigation-tree-context-settings {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.navigation-tree-context-switch {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--xuan-text);
  font-size: 13px;
}

.component-demo-code-blocks {
  display: grid;
  grid-template-columns: repeat(2, minmax(260px, 1fr));
  gap: 12px;
}

.component-demo-code-block {
  min-width: 0;
}

.component-demo-code-block span {
  display: block;
  margin-bottom: 8px;
  color: var(--xuan-muted);
  font-size: 12px;
}

.component-demo-code-block pre {
  min-height: 0;
  margin: 0;
  border: 1px solid var(--xuan-border);
  border-radius: 8px;
  padding: 12px;
  color: #334155;
  background: #f8fafc;
  font-size: 12px;
  line-height: 1.6;
  overflow: auto;
}

.component-demo-code-block code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, "Liberation Mono", monospace;
  white-space: pre;
}

.decimal-input-demo-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(180px, 1fr));
  gap: 16px;
}

.date-range-demo-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(260px, 1fr));
  gap: 16px;
}

.decimal-input-demo-item {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 8px;
  color: var(--xuan-text);
  font-size: 13px;
}

.date-range-demo-item {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 8px;
  color: var(--xuan-text);
  font-size: 13px;
}

.decimal-input-demo-item span,
.date-range-demo-item span {
  color: var(--xuan-muted);
}

.login-recent-account-demo {
  display: grid;
  max-width: 440px;
  gap: 12px;
}

.login-recent-account-demo-state {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 24px;
  color: var(--xuan-muted);
  font-size: 13px;
}

.login-recent-account-demo-state strong {
  color: var(--xuan-text);
  font-size: 14px;
}

@media (max-width: 900px) {
  .navigation-tree-demo,
  .component-demo-code-blocks,
  .decimal-input-demo-grid,
  .date-range-demo-grid {
    grid-template-columns: 1fr;
  }
}
</style>
