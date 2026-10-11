package com.onlineshop.mapper;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlineshop.entity.Product;
import com.onlineshop.enums.ProductStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 商品数据访问：增删改查、状态存储、分页。每个用例结束后回滚，不留测试数据。
 *
 * @author 李帆
 * @date 2026-10-11
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductMapperTest {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("插入后回填自增主键，查询能取回全部字段")
    void insertAndSelect() {
        Product product = product("二手键盘", ProductStatus.ON_SALE, LocalDateTime.of(2026, 10, 11, 9, 30));
        product.setDescription("九成新");
        productMapper.insert(product);

        assertThat(product.getId()).isNotNull();
        Product saved = productMapper.selectById(product.getId());
        assertThat(saved.getName()).isEqualTo("二手键盘");
        assertThat(saved.getDescription()).isEqualTo("九成新");
        assertThat(saved.getPrice()).isEqualByComparingTo("99.90");
        assertThat(saved.getStatus()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(saved.getCreateTime()).isEqualTo(LocalDateTime.of(2026, 10, 11, 9, 30));
    }

    @Test
    @DisplayName("状态在库里存成枚举名")
    void statusStoredAsName() {
        Product product = product("台灯", ProductStatus.FROZEN, LocalDateTime.now());
        productMapper.insert(product);

        String stored = jdbcTemplate.queryForObject(
                "SELECT status FROM product WHERE id = ?", String.class, product.getId());
        assertThat(stored).isEqualTo("FROZEN");
    }

    @Test
    @DisplayName("更新状态后再查询能看到新状态")
    void updateStatus() {
        Product product = product("书架", ProductStatus.ON_SALE, LocalDateTime.now());
        productMapper.insert(product);

        product.setStatus(ProductStatus.FROZEN);
        productMapper.updateById(product);

        assertThat(productMapper.selectById(product.getId()).getStatus()).isEqualTo(ProductStatus.FROZEN);
    }

    @Test
    @DisplayName("分页查询：按发布时间倒序，返回当前页和总数")
    void selectPage() {
        LocalDateTime base = LocalDateTime.of(2026, 10, 1, 8, 0);
        for (int i = 1; i <= 3; i++) {
            productMapper.insert(product("历史商品" + i, ProductStatus.DELISTED, base.plusDays(i)));
        }

        Page<Product> firstPage = productMapper.selectPage(new Page<>(1, 2), Wrappers.<Product>lambdaQuery()
                .eq(Product::getStatus, ProductStatus.DELISTED)
                .likeRight(Product::getName, "历史商品")
                .orderByDesc(Product::getCreateTime));
        assertThat(firstPage.getTotal()).isEqualTo(3);
        assertThat(firstPage.getRecords()).extracting(Product::getName).containsExactly("历史商品3", "历史商品2");

        Page<Product> secondPage = productMapper.selectPage(new Page<>(2, 2), Wrappers.<Product>lambdaQuery()
                .eq(Product::getStatus, ProductStatus.DELISTED)
                .likeRight(Product::getName, "历史商品")
                .orderByDesc(Product::getCreateTime));
        assertThat(secondPage.getRecords()).extracting(Product::getName).containsExactly("历史商品1");
    }

    private static Product product(String name, ProductStatus status, LocalDateTime createTime) {
        Product product = new Product();
        product.setName(name);
        product.setPrice(new BigDecimal("99.90"));
        product.setStatus(status);
        product.setCreateTime(createTime);
        return product;
    }
}
