package com.onlineshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
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
}
