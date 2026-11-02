package com.onlineshop.mapper;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.onlineshop.entity.Category;
import com.onlineshop.entity.Customer;
import com.onlineshop.entity.Product;
import com.onlineshop.entity.ProductImage;
import com.onlineshop.entity.PurchaseIntent;
import com.onlineshop.enums.IntentStatus;
import com.onlineshop.enums.ProductStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 挑战需求 A 新增的表和字段（V4）：实体和表字段对得上，能存能取；类别同一个上级下不重名，客户用户名不重复。
 * 每个用例结束后回滚，不留测试数据。
 *
 * @author 李帆
 * @date 2026-11-02
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ChallengeSchemaMapperTest {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ProductImageMapper productImageMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private PurchaseIntentMapper intentMapper;

    @Test
    @DisplayName("商品的库存和类别能存能取；不填库存时为 1 件，不填类别时为未分类")
    void productStockAndCategory() {
        Product lamp = product(5, 7L);
        Product cup = product(null, null);

        assertThat(productMapper.selectById(lamp.getId()))
                .extracting(Product::getStock, Product::getCategoryId).containsExactly(5, 7L);
        assertThat(productMapper.selectById(cup.getId()))
                .extracting(Product::getStock, Product::getCategoryId).containsExactly(1, null);
    }

    @Test
    @DisplayName("两级类别：一级的 parentId 为 0，二级指向一级；同一个上级下不能重名，不同上级下可以")
    void category() {
        Category digital = category("数码", Category.ROOT_PARENT_ID);
        Category books = category("图书", Category.ROOT_PARENT_ID);
        Category phone = category("手机", digital.getId());
        category("其他", digital.getId());
        category("其他", books.getId());

        assertThat(categoryMapper.selectById(phone.getId()))
                .extracting(Category::getName, Category::getParentId, Category::getSort)
                .containsExactly("手机", digital.getId(), 0);
        assertThat(categoryMapper.selectCount(Wrappers.<Category>lambdaQuery()
                .eq(Category::getParentId, Category.ROOT_PARENT_ID))).isEqualTo(2);
        assertThatThrownBy(() -> category("手机", digital.getId())).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    @DisplayName("商品图片按 sort 存取")
    void productImages() {
        Product lamp = product(1, null);
        image(lamp, "/api/uploads/b.png", 1);
        image(lamp, "/api/uploads/a.png", 0);

        assertThat(productImageMapper.selectList(Wrappers.<ProductImage>lambdaQuery()
                        .eq(ProductImage::getProductId, lamp.getId())
                        .orderByAsc(ProductImage::getSort)))
                .extracting(ProductImage::getUrl).containsExactly("/api/uploads/a.png", "/api/uploads/b.png");
    }

    @Test
    @DisplayName("客户能存能取；用户名不能重复")
    void customer() {
        Customer zhang = customer("zhangsan");

        assertThat(customerMapper.selectById(zhang.getId()))
                .extracting(Customer::getUsername, Customer::getPhone, Customer::getDefaultLocation, Customer::getCreateTime)
                .containsExactly("zhangsan", "13800000001", "图书馆门口", LocalDateTime.of(2026, 11, 2, 9, 0));
        assertThatThrownBy(() -> customer("zhangsan")).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    @DisplayName("意向记下提交的客户 id，匿名提交时为空")
    void intentCustomer() {
        Product lamp = product(1, null);
        Customer zhang = customer("zhangsan");
        PurchaseIntent byCustomer = intent(lamp, "CUSTOMER1", 1, zhang.getId());
        PurchaseIntent anonymous = intent(lamp, "ANONYMOUS", 2, null);

        assertThat(intentMapper.selectById(byCustomer.getId()).getCustomerId()).isEqualTo(zhang.getId());
        assertThat(intentMapper.selectById(anonymous.getId()).getCustomerId()).isNull();
    }

    private Product product(Integer stock, Long categoryId) {
        Product product = new Product();
        product.setName("台灯");
        product.setPrice(new BigDecimal("35.00"));
        product.setStock(stock);
        product.setCategoryId(categoryId);
        product.setStatus(ProductStatus.ON_SALE);
        product.setCreateTime(LocalDateTime.of(2026, 11, 2, 9, 0));
        productMapper.insert(product);
        return product;
    }

    private Category category(String name, long parentId) {
        Category category = new Category();
        category.setName(name);
        category.setParentId(parentId);
        category.setCreateTime(LocalDateTime.of(2026, 11, 2, 9, 0));
        categoryMapper.insert(category);
        return category;
    }

    private void image(Product product, String url, int sort) {
        ProductImage image = new ProductImage();
        image.setProductId(product.getId());
        image.setUrl(url);
        image.setSort(sort);
        productImageMapper.insert(image);
    }

    private Customer customer(String username) {
        Customer customer = new Customer();
        customer.setUsername(username);
        customer.setPassword("不会用来登录");
        customer.setPhone("13800000001");
        customer.setDefaultLocation("图书馆门口");
        customer.setCreateTime(LocalDateTime.of(2026, 11, 2, 9, 0));
        customerMapper.insert(customer);
        return customer;
    }

    private PurchaseIntent intent(Product product, String code, int position, Long customerId) {
        PurchaseIntent intent = new PurchaseIntent();
        intent.setProductId(product.getId());
        intent.setName("买家");
        intent.setPhone("1390000000" + position);
        intent.setCode(code);
        intent.setPosition(position);
        intent.setStatus(IntentStatus.QUEUING);
        intent.setCreateTime(LocalDateTime.of(2026, 11, 2, 10, 0));
        intent.setCustomerId(customerId);
        intentMapper.insert(intent);
        return intent;
    }
}
