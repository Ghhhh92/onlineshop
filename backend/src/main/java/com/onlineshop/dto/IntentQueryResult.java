package com.onlineshop.dto;

import com.onlineshop.enums.IntentStatus;

import java.time.LocalDateTime;

/**
 * 买家凭口令码查到的意向（US-06、US-07）
 *
 * @param code        口令码
 * @param position    当前排队位次，从 1 开始；前面有人撤销或作废时自动前移
 * @param status      意向状态（QUEUING / TRADING）
 * @param statusText  状态的中文
 * @param name        买家姓名
 * @param phone       联系电话，中间四位打码
 * @param productName 商品名称
 * @param createTime  提交时间
 * @author 李帆
 * @date 2026-10-19
 */
public record IntentQueryResult(String code, int position, IntentStatus status, String statusText,
                                String name, String phone, String productName, LocalDateTime createTime) {
}
