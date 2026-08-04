package com.xuan.erp.iam.domain.repository;

import java.util.List;
import java.util.Map;

/**
 * IAM 列权限仓储端口，负责按租户上限和角色集合读取可消费的列权限规则。
 */
public interface IamColumnPermissionRepository {

    Map<String, Map<String, String>> findMergedColumnPermissionsByRoleIds(Long tenantId, List<Long> roleIds);
}
