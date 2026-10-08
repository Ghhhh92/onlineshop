package com.onlineshop.statemachine;

import com.onlineshop.common.BizException;
import com.onlineshop.entity.Product;
import com.onlineshop.enums.ProductStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 商品状态机单元测试
 *
 * @author 高佳豪
 * @date 2026-10-08
 */
class ProductStateMachineTest {

    private ProductStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new ProductStateMachine();
    }

    private Product productWith(ProductStatus status) {
        Product p = new Product();
        p.setId(1L);
        p.setName("测试商品");
        p.setStatus(status);
        return p;
    }

    @Nested
    @DisplayName("合法状态流转")
    class LegalTransitions {

        @Test
        @DisplayName("在售 → 已冻结（队首进入交易）")
        void onSaleToFrozen() {
            Product p = productWith(ProductStatus.ON_SALE);
            stateMachine.freeze(p);
            assertThat(p.getStatus()).isEqualTo(ProductStatus.FROZEN);
        }

        @Test
        @DisplayName("已冻结 → 已下架（交易成功）")
        void frozenToDelisted() {
            Product p = productWith(ProductStatus.FROZEN);
            stateMachine.delist(p);
            assertThat(p.getStatus()).isEqualTo(ProductStatus.DELISTED);
        }

        @Test
        @DisplayName("已冻结 → 在售（交易失败回退）")
        void frozenToOnSale() {
            Product p = productWith(ProductStatus.FROZEN);
            stateMachine.unfreeze(p);
            assertThat(p.getStatus()).isEqualTo(ProductStatus.ON_SALE);
        }
    }

    @Nested
    @DisplayName("非法状态流转必须被拒绝")
    class IllegalTransitions {

        @Test
        @DisplayName("在售 → 已下架（跳过冻结）被拒绝")
        void onSaleToDelisted() {
            Product p = productWith(ProductStatus.ON_SALE);
            assertThatThrownBy(() -> stateMachine.delist(p))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("非法状态流转");
            assertThat(p.getStatus()).isEqualTo(ProductStatus.ON_SALE);
        }

        @Test
        @DisplayName("已下架商品不能再流转")
        void delistedCannotTransit() {
            Product p = productWith(ProductStatus.DELISTED);
            assertThatThrownBy(() -> stateMachine.freeze(p))
                    .isInstanceOf(BizException.class);
            assertThat(p.getStatus()).isEqualTo(ProductStatus.DELISTED);
        }

        @Test
        @DisplayName("商品为 null 时抛异常")
        void nullProduct() {
            assertThatThrownBy(() -> stateMachine.transit(null, ProductStatus.FROZEN))
                    .isInstanceOf(BizException.class);
        }
    }
}
