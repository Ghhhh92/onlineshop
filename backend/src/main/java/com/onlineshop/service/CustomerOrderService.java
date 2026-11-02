package com.onlineshop.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlineshop.common.Pages;
import com.onlineshop.dto.OrderInfo;
import com.onlineshop.dto.PageResult;
import com.onlineshop.entity.Product;
import com.onlineshop.entity.PurchaseIntent;
import com.onlineshop.mapper.ProductMapper;
import com.onlineshop.mapper.PurchaseIntentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 客户的下单记录：客户登录后提交的意向，每条带商品信息和交易结果（成功、失败的都保留）。
 * 卖家查看某个客户的购买历史（US-38）用它；客户查看自己的历史下单（US-37）也可以直接用。
 *
 * @author 李帆
 * @date 2026-11-02
 */
@Service
@RequiredArgsConstructor
public class CustomerOrderService {

    private final PurchaseIntentMapper intentMapper;
    private final ProductMapper productMapper;

    /**
     * 某个客户的下单记录，按下单时间倒序分页。这一页涉及的商品用一条 SQL 查出来。
     */
    public PageResult<OrderInfo> orders(Long customerId, int page, int size) {
        Page<PurchaseIntent> result = intentMapper.selectCustomerPage(Pages.of(page, size), customerId);
        List<Long> productIds = result.getRecords().stream().map(PurchaseIntent::getProductId).distinct().toList();
        Map<Long, Product> products = productIds.isEmpty() ? Map.of()
                : productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        List<OrderInfo> records = result.getRecords().stream()
                .map(intent -> OrderInfo.of(intent, products.get(intent.getProductId())))
                .toList();
        return new PageResult<>(records, result.getTotal());
    }
}
