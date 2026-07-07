package com.xuan.erp.iam.domain.repository;

import com.xuan.erp.iam.domain.model.IamMenu;
import java.util.List;
import java.util.Optional;

/**
 * IAM 菜单仓储端口，隔离全局菜单目录的读取细节。
 */
public interface IamMenuRepository {

    Optional<IamMenu> findByCode(String code);

    List<IamMenu> findActiveMenus();
}
