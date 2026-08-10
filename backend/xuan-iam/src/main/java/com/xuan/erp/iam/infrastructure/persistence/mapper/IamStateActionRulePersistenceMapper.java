package com.xuan.erp.iam.infrastructure.persistence.mapper;

import com.xuan.erp.iam.domain.model.IamResourceAction;
import com.xuan.erp.iam.domain.model.IamResourceState;
import com.xuan.erp.iam.domain.model.IamRoleStateActionRule;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * IAM 状态动作权限持久化 Mapper。
 */
@Mapper
public interface IamStateActionRulePersistenceMapper {

    List<IamResourceState> findStates(
            @Param("tenantId") Long tenantId,
            @Param("resourceKey") String resourceKey,
            @Param("enabled") Boolean enabled);

    IamResourceState findStateById(@Param("stateId") Long stateId);

    IamResourceState findState(
            @Param("tenantId") Long tenantId,
            @Param("resourceKey") String resourceKey,
            @Param("stateCode") String stateCode);

    Long insertState(
            @Param("tenantId") Long tenantId,
            @Param("resourceKey") String resourceKey,
            @Param("stateCode") String stateCode,
            @Param("stateName") String stateName,
            @Param("description") String description,
            @Param("sortNo") Integer sortNo,
            @Param("enabled") boolean enabled,
            @Param("metadataJson") String metadataJson,
            @Param("operator") String operator);

    void updateState(
            @Param("stateId") Long stateId,
            @Param("stateName") String stateName,
            @Param("description") String description,
            @Param("sortNo") Integer sortNo,
            @Param("enabled") boolean enabled,
            @Param("metadataJson") String metadataJson,
            @Param("operator") String operator);

    void setStateEnabled(
            @Param("stateId") Long stateId,
            @Param("enabled") boolean enabled,
            @Param("operator") String operator);

    List<IamResourceAction> findActions(
            @Param("tenantId") Long tenantId,
            @Param("resourceKey") String resourceKey,
            @Param("enabled") Boolean enabled);

    IamResourceAction findActionById(@Param("actionId") Long actionId);

    IamResourceAction findAction(
            @Param("tenantId") Long tenantId,
            @Param("resourceKey") String resourceKey,
            @Param("actionCode") String actionCode);

    Long insertAction(
            @Param("tenantId") Long tenantId,
            @Param("resourceKey") String resourceKey,
            @Param("actionCode") String actionCode,
            @Param("actionName") String actionName,
            @Param("permissionCode") String permissionCode,
            @Param("description") String description,
            @Param("sortNo") Integer sortNo,
            @Param("enabled") boolean enabled,
            @Param("metadataJson") String metadataJson,
            @Param("operator") String operator);

    void updateAction(
            @Param("actionId") Long actionId,
            @Param("actionName") String actionName,
            @Param("permissionCode") String permissionCode,
            @Param("description") String description,
            @Param("sortNo") Integer sortNo,
            @Param("enabled") boolean enabled,
            @Param("metadataJson") String metadataJson,
            @Param("operator") String operator);

    void setActionEnabled(
            @Param("actionId") Long actionId,
            @Param("enabled") boolean enabled,
            @Param("operator") String operator);

    List<IamRoleStateActionRule> findRoleRules(
            @Param("tenantId") Long tenantId,
            @Param("roleId") Long roleId);

    void disableRoleRules(
            @Param("tenantId") Long tenantId,
            @Param("roleId") Long roleId,
            @Param("operator") String operator);

    void insertRoleRule(
            @Param("tenantId") Long tenantId,
            @Param("roleId") Long roleId,
            @Param("resourceKey") String resourceKey,
            @Param("stateCode") String stateCode,
            @Param("actionCode") String actionCode,
            @Param("operator") String operator);

    List<IamStateActionRuleEntry> findMergedStateActionRulesByRoleIds(
            @Param("tenantId") Long tenantId,
            @Param("roleIds") List<Long> roleIds);

    /**
     * 当前权限快照聚合状态动作权限时使用的扁平行。
     */
    record IamStateActionRuleEntry(String ruleKey, String actionCode) {
    }
}
