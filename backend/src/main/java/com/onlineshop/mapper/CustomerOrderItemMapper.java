package com.onlineshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.onlineshop.entity.CustomerOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 订单明细数据访问。
 *
 * @author 高佳豪
 * @date 2026-11-17
 */
@Mapper
public interface CustomerOrderItemMapper extends BaseMapper<CustomerOrderItem> {

    /**
     * 一笔订单的全部明细
     */
    default List<CustomerOrderItem> selectByOrderId(Long orderId) {
        return selectList(Wrappers.<CustomerOrderItem>lambdaQuery()
                .eq(CustomerOrderItem::getOrderId, orderId)
                .orderByAsc(CustomerOrderItem::getId));
    }

    /**
     * 多笔订单的明细，一次查完再按订单分组，列表页不用逐条查
     */
    default List<CustomerOrderItem> selectByOrderIds(Collection<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<CustomerOrderItem>lambdaQuery()
                .in(CustomerOrderItem::getOrderId, orderIds)
                .orderByAsc(CustomerOrderItem::getId));
    }

    /**
     * 某商品「已发货但还没交易完成」的数量。
     * 开始发货时要保证「已发货量 + 本单数量 ≤ 当前库存」，用这个数做校验。
     */
    @Select("""
            SELECT COALESCE(SUM(i.quantity), 0)
            FROM customer_order_item i
            JOIN customer_order o ON o.id = i.order_id
            WHERE i.product_id = #{productId} AND o.status = 'SHIPPED'
            """)
    int shippedQuantity(@Param("productId") Long productId);
}
