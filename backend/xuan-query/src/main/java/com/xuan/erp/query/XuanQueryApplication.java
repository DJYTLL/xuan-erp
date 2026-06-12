package com.xuan.erp.query;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class XuanQueryApplication {

    public static void main(String[] args) {
        SpringApplication.run(XuanQueryApplication.class, args);
    }
}
