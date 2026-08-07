package com.md;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @Author: CM
 * @Description TODO
 */
@EnableFeignClients(basePackages = "com.md.feign")
@MapperScan(basePackages = "com.md.mapper")
@SpringBootApplication
@EnableDiscoveryClient
public class CourseApp {
    public static void main(String[] args) {
        SpringApplication.run(CourseApp.class,args);
    }
}