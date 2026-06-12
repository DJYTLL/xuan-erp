package com.xuan.erp.warehouse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class XuanWarehouseApplication {

    public static void main(String[] args) {
        SpringApplication.run(XuanWarehouseApplication.class, args);
    }
}
