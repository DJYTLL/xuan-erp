package com.xuan.erp.party;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class XuanPartyApplication {

    public static void main(String[] args) {
        SpringApplication.run(XuanPartyApplication.class, args);
    }
}
