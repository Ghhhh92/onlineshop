package com.onlineshop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlineshop.common.BizException;
import com.onlineshop.common.Pages;
import com.onlineshop.dto.OrderDetail;
import com.onlineshop.dto.OrderLine;
import com.onlineshop.dto.PageResult;
import com.onlineshop.dto.ShoppingItem;
import com.onlineshop.entity.CustomerCart;
import com.onlineshop.entity.CustomerFavorite;
import com.onlineshop.entity.Product;
import com.onlineshop.enums.ProductStatus;
import com.onlineshop.mapper.CustomerCartMapper;
import com.onlineshop.mapper.CustomerFavoriteMapper;
import com.onlineshop.mapper.CustomerMapper;
import com.onlineshop.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 收藏、购物车业务（22637～22640）。
 * <p>
 * 写操作先锁定客户行，同一客户的并发加购、收藏、修改、删除按顺序执行，配合唯一索引保证去重；
 * 加购不预占库存，实际结算由订单模块校验。
 *
 * @author 范与恒
 * @date 2026-11-17
 */
@Service
@RequiredArgsConstructor
public class CustomerShoppingService {

    private static final int MAX_QUANTITY = 999;

    private final CustomerCartMapper cartMapper;
    private final CustomerFavoriteMapper favoriteMapper;
    private final CustomerMapper customerMapper;
    private final ProductMapper productMapper;
    private final OrderService orders;

    /**
     * 购物车列表，按加入时间倒序。商品信息实时读取，商品不可购买或已不存在时条目仍保留，供客户移除。
     */
    @Transactional(readOnly = true)
    public List<ShoppingItem> cart(Long customerId) {
        List<CustomerCart> carts = cartMapper.selectByCustomer(customerId);
        Map<Long, Product> products = productsOf(carts.stream().map(CustomerCart::getProductId).toList());
        return carts.stream()
                .map(cart -> toItem(cart.getId(), cart.getProductId(), cart.getQuantity(),
                        cart.getCreateTime(), products.get(cart.getProductId())))
                .toList();
    }

    /**
     * 收藏列表，按收藏时间倒序分页
     */
    @Transactional(readOnly = true)
    public PageResult<ShoppingItem> favorites(Long customerId, int page, int size) {
        Page<CustomerFavorite> result = favoriteMapper.selectCustomerPage(Pages.of(page, size), customerId);
        Map<Long, Product> products = productsOf(
                result.getRecords().stream().map(CustomerFavorite::getProductId).toList());
        List<ShoppingItem> items = result.getRecords().stream()
                .map(favorite -> toItem(favorite.getId(), favorite.getProductId(), 1,
                        favorite.getCreateTime(), products.get(favorite.getProductId())))
                .toList();
        return new PageResult<>(items, result.getTotal());
    }

    /**
     * 收藏商品，已收藏时直接返回，不重复插入
     */
    @Transactional
    public void favorite(Long customerId, Long productId) {
        lockCustomer(customerId);
        requireProduct(productId);
        if (!favoriteMapper.exists(customerId, productId)) {
            favoriteMapper.insert(favoriteOf(customerId, productId, now()));
        }
    }

    /**
     * 取消收藏，没收藏过也返回成功
     */
    @Transactional
    public void unfavorite(Long customerId, Long productId) {
        lockCustomer(customerId);
        favoriteMapper.delete(Wrappers.<CustomerFavorite>lambdaQuery()
                .eq(CustomerFavorite::getCustomerId, customerId)
                .eq(CustomerFavorite::getProductId, productId));
    }

    /**
     * 加入购物车：同商品已在车中时累加数量，不新增条目。加购时不校验库存，结算时才校验。
     */
    @Transactional
    public void addCart(Long customerId, Long productId, int quantity) {
        checkQuantity(quantity, false);
        lockCustomer(customerId);
        requireProduct(productId);
        Integer existing = cartMapper.selectQuantity(customerId, productId);
        LocalDateTime time = now();
        if (existing == null) {
            CustomerCart cart = new CustomerCart();
            cart.setCustomerId(customerId);
            cart.setProductId(productId);
            cart.setQuantity(quantity);
            cart.setCreateTime(time);
            cart.setUpdateTime(time);
            cartMapper.insert(cart);
        } else {
            int sum = existing + quantity;
            checkQuantity(sum, false);
            CustomerCart cart = cartMapper.selectOne(Wrappers.<CustomerCart>lambdaQuery()
                    .eq(CustomerCart::getCustomerId, customerId)
                    .eq(CustomerCart::getProductId, productId));
            cart.setQuantity(sum);
            cart.setUpdateTime(time);
            cartMapper.updateById(cart);
        }
    }

    /**
     * 修改数量：传 0 等同移出购物车，1～999 直接改
     */
    @Transactional
    public void updateCart(Long customerId, Long id, int quantity) {
        checkQuantity(quantity, true);
        lockCustomer(customerId);
        int changed;
        if (quantity == 0) {
            changed = cartMapper.delete(wrapperOf(customerId, id));
        } else {
            CustomerCart cart = cartMapper.selectOne(wrapperOf(customerId, id));
            if (cart == null) {
                changed = 0;
            } else {
                cart.setQuantity(quantity);
                cart.setUpdateTime(now());
                changed = cartMapper.updateById(cart);
            }
        }
        if (changed == 0) {
            throw new BizException("购物车条目不存在");
        }
    }

    /**
     * 移出购物车
     */
    @Transactional
    public void removeCart(Long customerId, Long id) {
        lockCustomer(customerId);
        if (cartMapper.delete(wrapperOf(customerId, id)) == 0) {
            throw new BizException("购物车条目不存在");
        }
    }

    /**
     * 购物车多选下单：与删除已结算条目共用一个事务；订单生成失败时购物车不变。
     */
    @Transactional
    public OrderDetail checkout(Long customerId, List<Long> cartItemIds) {
        lockCustomer(customerId);
        List<CustomerCart> selected = selectedItems(customerId, cartItemIds);
        OrderDetail order = orders.place(customerId, selected.stream()
                .map(cart -> new OrderLine(cart.getProductId(), cart.getQuantity()))
                .toList());
        deleteSelected(customerId, selected);
        return order;
    }

    /**
     * 购物车多选转收藏：已在收藏中的跳过，转换后从购物车移除；任一条目失效时整体回滚。
     */
    @Transactional
    public void toFavorite(Long customerId, List<Long> cartItemIds) {
        lockCustomer(customerId);
        List<CustomerCart> selected = selectedItems(customerId, cartItemIds);
        List<Long> productIds = selected.stream().map(CustomerCart::getProductId).distinct().toList();
        requireProducts(productIds);
        Set<Long> favorited = favoriteMapper.selectFavoritedProductIds(customerId, productIds);
        LocalDateTime time = now();
        for (Long productId : productIds) {
            if (!favorited.contains(productId)) {
                favoriteMapper.insert(favoriteOf(customerId, productId, time));
            }
        }
        deleteSelected(customerId, selected);
    }

    // ===== 内部方法 =====

    /**
     * 锁住客户行；客户不存在按未登录处理
     */
    private void lockCustomer(Long customerId) {
        if (customerId == null || customerMapper.selectByIdForUpdate(customerId) == null) {
            throw new BizException(401, "请先登录");
        }
    }

    private void requireProduct(Long productId) {
        if (productId == null || productMapper.selectById(productId) == null) {
            throw new BizException("商品不存在");
        }
    }

    private void requireProducts(List<Long> productIds) {
        if (productIds.isEmpty()) {
            throw new BizException("商品不存在");
        }
        if (productMapper.selectBatchIds(productIds).size() != productIds.size()) {
            throw new BizException("商品不存在");
        }
    }

    /**
     * 校验选中条目都在本人购物车里，重复 id 只处理一次
     */
    private List<CustomerCart> selectedItems(Long customerId, List<Long> cartItemIds) {
        if (cartItemIds == null || cartItemIds.isEmpty()
                || cartItemIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new BizException("请选择有效的购物车条目");
        }
        Set<Long> distinct = new LinkedHashSet<>(cartItemIds);
        List<CustomerCart> selected = cartMapper.selectByIdsForUpdate(customerId, distinct);
        if (selected.size() != distinct.size()) {
            throw new BizException("购物车条目不存在，请刷新后重试");
        }
        return selected;
    }

    private void deleteSelected(Long customerId, List<CustomerCart> selected) {
        int deleted = cartMapper.deleteByIds(customerId,
                selected.stream().map(CustomerCart::getId).toList());
        if (deleted != selected.size()) {
            throw new BizException("购物车已变更，请刷新后重试");
        }
    }

    /**
     * 一次查出这批商品，组装列表时不用逐条查询
     */
    private Map<Long, Product> productsOf(List<Long> productIds) {
        List<Long> distinct = productIds.stream().distinct().toList();
        if (distinct.isEmpty()) {
            return Map.of();
        }
        return productMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private static ShoppingItem toItem(Long id, Long productId, int quantity,
                                       LocalDateTime createTime, Product product) {
        String status = product == null ? null : product.getStatus().name();
        Integer stock = product == null ? null : product.getStock();
        boolean delisted = product == null || product.getStatus() == ProductStatus.DELISTED;
        boolean available = product != null && product.getStatus() == ProductStatus.ON_SALE
                && stock != null && stock > 0;
        return new ShoppingItem(id, productId,
                product == null ? null : product.getName(),
                product == null ? null : product.getPrice(),
                product == null ? null : product.getImage(),
                quantity, stock, status, delisted, available, createTime);
    }

    private static CustomerFavorite favoriteOf(Long customerId, Long productId, LocalDateTime time) {
        CustomerFavorite favorite = new CustomerFavorite();
        favorite.setCustomerId(customerId);
        favorite.setProductId(productId);
        favorite.setCreateTime(time);
        return favorite;
    }

    private static LambdaQueryWrapper<CustomerCart> wrapperOf(Long customerId, Long id) {
        return Wrappers.<CustomerCart>lambdaQuery()
                .eq(CustomerCart::getId, id)
                .eq(CustomerCart::getCustomerId, customerId);
    }

    private static void checkQuantity(int quantity, boolean allowZero) {
        if (quantity < (allowZero ? 0 : 1) || quantity > MAX_QUANTITY) {
            throw new BizException("商品数量超出范围，上限为" + MAX_QUANTITY);
        }
    }

    private static LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
