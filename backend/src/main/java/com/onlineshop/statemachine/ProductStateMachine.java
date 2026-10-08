package com.onlineshop.statemachine;

import com.onlineshop.common.BizException;
import com.onlineshop.entity.Product;
import com.onlineshop.enums.ProductStatus;
import org.springframework.stereotype.Component;

/**
 * 商品状态机
 * 集中管理商品状态流转规则，任何状态变更都必须经过本组件校验，
 * 禁止绕过状态机直接 setStatus / update。
 *
 * 合法流转：
 *   在售 → 已冻结（队首意向进入交易）
 *   已冻结 → 已下架（交易成功）
 *   已冻结 → 在售（交易失败 / 手动解冻回退）
 *
 * @author 高佳豪
 * @date 2026-10-08
 */
@Component
public class ProductStateMachine {

    /**
     * 通用流转：校验合法性后改变商品状态
     *
     * @param product 商品
     * @param target  目标状态
     */
    public void transit(Product product, ProductStatus target) {
        if (product == null) {
            throw new BizException("商品不存在");
        }
        ProductStatus current = product.getStatus();
        if (!current.canTransitTo(target)) {
            throw new BizException("非法状态流转：" + current.getDesc() + " → " + target.getDesc());
        }
        product.setStatus(target);
    }

    /**
     * 冻结：在售 → 已冻结
     */
    public void freeze(Product product) {
        transit(product, ProductStatus.FROZEN);
    }

    /**
     * 解冻回退：已冻结 → 在售
     */
    public void unfreeze(Product product) {
        transit(product, ProductStatus.ON_SALE);
    }

    /**
     * 下架（成交）：已冻结 → 已下架
     */
    public void delist(Product product) {
        transit(product, ProductStatus.DELISTED);
    }
}
