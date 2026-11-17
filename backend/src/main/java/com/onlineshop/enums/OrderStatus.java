package com.onlineshop.enums;

/**
 * 订单状态（挑战需求 B）。
 * <p>
 * 课程原文规定的五个业务状态：客户下单 → 商家确认 → 备货完成 → 开始发货 → 交易完成；
 * 取消后进入「已取消」，它是终态，不算在五个业务状态里。
 * <p>
 * 取消规则：客户只能在「开始发货」之前取消；商家可以取消到「交易完成」之前。
 *
 * @author 高佳豪
 * @date 2026-11-17
 */
public enum OrderStatus {

    PENDING("客户下单"),
    CONFIRMED("商家确认"),
    PREPARED("备货完成"),
    SHIPPED("开始发货"),
    COMPLETED("交易完成"),
    CANCELED("已取消");

    private final String desc;

    OrderStatus(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 判断当前状态是否可以流转到目标状态
     */
    public boolean canTransitTo(OrderStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case PENDING -> target == CONFIRMED || target == CANCELED;
            case CONFIRMED -> target == PREPARED || target == CANCELED;
            case PREPARED -> target == SHIPPED || target == CANCELED;
            case SHIPPED -> target == COMPLETED || target == CANCELED;
            case COMPLETED, CANCELED -> false;
        };
    }

    /**
     * 客户能否取消：只有「开始发货」之前的三个状态可以
     */
    public boolean cancelableByCustomer() {
        return this == PENDING || this == CONFIRMED || this == PREPARED;
    }

    /**
     * 商家能否取消：交易完成之前的四个状态都可以，含已发货
     */
    public boolean cancelableBySeller() {
        return this == PENDING || this == CONFIRMED || this == PREPARED || this == SHIPPED;
    }
}
