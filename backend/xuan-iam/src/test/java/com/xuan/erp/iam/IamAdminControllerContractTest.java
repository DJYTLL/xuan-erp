package com.xuan.erp.iam;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class IamAdminControllerContractTest {

    private static final Path CONTROLLER_DIR = Path.of("src/main/java/com/xuan/erp/iam/interfaces/controller");

    @Test
    void menuPermissionAndRoleControllersExposeAdminWriteEndpoints() throws IOException {
        String menuController = Files.readString(CONTROLLER_DIR.resolve("IamMenuController.java"));
        String permissionController = Files.readString(CONTROLLER_DIR.resolve("IamPermissionController.java"));
        String roleController = Files.readString(CONTROLLER_DIR.resolve("IamRoleController.java"));
        String userController = Files.readString(CONTROLLER_DIR.resolve("IamUserController.java"));
        String initTemplateController = Files.readString(CONTROLLER_DIR.resolve("IamTenantInitTemplateController.java"));

        assertTrue(menuController.contains("@PostMapping"), "菜单管理必须支持新增菜单");
        assertTrue(menuController.contains("@PutMapping(\"/{menuId}\")"), "菜单管理必须支持修改菜单");
        assertTrue(menuController.contains("@PostMapping(\"/{menuId}/enable\")"), "菜单管理必须支持启用菜单");
        assertTrue(menuController.contains("@PostMapping(\"/{menuId}/disable\")"), "菜单管理必须支持停用菜单");

        assertTrue(permissionController.contains("@PostMapping"), "权限管理必须支持新增权限");
        assertTrue(permissionController.contains("@PutMapping(\"/{permissionId}\")"), "权限管理必须支持修改权限");
        assertTrue(permissionController.contains("@PostMapping(\"/{permissionId}/enable\")"), "权限管理必须支持启用权限");
        assertTrue(permissionController.contains("@PostMapping(\"/{permissionId}/disable\")"), "权限管理必须支持停用权限");

        assertTrue(roleController.contains("@PostMapping"), "角色管理必须支持新增角色");
        assertTrue(roleController.contains("@PutMapping(\"/{roleId}\")"), "角色管理必须支持修改角色");
        assertTrue(roleController.contains("@GetMapping(\"/{roleId}/permissions\")"), "角色管理必须支持查询角色权限");
        assertTrue(roleController.contains("@PutMapping(\"/{roleId}/permissions\")"), "角色管理必须支持保存角色权限");

        assertTrue(userController.contains("@GetMapping(\"/{userId}/roles\")"), "用户管理必须支持查询用户角色");
        assertTrue(userController.contains("@PutMapping(\"/{userId}/roles\")"), "用户管理必须支持保存用户角色");

        assertTrue(initTemplateController.contains("@GetMapping"), "初始化模板必须支持查询模板列表");
        assertTrue(initTemplateController.contains("@PostMapping"), "初始化模板必须支持新增模板");
        assertTrue(initTemplateController.contains("@PutMapping(\"/{templateId}\")"), "初始化模板必须支持修改模板");
        assertTrue(initTemplateController.contains("@PutMapping(\"/{templateId}/permissions\")"), "初始化模板必须支持保存模板权限");
    }
}
