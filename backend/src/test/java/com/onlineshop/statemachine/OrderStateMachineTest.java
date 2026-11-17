package com.onlineshop.statemachine;

import com.onlineshop.common.BizException;
import com.onlineshop.entity.CustomerOrder;
import com.onlineshop.enums.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 订单状态机（挑战需求 B）：五个业务状态按序推进，客户/商家的取消边界，终态不可再流转。
 *
 * @author 高佳豪
 * @date 2026-11-17
 */
class OrderStateMachineTest {

    private final OrderStateMachine machine = new OrderStateMachine();

    @Test
    @DisplayName("正常推进：客户下单 → 商家确认 → 备货完成 → 开始发货 → 交易完成")
    void happyPath() {
        CustomerOrder order = order(OrderStatus.PENDING);

        machine.confirm(order);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        machine.prepare(order);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PREPARED);
        machine.ship(order);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        machine.complete(order);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("不能跳过中间状态：客户下单不能直接发货或完成")
    void cannotSkipStates() {
        CustomerOrder order = order(OrderStatus.PENDING);

        assertThatThrownBy(() -> machine.ship(order))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("非法状态流转");
        assertThatThrownBy(() -> machine.complete(order))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("非法状态流转");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"PENDING", "CONFIRMED", "PREPARED"})
    @DisplayName("客户取消：开始发货之前的三个状态都可以取消")
    void customerCanCancelBeforeShipping(OrderStatus status) {
        CustomerOrder order = order(status);

        machine.cancel(order, true);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    @DisplayName("客户取消：已发货、已完成、已取消都不能取消")
    void customerCannotCancelAfterShipping() {
        assertThatThrownBy(() -> machine.cancel(order(OrderStatus.SHIPPED), true))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已发货");
        assertThatThrownBy(() -> machine.cancel(order(OrderStatus.COMPLETED), true))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已发货");
        assertThatThrownBy(() -> machine.cancel(order(OrderStatus.CANCELED), true))
                .isInstanceOf(BizException.class);
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"PENDING", "CONFIRMED", "PREPARED", "SHIPPED"})
    @DisplayName("商家取消：交易完成之前的四个状态都可以取消，含已发货")
    void sellerCanCancelBeforeCompleted(OrderStatus status) {
        CustomerOrder order = order(status);

        machine.cancel(order, false);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    @DisplayName("商家取消：已完成、已取消都不能取消")
    void sellerCannotCancelCompleted() {
        assertThatThrownBy(() -> machine.cancel(order(OrderStatus.COMPLETED), false))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("交易完成");
        assertThatThrownBy(() -> machine.cancel(order(OrderStatus.CANCELED), false))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("终态不可再推进：已取消不能确认，已完成不能取消")
    void terminalStatesCannotTransit() {
        CustomerOrder canceled = order(OrderStatus.CANCELED);
        assertThatThrownBy(() -> machine.confirm(canceled))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("非法状态流转");
        assertThat(canceled.getStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    private static CustomerOrder order(OrderStatus status) {
        CustomerOrder order = new CustomerOrder();
        order.setId(1L);
        order.setStatus(status);
        return order;
    }
}
