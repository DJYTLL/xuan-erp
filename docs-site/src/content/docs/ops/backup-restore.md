---
title: "备份与恢复"
---

本文记录 Xuan ERP 前期部署阶段的一键备份方案。第一版采用“服务器脚本 + cron 定时任务 + 本机备份目录”，不写入 Java 业务代码。

## 基本约定

| 项 | 默认值 |
| --- | --- |
| 服务器 | `duaoyunxuan.com` Linux 服务器 |
| 备份目录 | `/data/xuan-erp/backup` |
| Compose / 配置目录 | `/data/xuan-erp/compose` |
| 脚本部署目录 | `/data/xuan-erp/backup/scripts` |
| 保留天数 | `30` 天 |
| 定时任务 | 每天 `02:00` |

备份文件默认按批次写入：

```text
/data/xuan-erp/backup/runs/YYYYMMDD-HHMMSS/
```

每个批次目录会生成 `manifest.md`，记录 PostgreSQL、Nacos、Redis、Elasticsearch、配置文件的成功、失败或跳过状态。

## 备份对象

| 对象 | 第一版做法 | 恢复定位 |
| --- | --- | --- |
| PostgreSQL | `pg_dump -F c` 生成自定义格式 dump | 业务事实数据，必须可恢复验证 |
| Nacos | 通过 OpenAPI 按 namespace / group / dataId 导出配置 | 配置中心恢复依据 |
| Redis | 备份 RDB / AOF 持久化文件 | 缓存、幂等键、短期状态，不作为业务事实来源 |
| Elasticsearch | 触发 snapshot repository 快照 | 业务搜索索引保留，SkyWalking 索引短期保留 |
| Docker Compose / `.env` / 反向代理配置 | 打包配置目录和备份脚本自身 | 服务器重建依据 |

## 脚本目录

仓库内脚本位置：

```text
ops/backup/
  .env.example
  backup-all.sh
  backup-postgres.sh
  backup-nacos.sh
  backup-redis.sh
  backup-elasticsearch.sh
  backup-config.sh
  restore-postgres-test.sh
  install-cron.sh
  README.md
```

服务器部署时同步到：

```text
/data/xuan-erp/backup/scripts/
```

## 配置文件

服务器本地创建 `.env`：

```bash
cd /data/xuan-erp/backup/scripts
cp .env.example .env
chmod 600 .env
```

关键配置：

```bash
BACKUP_ROOT=/data/xuan-erp/backup
COMPOSE_ROOT=/data/xuan-erp/compose
RETENTION_DAYS=30

POSTGRES_CONTAINER=xuan-pgsql
POSTGRES_HOST=127.0.0.1
POSTGRES_PORT=9042
POSTGRES_USER=xuan
POSTGRES_DB=xuan_erp

NACOS_BASE_URL=http://duaoyunxuan.com:9041
NACOS_USERNAME=nacos
NACOS_NAMESPACE=prod
NACOS_GROUP=XUAN_ERP_GROUP
NACOS_DATA_IDS=xuan-common.yaml,xuan-gateway.yaml,xuan-product.yaml

REDIS_CONTAINER=xuan-erp-redis
REDIS_DATA_DIR=/data/xuan-erp/data/redis

ES_BASE_URL=http://127.0.0.1:9040
ES_SNAPSHOT_REPOSITORY=xuan_backup
```

真实密码只写在服务器本地 `.env`，不提交仓库。

## 手动备份

先看执行计划：

```bash
bash /data/xuan-erp/backup/scripts/backup-all.sh --dry-run
```

执行一次完整备份：

```bash
bash /data/xuan-erp/backup/scripts/backup-all.sh
```

备份后检查：

```bash
ls -lah /data/xuan-erp/backup/runs
cat /data/xuan-erp/backup/runs/<run-id>/manifest.md
```

## 定时任务

安装 cron：

```bash
bash /data/xuan-erp/backup/scripts/install-cron.sh
```

默认写入：

```text
0 2 * * * /bin/bash /data/xuan-erp/backup/scripts/backup-all.sh >> /data/xuan-erp/backup/logs/cron.log 2>&1
```

查看日志：

```bash
tail -f /data/xuan-erp/backup/logs/cron.log
```

## PostgreSQL 恢复验证

没有恢复验证不算备份。至少定期执行一次恢复验证：

```bash
bash /data/xuan-erp/backup/scripts/restore-postgres-test.sh
```

默认行为：

1. 查找最近一次 `postgres/*.dump.gz`。
2. 恢复到临时测试库 `xuan_erp_restore_test`。
3. 执行 `select 1;` 验证可连接。

如果要指定备份文件：

```bash
bash /data/xuan-erp/backup/scripts/restore-postgres-test.sh --dump /data/xuan-erp/backup/runs/<run-id>/postgres/xuan_erp-<run-id>.dump.gz
```

恢复验证只用于确认备份可用，不替代正式灾难恢复演练。

## Nacos 边界

第一版通过 Nacos OpenAPI 导出指定 `namespace / group / dataId` 配置。

要求：

- `.env` 中必须维护 `NACOS_DATA_IDS`。
- 如果 Nacos 开启鉴权，服务器本地 `.env` 需要配置 `NACOS_PASSWORD`。
- 如果后续确认 Nacos 使用外部数据库，再增加数据库级备份脚本。

Nacos 配置导出主要解决“配置误删、服务器重建、环境迁移”问题，不负责替代配置变更审批。

## Redis 边界

Redis 备份只复制 RDB / AOF 持久化文件。

约定：

- Redis 不作为订单、库存、财务等业务事实来源。
- 业务事实仍以 PostgreSQL 为准。
- 如果需要在备份前强制执行 `SAVE`，可在服务器 `.env` 中设置 `REDIS_RUN_SAVE=true`，但要注意阻塞风险。

## Elasticsearch 边界

Elasticsearch 备份依赖服务器已提前配置 snapshot repository：

```bash
curl http://127.0.0.1:9040/_snapshot/xuan_backup
```

约定：

- 业务搜索索引纳入快照。
- SkyWalking APM 索引只做短期保留，不作为长期恢复目标。
- Elasticsearch 不是主业务数据库，重要业务数据必须能从 PostgreSQL 或业务事件重建。

## 配置文件边界

配置备份会打包：

- `/data/xuan-erp/compose`
- `/etc/nginx`
- `/etc/caddy`
- `/data/xuan-erp/backup/scripts`

如果实际服务器使用其它反向代理或目录，需要同步修改服务器 `.env`。

## 保留策略

第一版本机保留 `RETENTION_DAYS=30` 天。

后续待扩展：

- NAS 同步。
- 对象存储同步。
- 每周或每月冷备归档。
- 定期灾难恢复演练。

## 验收规则

- 能手动执行 `backup-all.sh --dry-run`。
- 能手动执行 `backup-all.sh` 并生成批次目录。
- `manifest.md` 能看到每个备份对象的状态。
- PostgreSQL 能通过 `restore-postgres-test.sh` 恢复到临时测试库。
- cron 已写入并能在第二天看到日志。
- Elasticsearch snapshot repository 不可用时，必须在 manifest 中明确跳过原因。
- 没有恢复验证不算备份。
