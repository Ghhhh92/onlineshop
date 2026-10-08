package com.onlineshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.onlineshop.entity.Product;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品 Mapper
 *
 * @author 李帆
 * @date 2026-10-08
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
