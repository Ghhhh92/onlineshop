package com.onlineshop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 卖家，对应 seller 表。系统只有一个卖家账号。
 *
 * @author 李帆
 * @date 2026-10-11
 */
@Data
@TableName("seller")
public class Seller {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名，唯一 */
    private String username;

    /** 密码（BCrypt 加密后的），不能返回给前端 */
    private String password;

    /** 密码版本号，每次修改密码加 1；登录时记进会话，版本对不上的会话视为已失效 */
    private Integer passwordVersion;
}
