package com.onlineshop.controller;

import com.onlineshop.common.BizException;
import com.onlineshop.entity.Product;
import com.onlineshop.enums.ProductStatus;
import com.onlineshop.mapper.ProductMapper;
import com.onlineshop.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 库存（US-33）：交易成功时库存减 1，卖家修改库存（补货）。每个用例结束后回滚。
 *
 * @author 李帆
 * @date 2026-11-02
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductStockTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductService productService;

    private MockHttpSession seller;

    @BeforeEach
    void login() throws Exception {
        seller = (MockHttpSession) mockMvc.perform(post("/api/seller/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    @Test
    @DisplayName("库存减 1：返回剩下的库存；减到 0 之后再减，提示库存不足，库存不会变成负数")
    void decreaseStock() {
        Product product = product(ProductStatus.FROZEN, 2);

        assertThat(productService.decreaseStock(product.getId())).isEqualTo(1);
        assertThat(productService.decreaseStock(product.getId())).isZero();
        assertThatThrownBy(() -> productService.decreaseStock(product.getId()))
                .isInstanceOf(BizException.class)
                .hasMessage("库存不足");
        assertThat(productMapper.selectById(product.getId()).getStock()).isZero();
    }

    @Test
    @DisplayName("库存减 1：商品不存在时提示")
    void decreaseStockOfMissingProduct() {
        assertThatThrownBy(() -> productService.decreaseStock(999999L))
                .isInstanceOf(BizException.class)
                .hasMessage("商品不存在");
    }

    @Test
    @DisplayName("在售商品库存减到 0：自动转为售罄（US-33）")
    void decreaseStockSellsOutWhenReachesZero() {
        Product product = product(ProductStatus.ON_SALE, 1);

        assertThat(productService.decreaseStock(product.getId())).isZero();
        assertThat(productMapper.selectById(product.getId()).getStatus()).isEqualTo(ProductStatus.SALES_OUT);
    }

    @Test
    @DisplayName("售罄商品补货：直接恢复在售，无需重新发布（US-33）")
    void restockSalesOut() throws Exception {
        Product product = product(ProductStatus.SALES_OUT, 0);

        updateStock(product.getId(), "{\"stock\":5}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stock").value(5))
                .andExpect(jsonPath("$.data.status").value("ON_SALE"));
        assertThat(productMapper.selectById(product.getId()).getStatus()).isEqualTo(ProductStatus.ON_SALE);
    }

    @Test
    @DisplayName("售罄商品不再接收新的购买意向（US-33）")
    void salesOutRejectsNewIntent() throws Exception {
        Product product = product(ProductStatus.SALES_OUT, 0);

        mockMvc.perform(post("/api/buyer/intent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + product.getId() + ",\"name\":\"张三\",\"phone\":\"13800000000\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("商品已售罄"));
    }

    @Test
    @DisplayName("修改库存需要登录")
    void updateStockRequiresLogin() throws Exception {
        Product product = product(ProductStatus.ON_SALE, 1);

        mockMvc.perform(put("/api/seller/product/" + product.getId() + "/stock")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"stock\":5}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("在售商品补货：库存改成新的数量，状态不变")
    void restockOnSale() throws Exception {
        Product product = product(ProductStatus.ON_SALE, 1);

        updateStock(product.getId(), "{\"stock\":5}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stock").value(5))
                .andExpect(jsonPath("$.data.status").value("ON_SALE"));
        assertThat(productMapper.selectById(product.getId()).getStock()).isEqualTo(5);
    }

    @Test
    @DisplayName("交易中的商品也能改库存，状态仍是已冻结")
    void restockFrozen() throws Exception {
        Product product = product(ProductStatus.FROZEN, 1);

        updateStock(product.getId(), "{\"stock\":3}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stock").value(3))
                .andExpect(jsonPath("$.data.status").value("FROZEN"));
    }

    @Test
    @DisplayName("库存没填或不在 1～99999：提示具体原因；商品不存在返回 404")
    void updateStockValidation() throws Exception {
        Product product = product(ProductStatus.ON_SALE, 1);

        updateStock(product.getId(), "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("库存不能为空"));
        updateStock(product.getId(), "{\"stock\":0}")
                .andExpect(jsonPath("$.message").value("库存至少为 1"));
        updateStock(product.getId(), "{\"stock\":100000}")
                .andExpect(jsonPath("$.message").value("库存不能超过 99999"));
        updateStock(999999L, "{\"stock\":5}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("商品不存在"));
        assertThat(productMapper.selectById(product.getId()).getStock()).isEqualTo(1);
    }

    private ResultActions updateStock(Long id, String body) throws Exception {
        return mockMvc.perform(put("/api/seller/product/" + id + "/stock").session(seller)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private Product product(ProductStatus status, int stock) {
        Product product = new Product();
        product.setName("台灯");
        product.setPrice(new BigDecimal("35.00"));
        product.setStock(stock);
        product.setStatus(status);
        product.setCreateTime(LocalDateTime.of(2026, 11, 1, 9, 0));
        productMapper.insert(product);
        return product;
    }
}
