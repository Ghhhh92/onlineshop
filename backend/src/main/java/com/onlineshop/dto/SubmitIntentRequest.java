package com.onlineshop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 买家提交购买意向（US-04）：只填姓名和联系电话，不需要注册登录。
 *
 * @author 李帆
 * @date 2026-10-19
 */
@Data
public class SubmitIntentRequest {

    @NotBlank(message = "请填写姓名")
    @Size(max = 50, message = "姓名不能超过 50 个字符")
    private String name;

    /** 与前端表单的校验规则一致：1 开头的 11 位数字 */
    @NotBlank(message = "请填写联系电话")
    @Pattern(regexp = "^1\\d{10}$", message = "请输入 11 位手机号")
    private String phone;
}
