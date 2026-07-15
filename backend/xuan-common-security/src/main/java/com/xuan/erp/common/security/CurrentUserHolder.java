package com.xuan.erp.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * 当前登录用户读取入口。
 *
 * <p>业务代码不应直接解析请求头或 JWT。认证过滤器会把通过验签的用户放入
 * Spring Security 上下文，本类只负责把上下文中的 {@link CurrentUser} 以稳定 API 暴露给业务层。</p>
 */
public final class CurrentUserHolder {

    private CurrentUserHolder() {
    }

    /**
     * 读取当前登录用户。
     *
     * @return 当前登录用户；未认证或认证主体不是 CurrentUser 时返回空
     */
    public static Optional<CurrentUser> current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser currentUser)) {
            return Optional.empty();
        }
        return Optional.of(currentUser);
    }

    /**
     * 读取必需的当前登录用户。
     *
     * @return 当前登录用户
     */
    public static CurrentUser required() {
        return current().orElseThrow(() -> new IllegalStateException("当前登录用户不存在"));
    }

    /**
     * 读取当前登录用户名；没有认证上下文时回退为 system。
     *
     * <p>该方法用于审计字段、操作人字段等兼容场景。真正需要强制登录的业务逻辑应使用
     * {@link #required()}。</p>
     *
     * @return 当前用户名或 system
     */
    public static String usernameOrSystem() {
        return current()
                .map(CurrentUser::username)
                .filter(username -> !username.isBlank())
                .orElse("system");
    }
}
