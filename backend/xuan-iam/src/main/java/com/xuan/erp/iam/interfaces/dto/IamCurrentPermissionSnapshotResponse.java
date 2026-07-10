package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

/**
 * 当前权限快照响应体，返回前端菜单、路由、按钮和列权限的最小权限快照。
 *
 * @param menus 当前用户可见菜单树
 * @param routePermissions 当前用户路由权限编码列表
 * @param buttonPermissions 当前用户按钮权限编码列表
 * @param columnPermissions 当前用户列权限映射
 * @param fieldPermissions 当前用户字段编辑权限映射
 * @param dataScopes 当前用户数据范围列表
 * @param stateActionRules 当前用户状态动作规则映射
 * @param authVersion 当前权限版本
 */
@Schema(description = "当前权限快照响应体")
public record IamCurrentPermissionSnapshotResponse(
        @Schema(description = "当前用户可见菜单树")
        List<IamCurrentMenuNodeResponse> menus,
        @Schema(description = "当前用户路由权限编码列表")
        List<String> routePermissions,
        @Schema(description = "当前用户按钮权限编码列表")
        List<String> buttonPermissions,
        @Schema(description = "当前用户列权限映射")
        Map<String, List<String>> columnPermissions,
        @Schema(description = "当前用户字段编辑权限映射")
        Map<String, List<String>> fieldPermissions,
        @Schema(description = "当前用户数据范围列表")
        List<String> dataScopes,
        @Schema(description = "当前用户状态动作规则映射")
        Map<String, List<String>> stateActionRules,
        @Schema(description = "当前权限版本", example = "7")
        Long authVersion
) {
}
