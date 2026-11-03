package com.onlineshop.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlineshop.entity.Product;
import com.onlineshop.enums.ProductStatus;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品数据访问。增删改查和分页查询（selectPage）继承自 BaseMapper。
 *
 * @author 李帆
 * @date 2026-10-11
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 当前商品：最新发布的在售或已冻结（交易中）商品，没有时返回 null。
     * 只展示一件商品的页面（买家首页、卖家后台的当前商品）用它；多商品用分页列表。
     */
    default Product selectCurrent() {
        return selectOne(currentQuery().last("LIMIT 1"));
    }

    /**
     * 同 {@link #selectCurrent()}，并对这一行加锁（SELECT … FOR UPDATE）。
     * 只能在事务里调用：同一时刻对当前商品的排队操作会按顺序执行，事务结束才释放锁。
     */
    default Product selectCurrentForUpdate() {
        return selectOne(currentQuery().last("LIMIT 1 FOR UPDATE"));
    }

    /**
     * 按 id 查商品，并对这一行加锁（SELECT … FOR UPDATE），不存在时返回 null。
     * 只能在事务里调用：同一件商品的排队操作会按顺序执行，事务结束才释放锁；不同商品互不影响。
     */
    default Product selectByIdForUpdate(Long id) {
        return selectOne(Wrappers.<Product>lambdaQuery().eq(Product::getId, id).last("FOR UPDATE"));
    }

    /**
     * 库存大于 0 时减 1，返回是否减成功。
     * 检查和扣减在同一条 UPDATE 里完成：多笔交易同时成交时一笔接一笔地减，库存不会减成负数。
     */
    default boolean decreaseStock(Long id) {
        return update(null, Wrappers.<Product>lambdaUpdate()
                .setSql("stock = stock - 1")
                .eq(Product::getId, id)
                .gt(Product::getStock, 0)) > 0;
    }

    /**
     * 买家能看到的商品：在售和交易中（已冻结）的，可按类别筛选，也可按关键词搜索名称或描述，按发布时间倒序分页
     */
    default Page<Product> selectBuyerPage(Page<Product> page, Long categoryId, String keyword) {
        String kw = keyword == null ? null : keyword.trim();
        return selectPage(page, Wrappers.<Product>lambdaQuery()
                .in(Product::getStatus, ProductStatus.ON_SALE, ProductStatus.FROZEN)
                .eq(categoryId != null, Product::getCategoryId, categoryId)
                .and(kw != null && !kw.isEmpty(),
                        w -> w.like(Product::getName, kw).or().like(Product::getDescription, kw))
                .orderByDesc(Product::getCreateTime)
                .orderByDesc(Product::getId));
    }

    /**
     * 卖家后台的商品列表：全部商品，可按状态、类别筛选，按发布时间倒序分页
     */
    default Page<Product> selectSellerPage(Page<Product> page, ProductStatus status, Long categoryId) {
        return selectPage(page, Wrappers.<Product>lambdaQuery()
                .eq(status != null, Product::getStatus, status)
                .eq(categoryId != null, Product::getCategoryId, categoryId)
                .orderByDesc(Product::getCreateTime)
                .orderByDesc(Product::getId));
    }

    /**
     * 历史商品（已下架），按成交时间倒序分页
     */
    default Page<Product> selectHistoryPage(Page<Product> page) {
        return selectPage(page, Wrappers.<Product>lambdaQuery()
                .eq(Product::getStatus, ProductStatus.DELISTED)
                .orderByDesc(Product::getFinishTime)
                .orderByDesc(Product::getId));
    }

    private static LambdaQueryWrapper<Product> currentQuery() {
        return Wrappers.<Product>lambdaQuery()
                .in(Product::getStatus, ProductStatus.ON_SALE, ProductStatus.FROZEN)
                .orderByDesc(Product::getId);
    }
}
