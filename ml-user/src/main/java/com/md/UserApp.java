package com.md;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;


/**
 * @Author: CM
 * @Description 用户模块启动类
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan(basePackages = "com.md.mapper")
public class UserApp {
    public static void main(String[] args) {
        SpringApplication.run(UserApp.class,args);
    }
}