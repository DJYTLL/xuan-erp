package com.xuan.erp.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class XuanProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(XuanProductApplication.class, args);
    }
}
