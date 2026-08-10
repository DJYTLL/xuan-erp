package com.xuan.erp.iam.domain.repository;

import java.util.List;
import java.util.Map;

/**
 * IAM 状态动作权限快照仓储端口，隔离多角色规则合并查询。
 */
@FunctionalInterface
public interface IamStateActionRuleRepository {

    Map<String, List<String>> findMergedStateActionRulesByRoleIds(Long tenantId, List<Long> roleIds);
}
