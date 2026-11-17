package com.onlineshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlineshop.entity.CustomerOrder;
import com.onlineshop.enums.OrderStatus;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单数据访问。
 *
 * @author 高佳豪
 * @date 2026-11-17
 */
@Mapper
public interface CustomerOrderMapper extends BaseMapper<CustomerOrder> {

    /**
     * 按 id 查订单并加锁（SELECT … FOR UPDATE），只能在事务里调用。
     * 同一笔订单的状态推进串行执行，避免确认、发货、取消同时操作一条订单。
     */
    default CustomerOrder selectByIdForUpdate(Long id) {
        return selectOne(Wrappers.<CustomerOrder>lambdaQuery()
                .eq(CustomerOrder::getId, id)
                .last("FOR UPDATE"));
    }

    /**
     * 客户自己的订单，按下单时间倒序分页，可按状态筛选
     */
    default Page<CustomerOrder> selectCustomerPage(Page<CustomerOrder> page, Long customerId, OrderStatus status) {
        return selectPage(page, Wrappers.<CustomerOrder>lambdaQuery()
                .eq(CustomerOrder::getCustomerId, customerId)
                .eq(status != null, CustomerOrder::getStatus, status)
                .orderByDesc(CustomerOrder::getCreateTime)
                .orderByDesc(CustomerOrder::getId));
    }

    /**
     * 卖家看到的全部订单（单卖家），按下单时间倒序分页，可按状态筛选
     */
    default Page<CustomerOrder> selectSellerPage(Page<CustomerOrder> page, OrderStatus status) {
        return selectPage(page, Wrappers.<CustomerOrder>lambdaQuery()
                .eq(status != null, CustomerOrder::getStatus, status)
                .orderByDesc(CustomerOrder::getCreateTime)
                .orderByDesc(CustomerOrder::getId));
    }
}
