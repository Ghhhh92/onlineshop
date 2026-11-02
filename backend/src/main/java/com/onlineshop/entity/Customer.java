package com.onlineshop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户（注册的买家），对应 customer 表。
 *
 * @author 李帆
 * @date 2026-11-02
 */
@Data
@TableName("customer")
public class Customer {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名，唯一 */
    private String username;

    /** 密码（BCrypt 加密后的），不能返回给前端 */
    private String password;

    /** 联系电话 */
    private String phone;

    /** 默认交易地点 */
    private String defaultLocation;

    /** 注册时间 */
    private LocalDateTime createTime;
}
