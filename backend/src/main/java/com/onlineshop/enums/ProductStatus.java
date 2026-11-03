package com.onlineshop.enums;

/**
 * 商品状态枚举（挑战需求 A 起为四态）
 * 在售 → 已冻结（卖家发起进入交易）/ 售罄（库存归零）
 * 已冻结 → 已下架（交易成功）/ 在售（交易失败回退）
 * 售罄 → 在售（卖家补货，库存回到大于 0）
 * 已下架为成交终态，不可再流转
 *
 * @author 高佳豪
 * @date 2026-10-08
 */
public enum ProductStatus {

    ON_SALE("在售"),
    FROZEN("已冻结"),
    SALES_OUT("售罄"),
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
            case ON_SALE -> target == FROZEN || target == SALES_OUT;
            case FROZEN -> target == DELISTED || target == ON_SALE;
            case SALES_OUT -> target == ON_SALE;
            case DELISTED -> false;
        };
    }
}
