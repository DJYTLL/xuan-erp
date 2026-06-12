package com.xuan.erp.finance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class XuanFinanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(XuanFinanceApplication.class, args);
    }
}
