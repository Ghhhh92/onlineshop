package com.onlineshop.enums;

/**
 * 购买意向状态枚举
 * 排队中 → 交易中 → 成功 / 失败
 * 排队中 → 已撤销（买家主动撤销）
 *
 * @author 高佳豪
 * @date 2026-10-08
 */
public enum IntentStatus {

    QUEUING("排队中"),
    TRADING("交易中"),
    SUCCESS("成功"),
    FAILED("失败"),
    CANCELED("已撤销");

    private final String desc;

    IntentStatus(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    public boolean canTransitTo(IntentStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case QUEUING -> target == TRADING || target == CANCELED;
            case TRADING -> target == SUCCESS || target == FAILED;
            case SUCCESS, FAILED, CANCELED -> false;
        };
    }
}
