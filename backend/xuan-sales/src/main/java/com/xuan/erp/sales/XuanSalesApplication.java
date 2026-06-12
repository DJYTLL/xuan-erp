package com.xuan.erp.sales;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class XuanSalesApplication {

    public static void main(String[] args) {
        SpringApplication.run(XuanSalesApplication.class, args);
    }
}
