package com.onlineshop.dto;

import com.onlineshop.entity.CustomerOrder;
import com.onlineshop.entity.CustomerOrderItem;
import com.onlineshop.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 一笔订单：订单信息加商品明细。
 * <p>
 * 客户自己的订单不带客户信息（客户名、电话为 null），卖家查看订单时才填充，用于确认下单人。
 *
 * @param id               订单 id
 * @param orderNo          订单号
 * @param status           状态值
 * @param statusText       状态中文：客户下单 / 商家确认 / 备货完成 / 开始发货 / 交易完成 / 已取消
 * @param totalAmount      订单总金额
 * @param createTime       下单时间
 * @param customerUsername 下单客户的用户名，客户侧查看时为 null
 * @param customerPhone    下单客户的联系电话，客户侧查看时为 null
 * @param items            商品明细
 * @author 高佳豪
 * @date 2026-11-17
 */
public record OrderDetail(Long id, String orderNo, OrderStatus status, String statusText,
                          BigDecimal totalAmount, LocalDateTime createTime,
                          String customerUsername, String customerPhone,
                          List<OrderItemInfo> items) {

    /**
     * 客户侧使用：不带客户信息
     */
    public static OrderDetail of(CustomerOrder order, List<CustomerOrderItem> items) {
        return of(order, items, null, null);
    }

    /**
     * 卖家侧使用：带上客户信息
     */
    public static OrderDetail of(CustomerOrder order, List<CustomerOrderItem> items,
                                 String customerUsername, String customerPhone) {
        return new OrderDetail(order.getId(), order.getOrderNo(), order.getStatus(),
                order.getStatus().getDesc(), order.getTotalAmount(), order.getCreateTime(),
                customerUsername, customerPhone,
                items.stream().map(OrderItemInfo::of).toList());
    }
}
