package com.onlineshop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品类别，对应 category 表。卖家自建，最多两级：一级类别的 parentId 为 0，二级类别的 parentId 是所属一级类别的 id。
 *
 * @author 李帆
 * @date 2026-11-02
 */
@Data
@TableName("category")
public class Category {

    /** 一级类别的 parentId */
    public static final long ROOT_PARENT_ID = 0L;

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 类别名称，同一个上级下不重复 */
    private String name;

    /** 上级类别 id，一级类别为 0 */
    private Long parentId;

    /** 同级类别的展示顺序，小的在前 */
    private Integer sort;

    /** 创建时间 */
    private LocalDateTime createTime;
}
