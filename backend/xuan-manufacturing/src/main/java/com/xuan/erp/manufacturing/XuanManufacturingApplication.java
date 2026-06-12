package com.xuan.erp.manufacturing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class XuanManufacturingApplication {

    public static void main(String[] args) {
        SpringApplication.run(XuanManufacturingApplication.class, args);
    }
}
