package com.xuan.erp.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class XuanInventoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(XuanInventoryApplication.class, args);
    }
}
