package com.onlineshop.enums;

/**
 * 商品状态枚举
 * 在售 → 已冻结 → 已下架
 * 已冻结 → 在售（交易失败 / 手动解冻回退）
 *
 * @author 高佳豪
 * @date 2026-10-08
 */
public enum ProductStatus {

    ON_SALE("在售"),
    FROZEN("已冻结"),
    DELISTED("已下架");

    private final String desc;

    ProductStatus(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 判断当前状态是否可以流转到目标状态
     */
    public boolean canTransitTo(ProductStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case ON_SALE -> target == FROZEN;
            case FROZEN -> target == DELISTED || target == ON_SALE;
            case DELISTED -> false;
        };
    }
}
