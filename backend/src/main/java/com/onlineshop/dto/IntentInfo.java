package com.onlineshop.dto;

import com.onlineshop.entity.PurchaseIntent;
import com.onlineshop.enums.IntentStatus;

import java.time.LocalDateTime;

/**
 * 卖家看到的意向购买人（US-16）
 *
 * @param id         意向 id
 * @param name       买家姓名
 * @param phone      联系电话（卖家要线下联系，返回完整号码）
 * @param createTime 提交时间
 * @param position   当前排队位次；已结束（成功、失败、撤销）的意向为 null
 * @param status     意向状态
 * @param statusText 状态的中文
 * @author 李帆
 * @date 2026-10-19
 */
public record IntentInfo(Long id, String name, String phone, LocalDateTime createTime,
                         Integer position, IntentStatus status, String statusText) {

    public static IntentInfo of(PurchaseIntent intent, Integer position) {
        return new IntentInfo(intent.getId(), intent.getName(), intent.getPhone(), intent.getCreateTime(),
                position, intent.getStatus(), intent.getStatus().getDesc());
    }
}
