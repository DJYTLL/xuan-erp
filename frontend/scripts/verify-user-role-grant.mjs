import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const currentDir = dirname(fileURLToPath(import.meta.url));
const frontendRoot = resolve(currentDir, '..');

function read(relativePath) {
  return readFileSync(resolve(frontendRoot, relativePath), 'utf8');
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

const packageSource = read('package.json');
const apiSource = read('src/api/iamAdmin.ts');
const viewSource = read('src/views/IamUserManagementView.vue');
const userTableSchemaSource = read('src/config/iamUserBrowseTableSchema.ts');
const routerSource = read('src/router/index.ts');
const navigationSource = read('src/config/navigation.ts');
const columnPermissionPagesSource = read('src/config/columnPermissionPages.ts');
const messagesSource = read('src/i18n/messages.ts');

assert(
  packageSource.includes('verify:user-role-grant'),
  'package.json 应暴露用户分配角色闭环校验脚本。',
);

assert(apiSource.includes('listIamUsers'), 'IAM 管理 API 应提供 listIamUsers。');
assert(apiSource.includes('createIamUser'), 'IAM 管理 API 应提供 createIamUser。');
assert(apiSource.includes('updateIamUser'), 'IAM 管理 API 应提供 updateIamUser。');
assert(apiSource.includes('resetIamUserPassword'), 'IAM 管理 API 应提供 resetIamUserPassword。');
assert(apiSource.includes('getIamUserRoles'), 'IAM 管理 API 应提供 getIamUserRoles。');
assert(apiSource.includes('setIamUserRoles'), 'IAM 管理 API 应提供 setIamUserRoles。');
assert(apiSource.includes('/api/iam/users'), 'IAM 管理 API 应请求 /api/iam/users。');
assert(apiSource.includes('/roles'), 'IAM 用户角色 API 应请求用户 roles 子资源。');
assert(apiSource.includes('/reset-password'), 'IAM 用户重置密码 API 应请求 reset-password 子资源。');

assert(viewSource.includes('listIamUsers'), '用户管理页应加载租户用户列表。');
assert(viewSource.includes('createIamUser'), '用户管理页应支持新增用户。');
assert(viewSource.includes('updateIamUser'), '用户管理页应支持编辑用户资料。');
assert(viewSource.includes('resetIamUserPassword'), '用户管理页应支持重置用户密码。');
assert(viewSource.includes('getIamUserRoles'), '用户管理页应查询当前用户角色。');
assert(viewSource.includes('setIamUserRoles'), '用户管理页应保存用户角色。');
assert(viewSource.includes('permission="iam-user:create"'), '新增用户按钮必须绑定 iam-user:create。');
assert(
  viewSource.includes('permission="iam-user:update"') || viewSource.includes("'iam-user:update'") || userTableSchemaSource.includes("'iam-user:update'"),
  '编辑用户资料和分配角色按钮必须绑定 iam-user:update。',
);
assert(
  viewSource.includes('permission="iam-user:reset-password"') || viewSource.includes("'iam-user:reset-password'") || userTableSchemaSource.includes("'iam-user:reset-password'"),
  '重置密码按钮必须绑定 iam-user:reset-password。',
);
assert(viewSource.includes('initialPassword'), '新增用户弹窗应使用初始密码字段。');
assert(viewSource.includes('newPassword'), '重置密码弹窗应使用新密码字段。');
assert(viewSource.includes('DynamicFormDialog'), '用户管理页新增、编辑、重置密码应使用组件中心 DynamicFormDialog。');
assert(viewSource.includes('XuanBrowseTable'), '用户管理页列表必须使用组件中心 XuanBrowseTable。');
assert(!viewSource.includes('<el-table'), '用户管理页列表不能继续直接使用原生 el-table。');
assert(viewSource.includes('iamUserBrowseTableSchema'), '用户管理页必须使用 iamUserBrowseTableSchema 描述表格列和操作。');
assert(viewSource.includes(':column-permission-snapshot="iamUserColumnPermissionSnapshot"'), '用户管理表格必须接入列权限快照。');
assert(viewSource.includes(':strict-column-permission-snapshot="strictColumnPermissionSnapshot"'), '用户管理表格必须严格消费列权限快照，缺失字段不能默认展示。');
assert(viewSource.includes('authorizationStore.columnPermissions'), '用户管理页列权限快照必须来自 authorizationStore.columnPermissions。');
assert(viewSource.includes('handleUserTableRowAction'), '用户管理页表格行操作必须统一分发。');
assert(viewSource.includes("case 'edit'"), '用户管理页表格行操作必须支持编辑。');
assert(viewSource.includes("case 'role-grant'"), '用户管理页表格行操作必须支持分配角色。');
assert(viewSource.includes("case 'reset-password'"), '用户管理页表格行操作必须支持重置密码。');
assert(viewSource.includes(':confirm-permission="userDialogConfirmPermission"'), '用户新增/编辑弹窗保存按钮必须绑定当前动作权限。');
assert(viewSource.includes('confirm-permission="iam-user:reset-password"'), '用户重置密码弹窗确认按钮必须绑定 iam-user:reset-password。');
assert(viewSource.includes('size="lg"'), '用户新增/编辑弹窗应使用组件管理表单弹窗的 lg 尺寸。');
assert(viewSource.includes('size="sm"'), '用户重置密码弹窗应使用组件管理表单弹窗的 sm 尺寸。');
assert(viewSource.includes(':render-form="false"'), '用户角色分配弹窗应使用组件中心 DynamicFormDialog 的无表单模式。');
assert(
  viewSource.includes('variant="workspace"') && viewSource.includes('workspace-size="lg"'),
  '用户角色分配弹窗应使用组件管理表单弹窗的 workspace 三档外框样式。',
);
assert(viewSource.includes('helper-text="拖动标题栏移动，拖动右下角调整大小"'), '用户角色分配弹窗应保持组件管理表单弹窗的可拖动/可缩放提示。');
assert(!viewSource.includes('<el-dialog v-model="grantVisible"'), '用户角色分配弹窗不能直接使用原生 el-dialog。');
assert(!viewSource.includes('<el-input-number'), '用户管理页不能直接使用原生 el-input-number。');
assert(!viewSource.includes("key: 'tenantId'"), '用户新增/编辑表单不能暴露租户 ID 字段，租户归属必须来自当前页面上下文。');
assert(!viewSource.includes('Object.assign(userForm, {\n    tenantId'), '用户新增/编辑表单模型不能夹带 tenantId。');
assert(
  viewSource.includes('isTenantReady') && viewSource.includes('tenantId.value >= 0'),
  '用户管理页应支持平台态 tenantId=0，并在租户 ID 非法时再阻止查询用户和角色。',
);
assert(
  viewSource.includes('请输入有效租户 ID'),
  '用户管理页应在租户 ID 无效时给出明确提示。',
);
assert(
  viewSource.includes('grantReadonly') && viewSource.includes('user.tenantId <= 0'),
  '用户管理页应把平台用户角色分配视为只读查看。',
);
assert(
  viewSource.includes('平台级用户当前仅支持查看角色分配结果'),
  '用户管理页应提示平台级用户角色当前是只读查看。',
);
assert(viewSource.includes('useAuthorizationStore'), '用户管理页应接入当前权限快照 store。');
assert(viewSource.includes('strictColumnPermissionSnapshot'), '用户管理页应只在租户上下文且权限快照已加载后启用严格列权限。');
assert(
  !viewSource.includes('Object.keys(iamUserRawColumnPermissions.value).length > 0'),
  '用户管理页不能因为 iam-user 快照缺字段就默认放行，否则真实页会比角色列权限预览多显示列。',
);
assert(
  viewSource.includes('authorizationStore.isLoaded') && viewSource.includes('Number(authStore.tenantId || 0) > 0'),
  '用户管理页应在租户上下文且当前权限快照已加载后严格消费列权限，缺失字段按隐藏处理。',
);
assert(
  viewSource.includes('authorizationStore.refreshCurrentAuthorizationContext'),
  '给当前登录用户保存角色后应重新拉取 /api/iam/permissions/current 和 /api/iam/menus/current。',
);
assert(
  viewSource.includes('authStore.currentUser?.userId'),
  '用户管理页应识别保存对象是否为当前登录用户。',
);

assert(userTableSchemaSource.includes('createIamUserBrowseTableSchema'), '用户管理表格 schema 应提供 createIamUserBrowseTableSchema。');
assert(userTableSchemaSource.includes("resourceKey: 'iam-user'"), '用户管理表格字段必须绑定 iam-user 列权限资源。');
assert(userTableSchemaSource.includes("columnKey: 'phone'") && userTableSchemaSource.includes("maskType: 'phone'"), '用户管理手机号列必须支持电话脱敏。');
assert(
  viewSource.includes("key: 'role-grant'") || userTableSchemaSource.includes("key: 'role-grant'"),
  '用户管理表格 schema 必须包含分配角色行操作。',
);
assert(
  viewSource.includes("key: 'reset-password'") || userTableSchemaSource.includes("key: 'reset-password'"),
  '用户管理表格 schema 必须包含重置密码行操作。',
);

assert(
  columnPermissionPagesSource.includes("menuCode: 'iam-user-management'") && columnPermissionPagesSource.includes("resourceKeys: ['iam-user']"),
  '列权限页面配置必须把用户管理菜单映射到 iam-user 字段资源。',
);

assert(
  routerSource.includes('IamUserManagementView') && routerSource.includes('system/iam/users'),
  '前端路由应接入 /system/iam/users 用户管理页。',
);
assert(
  navigationSource.includes('iam-user-management') && navigationSource.includes('/system/iam/users'),
  '导航配置应接入 iam-user-management 用户管理菜单。',
);
assert(
  messagesSource.includes('iamUsers') && messagesSource.includes('用户管理') && messagesSource.includes('User Management'),
  '中英文国际化应包含用户管理 route/menu 文案。',
);

console.log('Verified user role grant closes user -> role -> permissions snapshot -> frontend refresh loop.');
