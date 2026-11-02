package com.onlineshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
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
}
