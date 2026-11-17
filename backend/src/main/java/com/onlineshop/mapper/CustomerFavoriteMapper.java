package com.onlineshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlineshop.entity.CustomerFavorite;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 收藏数据访问。
 *
 * @author 范与恒
 * @date 2026-11-17
 */
@Mapper
public interface CustomerFavoriteMapper extends BaseMapper<CustomerFavorite> {

    /**
     * 某个客户的收藏，按收藏时间倒序分页
     */
    default Page<CustomerFavorite> selectCustomerPage(Page<CustomerFavorite> page, Long customerId) {
        return selectPage(page, Wrappers.<CustomerFavorite>lambdaQuery()
                .eq(CustomerFavorite::getCustomerId, customerId)
                .orderByDesc(CustomerFavorite::getCreateTime)
                .orderByDesc(CustomerFavorite::getId));
    }

    /**
     * 这批商品中已经被该客户收藏的商品 id
     */
    default Set<Long> selectFavoritedProductIds(Long customerId, Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Set.of();
        }
        return selectList(Wrappers.<CustomerFavorite>lambdaQuery()
                .eq(CustomerFavorite::getCustomerId, customerId)
                .in(CustomerFavorite::getProductId, productIds))
                .stream().map(CustomerFavorite::getProductId).collect(Collectors.toSet());
    }

    /**
     * 这个商品是否已被该客户收藏
     */
    default boolean exists(Long customerId, Long productId) {
        return selectCount(Wrappers.<CustomerFavorite>lambdaQuery()
                .eq(CustomerFavorite::getCustomerId, customerId)
                .eq(CustomerFavorite::getProductId, productId)) > 0;
    }
}
