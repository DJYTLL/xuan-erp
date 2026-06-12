package com.xuan.erp.tenant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class XuanTenantApplication {

    public static void main(String[] args) {
        SpringApplication.run(XuanTenantApplication.class, args);
    }
}
