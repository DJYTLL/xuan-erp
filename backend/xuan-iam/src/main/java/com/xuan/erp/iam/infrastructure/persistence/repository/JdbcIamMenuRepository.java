package com.xuan.erp.iam.infrastructure.persistence.repository;

import com.xuan.erp.iam.domain.model.IamMenu;
import com.xuan.erp.iam.domain.repository.IamMenuRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
/**
 * IAM 菜单 JDBC 仓储适配器，承接全局菜单目录的数据库访问。
 */
public class JdbcIamMenuRepository implements IamMenuRepository {

    @Override
    public Optional<IamMenu> findByCode(String code) {
        return Optional.empty();
    }

    @Override
    public List<IamMenu> findActiveMenus() {
        return List.of();
    }
}
