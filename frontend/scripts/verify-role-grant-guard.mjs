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
  roleViewSource.includes('selectedPermissionCodes.value = allEnabledPermissionCodes.value'),
  '平台级角色打开授权抽屉时应回填全部启用权限用于查看。',
);

assert(
  roleViewSource.includes(':readonly="grantReadonly"'),
  '角色授权页应将只读状态传给权限树组件。',
);

assert(
  roleViewSource.includes(':disabled="grantReadonly"'),
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
  roleViewSource.includes('const allEnabledPermissionCodes = computed(() =>'),
  '角色授权页应维护全部启用权限列表，供平台级角色查看。',
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
