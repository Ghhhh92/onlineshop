package com.onlineshop;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 在线购物系统后端启动类
 *
 * @author onlineshop-team
 * @date 2026-10-08
 */
@SpringBootApplication
@MapperScan("com.onlineshop.mapper")
public class OnlineshopApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnlineshopApplication.class, args);
    }
}
