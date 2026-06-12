package com.xuan.erp.document;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class XuanDocumentApplication {

    public static void main(String[] args) {
        SpringApplication.run(XuanDocumentApplication.class, args);
    }
}
