import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const currentDir = dirname(fileURLToPath(import.meta.url));
const frontendRoot = resolve(currentDir, '..');
const roleViewSource = readFileSync(resolve(frontendRoot, 'src/views/IamRoleManagementView.vue'), 'utf8');
const menuComponentSource = readFileSync(resolve(frontendRoot, 'src/framework/components/MenuPermissionAssignment.vue'), 'utf8');

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

assert(
  roleViewSource.includes('grantReadonly'),
  '角色授权页应显式维护授权抽屉的只读状态。',
);

assert(
  roleViewSource.includes(':readonly="grantReadonly"'),
  '角色授权页应将只读状态传给权限树组件。',
);

assert(
  roleViewSource.includes('confirm-text="保存授权"'),
  '角色授权页保存授权按钮应使用共享弹窗壳的保存授权文案。',
);

assert(
  roleViewSource.includes('DynamicFormDialog'),
  '角色新增/编辑弹窗应使用组件中心 DynamicFormDialog。',
);

assert(
  roleViewSource.includes('size="lg"'),
  '角色授权弹窗应使用组件管理表单弹窗的 lg 尺寸。',
);

assert(
  roleViewSource.includes(':confirm-permission="roleDialogConfirmPermission"'),
  '角色新增/编辑弹窗保存按钮必须绑定当前动作权限。',
);

assert(
  roleViewSource.includes('variant="workspace"') && roleViewSource.includes('workspace-size="lg"'),
  '角色授权弹窗应使用组件中心 workspace/lg 弹窗形态。',
);

assert(
  roleViewSource.includes('helper-text="拖动标题栏移动，拖动右下角调整大小"'),
  '角色授权弹窗应沿用组件中心 workspace 弹窗的拖动和缩放提示。',
);

assert(
  roleViewSource.includes(':render-form="false"'),
  '角色授权弹窗应使用组件中心 DynamicFormDialog 的无表单模式。',
);

assert(
  !roleViewSource.includes('<el-dialog v-model="grantVisible"'),
  '角色授权弹窗不能直接使用原生 el-dialog。',
);

assert(
  !roleViewSource.includes('<el-input-number'),
  '角色授权页不能直接使用原生 el-input-number。',
);

assert(
  !roleViewSource.includes("key: 'tenantId'"),
  '角色新增/编辑表单不能暴露租户 ID 字段，租户归属必须来自当前页面上下文。',
);

assert(
  !roleViewSource.includes('Object.assign(form, {\n    tenantId'),
  '角色新增/编辑表单模型不能夹带 tenantId。',
);

assert(
  roleViewSource.includes(':confirm-disabled-reason="grantReadonly ? \'平台级角色当前仅支持查看\' : \'\'"'),
  '角色授权页应在只读状态禁用保存授权按钮。',
);

assert(
  roleViewSource.includes('grantReadonly.value = row.tenantId <= 0'),
  '平台级角色应被标记为授权只读。',
);

assert(
  roleViewSource.includes('if (grantReadonly.value)'),
  '只读授权抽屉不应继续发起保存请求。',
);

assert(
  roleViewSource.includes('平台级角色当前仅支持查看权限分配结果'),
  '角色授权页应向用户说明平台级角色当前是只读查看。',
);

assert(
  roleViewSource.includes('availablePermissionCodes'),
  '角色授权页应读取后端返回的租户权限池 availablePermissionCodes。',
);

assert(
  roleViewSource.includes('availablePermissions'),
  '角色授权页应读取后端返回的租户可授权权限元数据 availablePermissions。',
);

assert(
  roleViewSource.includes('permissions.value = grant.availablePermissions || []'),
  '角色授权页应使用角色授权接口返回的 availablePermissions 回填授权树权限元数据。',
);

assert(
  !roleViewSource.includes('listIamPermissions'),
  '角色授权页不能调用权限目录接口 listIamPermissions，否则租户授权会被迫依赖 iam-permission:view。',
);

assert(
  !roleViewSource.includes('listIamMenus'),
  '角色授权页不能调用菜单目录接口 listIamMenus，否则租户授权会被迫依赖 iam-menu:view。',
);

assert(
  roleViewSource.includes('grantAvailablePermissionCodes'),
  '角色授权页应维护当前角色可选择的权限池。',
);

assert(
  roleViewSource.includes(':permissions="grantAvailablePermissions"'),
  '角色授权树只能展示租户权限池内的可授权权限。',
);

assert(
  roleViewSource.includes('selectedPermissionCodes.value = selectedPermissionCodes.value.filter((code) => grantAvailablePermissionCodes.value.includes(code))'),
  '角色授权页应在前端裁剪超出租户权限池的已选权限。',
);

assert(
  menuComponentSource.includes('readonly?: boolean'),
  '权限树组件应声明 readonly 属性。',
);

assert(
  menuComponentSource.includes(':disabled="props.readonly"'),
  '权限树组件应在只读模式下禁用复选框。',
);

assert(
  menuComponentSource.includes("v-if=\"!props.readonly\""),
  '权限树组件在只读模式下不应显示批量编辑操作。',
);

assert(
  menuComponentSource.includes('if (props.readonly)'),
  '权限树组件在只读模式下应阻止树节点批量切换。',
);

console.log('Verified platform role grant guard contract.');
