package com.onlineshop.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlineshop.common.BizException;
import com.onlineshop.dto.IntentInfo;
import com.onlineshop.dto.PageResult;
import com.onlineshop.dto.ProductInfo;
import com.onlineshop.entity.Product;
import com.onlineshop.entity.PurchaseIntent;
import com.onlineshop.mapper.ProductMapper;
import com.onlineshop.mapper.PurchaseIntentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 卖家后台查询：当前商品状态（US-15）、意向购买人（US-16）、历史商品（US-21）。
 *
 * @author 李帆
 * @date 2026-10-19
 */
@Service
@RequiredArgsConstructor
public class SellerProductQueryService {

    /** 历史商品每页最多条数 */
    static final int MAX_PAGE_SIZE = 50;

    private final ProductMapper productMapper;
    private final PurchaseIntentMapper intentMapper;

    /**
     * 当前商品：在售或已冻结（交易中），没有时返回 null
     */
    public ProductInfo currentProduct() {
        Product product = productMapper.selectCurrent();
        return product == null ? null : ProductInfo.from(product);
    }

    /**
     * 某件商品的意向购买人，按先到先得排列。
     * 仍在队列中的意向带当前位次（前面有人撤销或作废时自动前移），已结束的位次为 null。
     */
    public List<IntentInfo> intents(Long productId) {
        if (productMapper.selectById(productId) == null) {
            throw new BizException(404, "商品不存在");
        }
        List<IntentInfo> result = new ArrayList<>();
        int position = 0;
        for (PurchaseIntent intent : intentMapper.selectByProductInOrder(productId)) {
            boolean active = PurchaseIntentMapper.ACTIVE_STATUSES.contains(intent.getStatus());
            result.add(IntentInfo.of(intent, active ? ++position : null));
        }
        return result;
    }

    /**
     * 历史商品（已下架），按成交时间倒序分页。页码从 1 开始；超出范围的页码、条数按边界处理。
     */
    public PageResult<ProductInfo> history(int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Page<Product> result = productMapper.selectHistoryPage(safePage, safeSize);
        return new PageResult<>(result.getRecords().stream().map(ProductInfo::from).toList(), result.getTotal());
    }
}
