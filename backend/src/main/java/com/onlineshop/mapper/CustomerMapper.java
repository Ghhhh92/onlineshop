package com.onlineshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

    /**
     * 按 id 查客户并对这一行加锁（SELECT … FOR UPDATE），不存在时返回 null。
     * 购物车、收藏的写操作先锁客户行，同一客户的并发改动按顺序执行。
     */
    default Customer selectByIdForUpdate(Long id) {
        return selectOne(Wrappers.<Customer>lambdaQuery().eq(Customer::getId, id).last("FOR UPDATE"));
    }

    /**
     * 全部客户，按注册时间倒序分页
     */
    default Page<Customer> selectLatestPage(Page<Customer> page) {
        return selectPage(page, Wrappers.<Customer>lambdaQuery()
                .orderByDesc(Customer::getCreateTime)
                .orderByDesc(Customer::getId));
    }
}
