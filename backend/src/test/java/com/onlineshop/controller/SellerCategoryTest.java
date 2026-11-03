package com.onlineshop.controller;

import com.onlineshop.entity.Category;
import com.onlineshop.entity.Product;
import com.onlineshop.enums.ProductStatus;
import com.onlineshop.mapper.CategoryMapper;
import com.onlineshop.mapper.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 卖家维护商品类别（US-32）：新增一级/二级类别、最多两级、改名与调序、删除约束。
 * 每个用例先清空类别和商品，结束后回滚。
 *
 * @author 叶文锦
 * @date 2026-11-03
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SellerCategoryTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockHttpSession seller;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.update("DELETE FROM purchase_intent");
        jdbcTemplate.update("DELETE FROM product_image");
        jdbcTemplate.update("DELETE FROM product");
        jdbcTemplate.update("DELETE FROM category");
        seller = (MockHttpSession) mockMvc.perform(post("/api/seller/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    @Test
    @DisplayName("新增一级类别：parentId 为 0")
    void createFirstLevel() throws Exception {
        create("{\"name\":\"数码\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.name").value("数码"))
                .andExpect(jsonPath("$.data.parentId").value(0));
    }

    @Test
    @DisplayName("新增二级类别：parentId 填一级类别的 id")
    void createSecondLevel() throws Exception {
        Category root = category("数码", 0L, 0);

        create("{\"name\":\"手机\",\"parentId\":" + root.getId() + "}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("手机"))
                .andExpect(jsonPath("$.data.parentId").value(root.getId()));
    }

    @Test
    @DisplayName("最多两级：往二级类别下再挂类别被拒绝")
    void thirdLevelRejected() throws Exception {
        Category root = category("数码", 0L, 0);
        Category second = category("手机", root.getId(), 0);

        create("{\"name\":\"苹果\",\"parentId\":" + second.getId() + "}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("最多支持两级类别"));
    }

    @Test
    @DisplayName("上级类别不存在时拒绝")
    void missingParentRejected() throws Exception {
        create("{\"name\":\"数码\",\"parentId\":999999}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("上级类别不存在"));
    }

    @Test
    @DisplayName("类别名称为空：提示必填")
    void blankNameRejected() throws Exception {
        create("{\"name\":\"  \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("类别名称不能为空"));
    }

    @Test
    @DisplayName("同一个上级下类别名称不能重复")
    void duplicateNameRejected() throws Exception {
        category("数码", 0L, 0);

        create("{\"name\":\"数码\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("同一上级下已存在同名类别"));
    }

    @Test
    @DisplayName("改名并调整顺序")
    void updateRenameAndSort() throws Exception {
        Category root = category("数码", 0L, 0);

        update(root.getId(), "{\"name\":\"数码产品\",\"sort\":5}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("数码产品"))
                .andExpect(jsonPath("$.data.sort").value(5));
    }

    @Test
    @DisplayName("改名改成同级的重名：拒绝")
    void updateDuplicateRejected() throws Exception {
        category("数码", 0L, 0);
        Category other = category("家居", 0L, 0);

        update(other.getId(), "{\"name\":\"数码\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("同一上级下已存在同名类别"));
    }

    @Test
    @DisplayName("删除类别：下面还有子类别时拒绝")
    void deleteWithChildrenRejected() throws Exception {
        Category root = category("数码", 0L, 0);
        category("手机", root.getId(), 0);

        deleteCategory(root.getId())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("该类别下还有子类别，请先删除子类别"));
        assertThat(categoryMapper.selectById(root.getId())).isNotNull();
    }

    @Test
    @DisplayName("删除类别：下面还挂着商品时拒绝")
    void deleteWithProductsRejected() throws Exception {
        Category root = category("数码", 0L, 0);
        product("台灯", root.getId());

        deleteCategory(root.getId())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("该类别下还有商品，不能删除"));
    }

    @Test
    @DisplayName("删除空类别：成功，类别从库里移除")
    void deleteEmptyCategory() throws Exception {
        Category root = category("数码", 0L, 0);

        deleteCategory(root.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        assertThat(categoryMapper.selectById(root.getId())).isNull();
    }

    @Test
    @DisplayName("删除不存在的类别：返回 404")
    void deleteMissingCategory() throws Exception {
        deleteCategory(999999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("类别不存在"));
    }

    @Test
    @DisplayName("新增、改名、删除类别都需要卖家登录")
    void requireLogin() throws Exception {
        mockMvc.perform(post("/api/seller/category")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"数码\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/seller/category/1")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"数码\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/seller/category/1"))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions create(String body) throws Exception {
        return mockMvc.perform(post("/api/seller/category").session(seller)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions update(Long id, String body) throws Exception {
        return mockMvc.perform(put("/api/seller/category/" + id).session(seller)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions deleteCategory(Long id) throws Exception {
        return mockMvc.perform(delete("/api/seller/category/" + id).session(seller));
    }

    private Category category(String name, Long parentId, int sort) {
        Category category = new Category();
        category.setName(name);
        category.setParentId(parentId);
        category.setSort(sort);
        category.setCreateTime(LocalDateTime.of(2026, 11, 3, 9, 0));
        categoryMapper.insert(category);
        return category;
    }

    private Product product(String name, Long categoryId) {
        Product product = new Product();
        product.setName(name);
        product.setPrice(new BigDecimal("10.00"));
        product.setStock(1);
        product.setCategoryId(categoryId);
        product.setStatus(ProductStatus.ON_SALE);
        product.setCreateTime(LocalDateTime.of(2026, 11, 3, 9, 0));
        productMapper.insert(product);
        return product;
    }
}
