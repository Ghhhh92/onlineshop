package com.onlineshop.statemachine;

import com.onlineshop.common.BizException;
import com.onlineshop.entity.CustomerOrder;
import com.onlineshop.enums.OrderStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 订单状态机（挑战需求 B）。
 * 集中管理订单状态流转规则，任何状态变更都必须经过本组件校验，禁止绕过状态机直接 setStatus / update。
 * <p>
 * 合法流转（课程原文五状态 + 已取消）：
 *   客户下单 → 商家确认 → 备货完成 → 开始发货 → 交易完成
 *   客户下单 / 商家确认 / 备货完成 → 已取消（客户、商家都可以取消，客户截止到开始发货之前）
 *   开始发货 → 已取消（只有商家可以）
 *   交易完成、已取消：终态，不可再流转
 *
 * @author 高佳豪
 * @date 2026-11-17
 */
@Component
public class OrderStateMachine {

    /**
     * 通用流转：校验合法性后改变订单状态
     *
     * @param order  订单
     * @param target 目标状态
     */
    public void transit(CustomerOrder order, OrderStatus target) {
        if (order == null) {
            throw new BizException("订单不存在");
        }
        OrderStatus current = order.getStatus();
        if (!current.canTransitTo(target)) {
            throw new BizException("非法状态流转：" + current.getDesc() + " → " + target.getDesc());
        }
        order.setStatus(target);
        order.setUpdateTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    }

    /**
     * 商家确认：客户下单 → 商家确认
     */
    public void confirm(CustomerOrder order) {
        transit(order, OrderStatus.CONFIRMED);
    }

    /**
     * 备货完成：商家确认 → 备货完成
     */
    public void prepare(CustomerOrder order) {
        transit(order, OrderStatus.PREPARED);
    }

    /**
     * 开始发货：备货完成 → 开始发货（库存校验在 Service 里做，这里只管状态）
     */
    public void ship(CustomerOrder order) {
        transit(order, OrderStatus.SHIPPED);
    }

    /**
     * 交易完成：开始发货 → 交易完成（库存扣减在 Service 里做）
     */
    public void complete(CustomerOrder order) {
        transit(order, OrderStatus.COMPLETED);
    }

    /**
     * 取消：进入「已取消」。
     * 客户只能在开始发货之前取消，商家可以取消到交易完成之前。
     *
     * @param byCustomer true 表示客户取消，false 表示商家取消
     */
    public void cancel(CustomerOrder order, boolean byCustomer) {
        if (order == null) {
            throw new BizException("订单不存在");
        }
        boolean allowed = byCustomer
                ? order.getStatus().cancelableByCustomer()
                : order.getStatus().cancelableBySeller();
        if (!allowed) {
            throw new BizException(byCustomer ? "订单已发货，不能取消" : "订单已交易完成，不能取消");
        }
        transit(order, OrderStatus.CANCELED);
    }
}
