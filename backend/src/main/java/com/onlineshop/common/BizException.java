package com.onlineshop.common;

/**
 * 业务异常：业务规则不满足时抛出，由 {@link GlobalExceptionHandler} 转成统一响应。
 * <p>
 * code 用 HTTP 状态码表示错误类别，不传时为 400；例如未登录用 401。
 *
 * @author 李帆
 * @date 2026-10-11
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        this(400, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
