package com.onlineshop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 卖家实体（单卖家）
 *
 * @author 李帆
 * @date 2026-10-08
 */
@Data
@TableName("seller")
public class Seller {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;
}
