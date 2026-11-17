package com.onlineshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.onlineshop.entity.CustomerCart;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * 购物车数据访问。
 *
 * @author 范与恒
 * @date 2026-11-17
 */
@Mapper
public interface CustomerCartMapper extends BaseMapper<CustomerCart> {

    /**
     * 某个客户的购物车，按加入时间倒序
     */
    default List<CustomerCart> selectByCustomer(Long customerId) {
        return selectList(Wrappers.<CustomerCart>lambdaQuery()
                .eq(CustomerCart::getCustomerId, customerId)
                .orderByDesc(CustomerCart::getCreateTime)
                .orderByDesc(CustomerCart::getId));
    }

    /**
     * 某个客户的指定条目，并对这些行加锁（SELECT … FOR UPDATE），只能在事务里调用。
     * 结算、转收藏时用它锁住选中的条目，避免同一条目被并发处理两次。
     */
    default List<CustomerCart> selectByIdsForUpdate(Long customerId, Collection<Long> ids) {
        return selectList(Wrappers.<CustomerCart>lambdaQuery()
                .eq(CustomerCart::getCustomerId, customerId)
                .in(CustomerCart::getId, ids)
                .orderByAsc(CustomerCart::getId)
                .last("FOR UPDATE"));
    }

    /**
     * 某个客户某件商品已在购物车中的数量，没有时返回 null
     */
    default Integer selectQuantity(Long customerId, Long productId) {
        CustomerCart cart = selectOne(Wrappers.<CustomerCart>lambdaQuery()
                .eq(CustomerCart::getCustomerId, customerId)
                .eq(CustomerCart::getProductId, productId));
        return cart == null ? null : cart.getQuantity();
    }

    /**
     * 删掉某个客户的指定条目，返回删除条数
     */
    default int deleteByIds(Long customerId, Collection<Long> ids) {
        return delete(Wrappers.<CustomerCart>lambdaQuery()
                .eq(CustomerCart::getCustomerId, customerId)
                .in(CustomerCart::getId, ids));
    }
}
