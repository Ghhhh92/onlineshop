package com.onlineshop.common;

/**
 * 业务异常
 *
 * @author onlineshop-team
 * @date 2026-10-08
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        super(message);
        this.code = 400;
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
