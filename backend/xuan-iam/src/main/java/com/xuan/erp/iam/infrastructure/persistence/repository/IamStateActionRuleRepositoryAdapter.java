package com.xuan.erp.iam.infrastructure.persistence.repository;

import com.xuan.erp.iam.domain.model.IamResourceAction;
import com.xuan.erp.iam.domain.model.IamResourceState;
import com.xuan.erp.iam.domain.model.IamRoleStateActionRule;
import com.xuan.erp.iam.domain.repository.IamStateActionRuleManagementRepository;
import com.xuan.erp.iam.domain.repository.IamStateActionRuleRepository;
import com.xuan.erp.iam.infrastructure.persistence.mapper.IamStateActionRulePersistenceMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * IAM 状态动作权限仓储适配器，负责管理端维护和当前权限快照聚合查询。
 */
@Repository
public class IamStateActionRuleRepositoryAdapter implements IamStateActionRuleRepository, IamStateActionRuleManagementRepository {

    private final IamStateActionRulePersistenceMapper mapper;

    public IamStateActionRuleRepositoryAdapter(IamStateActionRulePersistenceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Map<String, List<String>> findMergedStateActionRulesByRoleIds(Long tenantId, List<Long> roleIds) {
        if (tenantId == null || roleIds == null || roleIds.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> result = new LinkedHashMap<>();
        mapper.findMergedStateActionRulesByRoleIds(tenantId, roleIds).forEach(row -> {
            List<String> actions = result.getOrDefault(row.ruleKey(), List.of());
            result.put(row.ruleKey(), java.util.stream.Stream.concat(actions.stream(), java.util.stream.Stream.of(row.actionCode()))
                    .distinct()
                    .sorted()
                    .toList());
        });
        return result;
    }

    @Override
    public List<IamResourceState> findStates(Long tenantId, String resourceKey, Boolean enabled) {
        return mapper.findStates(tenantId, resourceKey, enabled);
    }

    @Override
    public Optional<IamResourceState> findStateById(Long stateId) {
        return Optional.ofNullable(mapper.findStateById(stateId));
    }

    @Override
    public Optional<IamResourceState> findState(Long tenantId, String resourceKey, String stateCode) {
        return Optional.ofNullable(mapper.findState(tenantId, resourceKey, stateCode));
    }

    @Override
    public IamResourceState saveState(IamResourceState state, String operator) {
        if (state.id() == null) {
            Long id = mapper.insertState(
                    state.tenantId(),
                    state.resourceKey(),
                    state.stateCode(),
                    state.stateName(),
                    state.description(),
                    state.sortNo(),
                    state.enabled(),
                    state.metadataJson(),
                    operator);
            return mapper.findStateById(id);
        }
        mapper.updateState(
                state.id(),
                state.stateName(),
                state.description(),
                state.sortNo(),
                state.enabled(),
                state.metadataJson(),
                operator);
        return mapper.findStateById(state.id());
    }

    @Override
    public void setStateEnabled(Long stateId, boolean enabled, String operator) {
        mapper.setStateEnabled(stateId, enabled, operator);
    }

    @Override
    public List<IamResourceAction> findActions(Long tenantId, String resourceKey, Boolean enabled) {
        return mapper.findActions(tenantId, resourceKey, enabled);
    }

    @Override
    public Optional<IamResourceAction> findActionById(Long actionId) {
        return Optional.ofNullable(mapper.findActionById(actionId));
    }

    @Override
    public Optional<IamResourceAction> findAction(Long tenantId, String resourceKey, String actionCode) {
        return Optional.ofNullable(mapper.findAction(tenantId, resourceKey, actionCode));
    }

    @Override
    public IamResourceAction saveAction(IamResourceAction action, String operator) {
        if (action.id() == null) {
            Long id = mapper.insertAction(
                    action.tenantId(),
                    action.resourceKey(),
                    action.actionCode(),
                    action.actionName(),
                    action.permissionCode(),
                    action.description(),
                    action.sortNo(),
                    action.enabled(),
                    action.metadataJson(),
                    operator);
            return mapper.findActionById(id);
        }
        mapper.updateAction(
                action.id(),
                action.actionName(),
                action.permissionCode(),
                action.description(),
                action.sortNo(),
                action.enabled(),
                action.metadataJson(),
                operator);
        return mapper.findActionById(action.id());
    }

    @Override
    public void setActionEnabled(Long actionId, boolean enabled, String operator) {
        mapper.setActionEnabled(actionId, enabled, operator);
    }

    @Override
    public List<IamRoleStateActionRule> findRoleRules(Long tenantId, Long roleId) {
        return mapper.findRoleRules(tenantId, roleId);
    }

    @Override
    @Transactional
    public void replaceRoleRules(Long tenantId, Long roleId, List<IamRoleStateActionRule> rules, String operator) {
        mapper.disableRoleRules(tenantId, roleId, operator);
        for (IamRoleStateActionRule rule : rules) {
            mapper.insertRoleRule(tenantId, roleId, rule.resourceKey(), rule.stateCode(), rule.actionCode(), operator);
        }
    }
}
