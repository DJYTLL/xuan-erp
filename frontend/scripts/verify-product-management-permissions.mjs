import fs from 'node:fs';
import path from 'node:path';
import { createServer } from 'vite';
import { createSSRApp, h, provide } from 'vue';
import { renderToString } from 'vue/server-renderer';

const root = process.cwd();

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), 'utf8');
}

function assertIncludes(source, text, message) {
  if (!source.includes(text)) {
    throw new Error(message);
  }
}

function assertNotIncludes(source, text, message) {
  if (source.includes(text)) {
    throw new Error(message);
  }
}

const packageSource = read('package.json');
const requiredPermissionSource = read('src/config/businessPageRequiredPermissions.ts');
const viewSource = read('src/views/ProductManagementView.vue');
const businessModuleViewSource = read('src/views/BusinessModuleView.vue');

assertIncludes(
  packageSource,
  'verify:product-management-permissions',
  'package.json should expose product management permission verification.',
);

[
  'PRODUCT_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS',
  "'product:view'",
  "'product:create'",
  "'product:update'",
  "'product:delete'",
  "'product:import'",
  "'product:export'",
  "'product-management'",
].forEach((text) => assertIncludes(
  requiredPermissionSource,
  text,
  `Product required permissions should include ${text}.`,
));

[
  'v-if="canUseProductPermission(\'product:import\')"',
  'v-if="canUseProductPermission(\'product:create\')"',
  'v-if="canUseProductPermission(\'product:export\')"',
  'v-if="canUseProductPermission(\'product:delete\')"',
  'v-if="canUseProductPermission(\'product:view\')"',
  'v-if="canUseProductPermission(\'product:update\')"',
  'canUseProductPermission',
  'type="primary" permission="product:create"',
  'permission="product:import"',
  'text permission="product:export"',
  'link type="primary" permission="product:view"',
  'link type="primary" permission="product:update"',
  'link type="danger" permission="product:delete"',
  'permission="product:delete"',
  ':disabled-reason="selectedRows.length ? \'\' : \'请先选择商品\'"',
  'requireProductButtonPermission',
  "requireProductButtonPermission('product:update')",
  "requireProductButtonPermission('product:delete')",
  "requireProductButtonPermission('product:export')",
].forEach((text) => assertIncludes(
  viewSource,
  text,
  `Product page should bind business button permission with ${text}.`,
));

[
  '<el-button>导入</el-button>',
  '<el-button>导入结果</el-button>',
  '<el-button text @click="exportRows">导出</el-button>',
  '<el-button text type="danger" :disabled="!selectedRows.length" @click="openDeleteProducts(selectedRows)">批量删除</el-button>',
  '<el-button link type="primary" @click="openProductDetail(row)">查看</el-button>',
  '<el-button link type="primary" @click="openEditProduct(row)">编辑</el-button>',
  '<el-button link type="danger" @click="openDeleteProducts([row])">删除</el-button>',
  'no-permission-mode="disable"',
].forEach((text) => assertNotIncludes(
  viewSource,
  text,
  `Product page should not keep unpermissioned business button: ${text}.`,
));

[
  "type ModuleAction = 'view' | 'create' | 'update' | 'delete' | 'audit' | 'import' | 'export'",
  "permissionFor('import')",
  "permissionFor('export')",
  "permissionFor('create')",
  "permissionFor('audit')",
  "permissionFor('delete')",
  "permissionFor('view')",
  "permissionFor('update')",
  "v-if=\"canUseAction('create')\"",
  "v-if=\"canUseAction('update')\"",
  "v-if=\"canUseAction('delete')\"",
  "v-if=\"canUseAction('import')\"",
  "v-if=\"canUseAction('export')\"",
  "v-if=\"canUseAction('audit')\"",
].forEach((text) => assertIncludes(
  businessModuleViewSource,
  text,
  `Business module CRUD/action buttons should bind permission with ${text}.`,
));

[
  '<el-button @click="openModuleAction(\'导入\')">导入</el-button>',
  '<el-button @click="openModuleAction(\'导出\')">导出</el-button>',
  '<el-button type="primary" @click="openModuleAction(\'新增\')">新增</el-button>',
  '<el-button link type="primary" @click="openRowAction(\'查看\', row)">查看</el-button>',
  '<el-button link type="primary" @click="openRowAction(\'编辑\', row)">编辑</el-button>',
  '<el-button link type="danger" @click="openRowAction(\'删除\', row)">删除</el-button>',
].forEach((text) => assertNotIncludes(
  businessModuleViewSource,
  text,
  `Business module should not keep unpermissioned action button: ${text}.`,
));

const server = await createServer({
  root,
  logLevel: 'silent',
  server: { middlewareMode: true },
});

try {
  const [{ default: PermissionButton }, { frameworkPermissionCheckerKey }] = await Promise.all([
    server.ssrLoadModule('/src/framework/components/PermissionButton.vue'),
    server.ssrLoadModule('/src/framework/auth/permissionChecker.ts'),
  ]);
  const allowedPermissions = new Set(['product:view']);
  const app = createSSRApp({
    setup() {
      provide(frameworkPermissionCheckerKey, (permission) => {
        if (!permission) {
          return true;
        }
        const permissions = Array.isArray(permission) ? permission : [permission];
        return permissions.every((item) => allowedPermissions.has(item));
      });
      return () => h('div', [
        h(PermissionButton, { permission: 'product:view' }, { default: () => '查看' }),
        h(PermissionButton, { permission: 'product:create' }, { default: () => '新增' }),
        h(PermissionButton, { permission: 'product:update' }, { default: () => '编辑' }),
        h(PermissionButton, { permission: 'product:delete' }, { default: () => '删除' }),
      ]);
    },
  });
  app.component('el-tooltip', {
    setup(_, { slots }) {
      return () => h('span', slots.default?.());
    },
  });
  app.component('el-button', {
    setup(_, { slots }) {
      return () => h('button', slots.default?.());
    },
  });
  const html = await renderToString(app);
  assertIncludes(html, '查看', 'Product view button should render when product:view is granted.');
  ['新增', '编辑', '删除'].forEach((text) => assertNotIncludes(
    html,
    text,
    `Product ${text} button should be hidden when permission is missing.`,
  ));

  const appWithoutPermissionProvider = createSSRApp({
    setup() {
      return () => h('div', [
        h(PermissionButton, { permission: 'product:create' }, { default: () => '新增' }),
      ]);
    },
  });
  appWithoutPermissionProvider.component('el-tooltip', {
    setup(_, { slots }) {
      return () => h('span', slots.default?.());
    },
  });
  appWithoutPermissionProvider.component('el-button', {
    setup(_, { slots }) {
      return () => h('button', slots.default?.());
    },
  });
  const htmlWithoutProvider = await renderToString(appWithoutPermissionProvider);
  assertNotIncludes(
    htmlWithoutProvider,
    '新增',
    'PermissionButton should fail closed when no permission provider is installed.',
  );
} finally {
  await server.close();
}

console.log('Product management button permission contract verified.');
