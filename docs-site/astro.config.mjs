import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';

export default defineConfig({
  site: 'http://localhost:3000',
  integrations: [
    starlight({
      title: 'Xuan ERP 微服务架构文档',
      description: '记录 Xuan ERP 微服务化重构的架构、权限、租户、服务边界和运维方案。',
      defaultLocale: 'zh-CN',
      locales: {
        root: {
          label: '简体中文',
          lang: 'zh-CN',
        },
      },
      social: [],
      customCss: ['./src/styles/custom.css'],
      sidebar: [
        {
          label: '项目概览',
          items: [
            { label: '文档首页', slug: 'index' },
            { label: '项目介绍', slug: 'overview/introduction' },
            { label: '整体架构', slug: 'overview/architecture' },
            { label: '服务清单', slug: 'overview/service-list' },
          ],
        },
        {
          label: '快速开始',
          items: [
            { label: '本地开发', slug: 'guide/local-dev' },
            { label: '数据库准备', slug: 'guide/database-setup' },
            { label: 'Nacos 准备', slug: 'guide/nacos-setup' },
            { label: '开发规范总览', slug: 'guide/development-rules' },
          ],
        },
        {
          label: '架构设计',
          items: [
            { label: '微服务拆分与权限架构', slug: 'architecture/microservice-split' },
            { label: '版本栈推荐', slug: 'architecture/version-stack' },
            { label: 'CI/CD 流程', slug: 'architecture/cicd' },
            { label: '网关设计', slug: 'architecture/gateway' },
            { label: '服务通信', slug: 'architecture/service-communication' },
            { label: '事件驱动', slug: 'architecture/event-driven' },
          ],
        },
        {
          label: '权限体系',
          items: [
            { label: 'IAM 权限中心', slug: 'security/iam' },
            { label: '列权限', slug: 'security/column-permission' },
            { label: '后端权限接入', slug: 'security/backend-permission-integration' },
            { label: '路由菜单权限', slug: 'frontend/route-menu-permission' },
          ],
        },
        {
          label: '租户体系',
          items: [
            { label: '多租户设计', slug: 'security/tenant' },
          ],
        },
        {
          label: '安全治理',
          items: [
            { label: '服务间鉴权', slug: 'security/service-auth' },
          ],
        },
        {
          label: '后端开发',
          items: [
            { label: '后端开发总览', slug: 'backend' },
            { label: 'DDD 分层与领域建模', slug: 'backend/ddd' },
            { label: '服务命名规范', slug: 'backend/service-naming' },
            { label: '项目根目录与 Maven 结构', slug: 'backend/project-structure' },
            { label: '服务工程结构', slug: 'backend/service-structure' },
            { label: '后端服务说明', slug: 'backend/services' },
            { label: '服务文档模板', slug: 'backend/service-doc-template' },
            { label: '接口文档位置规范', slug: 'backend/api-doc-location' },
            { label: '接口契约规范', slug: 'backend/api-contract' },
            { label: '数据库结构文档规范', slug: 'backend/database-doc-location' },
            { label: '权限接入规范', slug: 'backend/permission-integration' },
            { label: '数据库迁移规范', slug: 'backend/database-migration' },
          ],
        },
        {
          label: '后端服务详情',
          items: [
            { label: 'xuan-gateway', slug: 'backend/services/xuan-gateway' },
            { label: 'xuan-iam', slug: 'backend/services/xuan-iam' },
            { label: 'xuan-tenant', slug: 'backend/services/xuan-tenant' },
            { label: 'xuan-product', slug: 'backend/services/xuan-product' },
            { label: 'xuan-party', slug: 'backend/services/xuan-party' },
            { label: 'xuan-warehouse', slug: 'backend/services/xuan-warehouse' },
            { label: 'xuan-sales', slug: 'backend/services/xuan-sales' },
            { label: 'xuan-sales DDD 示例', slug: 'backend/services/xuan-sales/ddd' },
            { label: 'xuan-procurement', slug: 'backend/services/xuan-procurement' },
            { label: 'xuan-inventory', slug: 'backend/services/xuan-inventory' },
            { label: 'xuan-manufacturing', slug: 'backend/services/xuan-manufacturing' },
            { label: 'xuan-finance', slug: 'backend/services/xuan-finance' },
            { label: 'xuan-document', slug: 'backend/services/xuan-document' },
            { label: 'xuan-audit', slug: 'backend/services/xuan-audit' },
            { label: 'xuan-query', slug: 'backend/services/xuan-query' },
          ],
        },
        {
          label: '前端接入',
          items: [
            { label: '页面开发规范', slug: 'frontend/page-development' },
            { label: '路由菜单权限', slug: 'frontend/route-menu-permission' },
          ],
        },
        {
          label: '功能组件',
          items: [
            { label: '提交功能', slug: 'components/submit' },
            { label: '导入功能', slug: 'components/import' },
            { label: '导出功能', slug: 'components/export' },
            { label: '打印功能', slug: 'components/print' },
          ],
        },
        {
          label: '部署运维',
          items: [
            { label: '部署方案', slug: 'ops/deployment' },
            { label: '监控', slug: 'ops/monitoring' },
            { label: '日志', slug: 'ops/logging' },
          ],
        },
        {
          label: '附录',
          items: [
            { label: '常见问题', slug: 'faq/common-issues' },
            { label: '变更记录', slug: 'changelog' },
          ],
        },
      ],
    }),
  ],
});




