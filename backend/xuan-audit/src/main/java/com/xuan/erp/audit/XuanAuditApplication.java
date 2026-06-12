package com.xuan.erp.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class XuanAuditApplication {

    public static void main(String[] args) {
        SpringApplication.run(XuanAuditApplication.class, args);
    }
}
