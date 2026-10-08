package com.onlineshop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.onlineshop.enums.IntentStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 购买意向实体
 *
 * @author 高佳豪
 * @date 2026-10-08
 */
@Data
@TableName("purchase_intent")
public class PurchaseIntent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    private String name;

    private String phone;

    /** 唯一口令码 */
    private String code;

    /** 排队位次 */
    private Integer position;

    private IntentStatus status;

    private LocalDateTime createTime;
}
