package com.xuan.erp.iam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
/**
 * IAM 服务启动入口，负责装载身份、权限、菜单和授权快照相关组件。
 */
public class XuanIamApplication {

    public static void main(String[] args) {
        SpringApplication.run(XuanIamApplication.class, args);
    }
}
