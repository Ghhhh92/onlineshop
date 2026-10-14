package com.onlineshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.onlineshop.entity.Product;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品数据访问。增删改查和分页查询（selectPage）继承自 BaseMapper。
 *
 * @author 李帆
 * @date 2026-10-11
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
