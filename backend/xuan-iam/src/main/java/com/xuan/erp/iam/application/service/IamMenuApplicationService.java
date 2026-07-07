package com.xuan.erp.iam.application.service;

import com.xuan.erp.iam.domain.model.IamMenu;
import com.xuan.erp.iam.domain.repository.IamMenuRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
/**
 * IAM 菜单应用服务，负责全局菜单目录查询用例。
 */
public class IamMenuApplicationService {

    private final IamMenuRepository menuRepository;

    public IamMenuApplicationService(IamMenuRepository menuRepository) {
        this.menuRepository = menuRepository;
    }

    public List<IamMenu> listMenus() {
        return menuRepository.findActiveMenus();
    }
}
