package com.onlineshop.dto;

import com.onlineshop.entity.Customer;

import java.time.LocalDateTime;

/**
 * 返回给前端的客户信息，不含密码
 *
 * @param id              客户 id
 * @param username        用户名
 * @param phone           联系电话
 * @param defaultLocation 默认交易地点
 * @param createTime      注册时间
 * @author 李帆
 * @date 2026-11-02
 */
public record CustomerInfo(Long id, String username, String phone, String defaultLocation, LocalDateTime createTime) {

    public static CustomerInfo from(Customer customer) {
        return new CustomerInfo(customer.getId(), customer.getUsername(), customer.getPhone(),
                customer.getDefaultLocation(), customer.getCreateTime());
    }
}
