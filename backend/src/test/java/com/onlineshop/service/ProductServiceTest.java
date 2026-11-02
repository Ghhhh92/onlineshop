package com.onlineshop.service;

import com.onlineshop.dto.PublishProductRequest;
import com.onlineshop.entity.Product;
import com.onlineshop.enums.ProductStatus;
import com.onlineshop.mapper.ProductImageMapper;
import com.onlineshop.mapper.ProductMapper;
import com.onlineshop.statemachine.ProductStateMachine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 商品发布 Service 单元测试（Mockito）
 *
 * @author 叶文锦
 * @date 2026-10-13
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductImageMapper productImageMapper;

    @Mock
    private ProductStateMachine productStateMachine;

    @InjectMocks
    private ProductService productService;

    /** 构造发布请求对象，与后端 publish(PublishProductRequest) 的签名对齐 */
    private PublishProductRequest publishRequest(String name, String price) {
        PublishProductRequest request = new PublishProductRequest();
        request.setName(name);
        request.setDescription("描述");
        request.setImage("img");
        request.setPrice(new BigDecimal(price));
        request.setStock(1);
        return request;
    }

    @Test
    @DisplayName("发布成功，状态为在售，图片记进图片表")
    void publishSuccess() {
        Product product = productService.publish(publishRequest("iPhone", "6999"));

        assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(product.getStock()).isEqualTo(1);
        verify(productMapper).insert(any(Product.class));
        verify(productImageMapper).insertAll(any(), eq(List.of("img")));
    }

    @Test
    @DisplayName("可以同时发布多件商品：发布前不查已有的在售商品")
    void publishMultipleProducts() {
        productService.publish(publishRequest("iPhone", "6999"));
        productService.publish(publishRequest("iPad", "3999"));

        verify(productMapper, never()).selectCount(any());
        verify(productMapper, times(2)).insert(any(Product.class));
    }

    @Test
    @DisplayName("冻结商品：调用状态机并回写")
    void freezeProduct() {
        Product product = new Product();
        product.setId(1L);
        product.setStatus(ProductStatus.ON_SALE);
        when(productMapper.selectById(1L)).thenReturn(product);

        productService.freezeProduct(1L);

        verify(productStateMachine).freeze(product);
        verify(productMapper).updateById(product);
    }

    @Test
    @DisplayName("下架商品：记录成交时间并回写")
    void delistProduct() {
        Product product = new Product();
        product.setId(1L);
        product.setStatus(ProductStatus.FROZEN);
        when(productMapper.selectById(1L)).thenReturn(product);

        productService.delistProduct(1L);

        verify(productStateMachine).delist(product);
        assertThat(product.getFinishTime()).isNotNull();
        verify(productMapper).updateById(product);
    }
}
