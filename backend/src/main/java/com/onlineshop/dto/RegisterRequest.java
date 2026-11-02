package com.onlineshop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 客户注册请求（US-36）：用户名、密码、电话、默认交易地点都必填。
 *
 * @author 李帆
 * @date 2026-11-02
 */
@Data
public class RegisterRequest {

    @NotNull(message = "请填写用户名")
    @Pattern(regexp = "^[A-Za-z0-9_\\u4e00-\\u9fa5]{2,20}$", message = "用户名为 2～20 位中文、字母、数字或下划线")
    private String username;

    /** 与卖家修改密码的长度要求一致 */
    @NotNull(message = "请填写密码")
    @Size(min = 8, max = 64, message = "密码长度为 8～64 个字符")
    private String password;

    /** 与提交意向的电话校验一致：1 开头的 11 位数字 */
    @NotNull(message = "请填写联系电话")
    @Pattern(regexp = "^1\\d{10}$", message = "请输入 11 位手机号")
    private String phone;

    @NotBlank(message = "请填写默认交易地点")
    @Size(max = 100, message = "默认交易地点不能超过 100 个字符")
    private String defaultLocation;
}
