package com.onlineshop.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应体：{"code": 200, "message": "success", "data": ...}
 * <p>
 * 成功时 code 为 200；失败时 code 与 HTTP 状态码一致（400 参数或业务错误、401 未登录、404 不存在、500 服务器错误），
 * message 是可以直接展示给用户的提示。
 *
 * @param <T> 数据类型
 * @author 李帆
 * @date 2026-10-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    public static final int SUCCESS_CODE = 200;

    private int code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        return new Result<>(SUCCESS_CODE, "success", data);
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> fail(String message) {
        return fail(400, message);
    }
}
