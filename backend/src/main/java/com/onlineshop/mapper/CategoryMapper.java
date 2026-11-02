package com.onlineshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.onlineshop.entity.Category;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品类别数据访问。增删改查继承自 BaseMapper。
 *
 * @author 李帆
 * @date 2026-11-02
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
