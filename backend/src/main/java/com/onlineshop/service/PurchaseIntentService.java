package com.onlineshop.service;

import com.onlineshop.entity.PurchaseIntent;
import com.onlineshop.mapper.PurchaseIntentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 购买意向服务（排队与状态机核心，迭代 3/4 实现）
 *
 * @author 高佳豪
 * @date 2026-10-08
 */
@Service
@RequiredArgsConstructor
public class PurchaseIntentService {

    private final PurchaseIntentMapper purchaseIntentMapper;

    // 迭代 3 起实现：提交意向、口令码生成、排队位次、状态机流转等
}
