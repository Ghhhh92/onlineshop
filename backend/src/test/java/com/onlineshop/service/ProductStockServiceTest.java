package com.onlineshop.service;

import com.onlineshop.common.BizException;
import com.onlineshop.entity.Product;
import com.onlineshop.enums.ProductStatus;
import com.onlineshop.mapper.ProductMapper;
import com.onlineshop.statemachine.ProductStateMachine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 补货时的状态处理（US-33）：已下架的商品要经过状态机恢复在售，在售、交易中的只改库存。
 * 状态机用 Mock 代替，只验证补货有没有交给状态机、状态机拒绝时库存不改。
 *
 * @author 李帆
 * @date 2026-11-02
 */
@ExtendWith(MockitoExtension.class)
class ProductStockServiceTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductStateMachine productStateMachine;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("售罄（库存为 0）的商品补货：经状态机恢复在售，再保存新库存")
    void restockSalesOut() {
        Product product = product(ProductStatus.SALES_OUT, 0);
        doAnswer(invocation -> {
            product.setStatus(ProductStatus.ON_SALE);
            return null;
        }).when(productStateMachine).restock(product);

        productService.updateStock(1L, 5);

        assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(product.getStock()).isEqualTo(5);
        verify(productMapper).updateById(product);
    }

    @Test
    @DisplayName("已下架（成交终态）的商品不允许补货，库存不改")
    void restockDelistedRejected() {
        Product product = product(ProductStatus.DELISTED, 0);

        assertThatThrownBy(() -> productService.updateStock(1L, 5))
                .isInstanceOf(BizException.class)
                .hasMessage("商品已下架，不能补货");

        assertThat(product.getStock()).isZero();
        verify(productMapper, never()).updateById(any(Product.class));
    }

    @Test
    @DisplayName("在售商品补货：不经过状态机，只改库存")
    void restockOnSale() {
        Product product = product(ProductStatus.ON_SALE, 1);

        productService.updateStock(1L, 5);

        verify(productStateMachine, never()).transit(any(), any());
        assertThat(product.getStock()).isEqualTo(5);
        verify(productMapper).updateById(product);
    }

    private Product product(ProductStatus status, int stock) {
        Product product = new Product();
        product.setId(1L);
        product.setStatus(status);
        product.setStock(stock);
        when(productMapper.selectByIdForUpdate(1L)).thenReturn(product);
        return product;
    }
}
