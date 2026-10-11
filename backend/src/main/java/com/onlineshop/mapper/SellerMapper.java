package com.onlineshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.onlineshop.entity.Seller;
import org.apache.ibatis.annotations.Mapper;

/**
 * 卖家数据访问。增删改查继承自 BaseMapper。
 *
 * @author 李帆
 * @date 2026-10-11
 */
@Mapper
public interface SellerMapper extends BaseMapper<Seller> {

    /**
     * 按用户名查卖家，不存在时返回 null
     */
    default Seller selectByUsername(String username) {
        return selectOne(Wrappers.<Seller>lambdaQuery().eq(Seller::getUsername, username));
    }
}
