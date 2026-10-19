package com.onlineshop.dto;

import java.time.LocalDateTime;

/**
 * 提交意向成功后返回给买家的信息（US-05）
 *
 * @param code       唯一口令码，买家凭它查询位次
 * @param position   当前排队位次，从 1 开始
 * @param createTime 提交时间
 * @author 李帆
 * @date 2026-10-19
 */
public record IntentSubmitResult(String code, int position, LocalDateTime createTime) {
}
