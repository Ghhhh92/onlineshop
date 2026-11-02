package com.onlineshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.onlineshop.entity.Customer;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户数据访问。增删改查继承自 BaseMapper。
 *
 * @author 李帆
 * @date 2026-11-02
 */
@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {

    /**
     * 按用户名查客户，不存在时返回 null
     */
    default Customer selectByUsername(String username) {
        return selectOne(Wrappers.<Customer>lambdaQuery().eq(Customer::getUsername, username));
    }
}
