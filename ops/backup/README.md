# Xuan ERP 一键备份脚本

这组脚本面向 Linux 服务器运行，第一版采用“服务器脚本 + cron 定时任务 + 本机备份目录”的方式，不写入 Java 业务代码。

## 目录

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
```

## 服务器安装

1. 将本目录同步到服务器：

```bash
mkdir -p /data/xuan-erp/backup/scripts
cp -a ops/backup/. /data/xuan-erp/backup/scripts/
```

2. 创建服务器本地配置：

```bash
cd /data/xuan-erp/backup/scripts
cp .env.example .env
chmod 600 .env
```

3. 修改 `.env` 中的真实账号、密码、Nacos dataId、目录和端口。

## 手动执行

```bash
bash /data/xuan-erp/backup/scripts/backup-all.sh --dry-run
bash /data/xuan-erp/backup/scripts/backup-all.sh
```

备份批次目录默认生成在：

```text
/data/xuan-erp/backup/runs/YYYYMMDD-HHMMSS
```

每次执行会写入 `manifest.md`，记录 PostgreSQL、Nacos、Redis、Elasticsearch、配置文件的成功或跳过状态。

## 安装定时任务

```bash
bash /data/xuan-erp/backup/scripts/install-cron.sh
```

默认安装：

```cron
0 2 * * * /bin/bash /data/xuan-erp/backup/scripts/backup-all.sh >> /data/xuan-erp/backup/logs/cron.log 2>&1
```

## 恢复验证

没有恢复验证不算备份。PostgreSQL 至少定期执行：

```bash
bash /data/xuan-erp/backup/scripts/restore-postgres-test.sh
```

默认会寻找最近一次 `*.dump.gz`，恢复到临时测试库 `xuan_erp_restore_test` 并执行 `select 1;`。

## 边界

- Redis 只备份 RDB / AOF 持久化文件，不作为业务事实来源。
- Nacos 第一版通过 OpenAPI 按 dataId 导出配置；后续如果确认使用外部数据库，再增加数据库级备份。
- Elasticsearch 依赖服务器已配置 snapshot repository；业务索引需要纳入快照，SkyWalking 索引只做短期保留。
- `.env` 包含敏感信息，只保存在服务器本地，不提交到仓库。
