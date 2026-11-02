package com.onlineshop.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlineshop.common.BizException;
import com.onlineshop.common.Pages;
import com.onlineshop.dto.IntentInfo;
import com.onlineshop.dto.PageResult;
import com.onlineshop.dto.ProductInfo;
import com.onlineshop.entity.Product;
import com.onlineshop.entity.PurchaseIntent;
import com.onlineshop.enums.ProductStatus;
import com.onlineshop.mapper.ProductMapper;
import com.onlineshop.mapper.PurchaseIntentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 卖家后台查询：商品列表和状态（US-15、US-31）、意向购买人（US-16）、历史商品（US-21）。
 *
 * @author 李帆
 * @date 2026-10-19
 */
@Service
@RequiredArgsConstructor
public class SellerProductQueryService {

    private final ProductMapper productMapper;
    private final PurchaseIntentMapper intentMapper;
    private final ProductInfoAssembler productInfoAssembler;

    /**
     * 当前商品：最新发布的在售或已冻结（交易中）商品，没有时返回 null
     */
    public ProductInfo currentProduct() {
        Product product = productMapper.selectCurrent();
        return product == null ? null : productInfoAssembler.toInfo(product);
    }

    /**
     * 全部商品，按发布时间倒序分页；status、categoryId 不为空时按状态、类别筛选
     */
    public PageResult<ProductInfo> list(ProductStatus status, Long categoryId, int page, int size) {
        Page<Product> result = productMapper.selectSellerPage(Pages.of(page, size), status, categoryId);
        return new PageResult<>(productInfoAssembler.toInfos(result.getRecords()), result.getTotal());
    }

    /**
     * 商品详情（任何状态），不存在时返回 404
     */
    public ProductInfo detail(Long id) {
        return productInfoAssembler.toInfo(getOrThrow(id));
    }

    /**
     * 某件商品的意向购买人，按先到先得排列。
     * 仍在队列中的意向带当前位次（前面有人撤销或作废时自动前移），已结束的位次为 null。
     */
    public List<IntentInfo> intents(Long productId) {
        getOrThrow(productId);
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
        Page<Product> result = productMapper.selectHistoryPage(Pages.of(page, size));
        return new PageResult<>(productInfoAssembler.toInfos(result.getRecords()), result.getTotal());
    }

    private Product getOrThrow(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BizException(404, "商品不存在");
        }
        return product;
    }
}
