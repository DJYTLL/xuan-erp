package com.xuan.erp.iam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class XuanIamApplication {

    public static void main(String[] args) {
        SpringApplication.run(XuanIamApplication.class, args);
    }
}
