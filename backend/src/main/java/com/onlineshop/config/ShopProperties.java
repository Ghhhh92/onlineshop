package com.onlineshop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * 本项目自定义的配置项，对应 application.yml 里的 shop.*
 *
 * @param cors          跨域配置
 * @param defaultSeller 初始卖家账号
 * @param upload        上传文件的保存位置
 * @author 李帆
 * @date 2026-10-11
 */
@ConfigurationProperties(prefix = "shop")
public record ShopProperties(@DefaultValue Cors cors, @DefaultValue DefaultSeller defaultSeller,
                             @DefaultValue Upload upload) {

    /**
     * @param allowedOrigins 允许跨域访问后端的前端地址
     */
    public record Cors(@DefaultValue({"http://localhost:5173", "http://127.0.0.1:5173"}) List<String> allowedOrigins) {
    }

    /**
     * 卖家表为空时自动创建的账号
     *
     * @param username 用户名
     * @param password 密码
     */
    public record DefaultSeller(@DefaultValue("admin") String username, @DefaultValue("123456") String password) {
    }

    /**
     * @param dir 商品图片的保存目录，相对路径相对于后端的启动目录
     */
    public record Upload(@DefaultValue("uploads") String dir) {
    }
}
