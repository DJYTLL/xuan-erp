package com.xuan.erp.iam.domain.repository;

import com.xuan.erp.iam.domain.model.IamResourceAction;
import com.xuan.erp.iam.domain.model.IamResourceState;
import com.xuan.erp.iam.domain.model.IamRoleStateActionRule;
import java.util.List;
import java.util.Optional;

/**
 * IAM 状态动作权限管理仓储端口，隔离状态、动作和角色规则维护细节。
 */
public interface IamStateActionRuleManagementRepository {

    List<IamResourceState> findStates(Long tenantId, String resourceKey, Boolean enabled);

    Optional<IamResourceState> findStateById(Long stateId);

    Optional<IamResourceState> findState(Long tenantId, String resourceKey, String stateCode);

    IamResourceState saveState(IamResourceState state, String operator);

    void setStateEnabled(Long stateId, boolean enabled, String operator);

    List<IamResourceAction> findActions(Long tenantId, String resourceKey, Boolean enabled);

    Optional<IamResourceAction> findActionById(Long actionId);

    Optional<IamResourceAction> findAction(Long tenantId, String resourceKey, String actionCode);

    IamResourceAction saveAction(IamResourceAction action, String operator);

    void setActionEnabled(Long actionId, boolean enabled, String operator);

    List<IamRoleStateActionRule> findRoleRules(Long tenantId, Long roleId);

    void replaceRoleRules(Long tenantId, Long roleId, List<IamRoleStateActionRule> rules, String operator);
}
