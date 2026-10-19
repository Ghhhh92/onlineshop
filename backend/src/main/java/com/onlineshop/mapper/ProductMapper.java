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
     * 当前商品：在售或已冻结（交易中）的那一件，没有时返回 null
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
     * 历史商品（已下架），按成交时间倒序分页
     */
    default Page<Product> selectHistoryPage(long page, long size) {
        return selectPage(new Page<>(page, size), Wrappers.<Product>lambdaQuery()
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
