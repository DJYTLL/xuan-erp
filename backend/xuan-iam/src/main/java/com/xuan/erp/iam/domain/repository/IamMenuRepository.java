package com.xuan.erp.iam.domain.repository;

import com.xuan.erp.iam.domain.model.IamMenu;
import java.util.List;
import java.util.Optional;

/**
 * IAM 菜单仓储端口，隔离全局菜单目录的读取细节。
 */
public interface IamMenuRepository {

    /**
     * 按菜单主键查询有效菜单。
     */
    default Optional<IamMenu> findById(Long id) {
        return Optional.empty();
    }

    Optional<IamMenu> findByCode(String code);

    List<IamMenu> findActiveMenus();

    /**
     * 保存菜单，新增和修改都通过领域对象表达最终状态。
     */
    default IamMenu save(IamMenu menu) {
        throw new UnsupportedOperationException("当前菜单仓储不支持写入");
    }
}
