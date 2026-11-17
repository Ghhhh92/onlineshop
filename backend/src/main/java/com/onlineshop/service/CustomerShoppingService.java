package com.onlineshop.service;

import com.onlineshop.common.BizException;
import com.onlineshop.common.Pages;
import com.onlineshop.dto.FavoritePage;
import com.onlineshop.dto.ShoppingItem;
import com.onlineshop.dto.OrderDetail;
import com.onlineshop.dto.OrderLine;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Collections;

/**
 * 收藏、购物车业务（22637～22640）。写操作锁定客户行，跨实例串行处理同一客户的
 * 重复加购/收藏及修改删除，配合唯一索引保证去重且不丢失数量更新。
 * 不在加购时预占库存，实际结算由订单模块校验库存。
 */
@Service
@RequiredArgsConstructor
public class CustomerShoppingService {
    private final JdbcTemplate jdbc;
    private final OrderService orders;

    private static final RowMapper<ShoppingItem> ITEM = (rs, index) -> {
        String status = rs.getString("status");
        Integer stock = (Integer) rs.getObject("stock");
        boolean available = "ON_SALE".equals(status) && stock != null && stock > 0;
        return new ShoppingItem(rs.getLong("id"), rs.getLong("product_id"),
                rs.getString("name"), rs.getBigDecimal("price"), rs.getString("image"),
                rs.getInt("quantity"), stock, status, "DELISTED".equals(status) || status == null,
                available, rs.getTimestamp("create_time").toLocalDateTime());
    };

    public List<ShoppingItem> cart(Long customerId) {
        return jdbc.query("""
                SELECT c.id, c.product_id, c.quantity, c.create_time,
                       p.name, p.price, p.image, p.stock, p.status
                FROM customer_cart c LEFT JOIN product p ON p.id = c.product_id
                WHERE c.customer_id = ? ORDER BY c.create_time DESC, c.id DESC
                """, ITEM, customerId);
    }

    @Transactional(readOnly = true)
    public FavoritePage favorites(Long customerId, int page, int size) {
        int actualPage = Math.max(1, page);
        int actualSize = Math.min(Pages.MAX_SIZE, Math.max(1, size));
        long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM customer_favorite WHERE customer_id = ?", Long.class, customerId);
        List<ShoppingItem> items = jdbc.query("""
                SELECT f.id, f.product_id, 1 AS quantity, f.create_time,
                       p.name, p.price, p.image, p.stock, p.status
                FROM customer_favorite f LEFT JOIN product p ON p.id = f.product_id
                WHERE f.customer_id = ? ORDER BY f.create_time DESC, f.id DESC LIMIT ? OFFSET ?
                """, ITEM, customerId, actualSize, (long) (actualPage - 1) * actualSize);
        return new FavoritePage(items, total, actualPage, actualSize);
    }

    @Transactional
    public void favorite(Long customerId, Long productId) {
        lockCustomer(customerId);
        requireProduct(productId);
        Long count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM customer_favorite WHERE customer_id = ? AND product_id = ?
                """, Long.class, customerId, productId);
        if (count == 0) {
            jdbc.update("INSERT INTO customer_favorite(customer_id, product_id, create_time) VALUES (?, ?, ?)",
                    customerId, productId, now());
        }
    }

    @Transactional
    public void unfavorite(Long customerId, Long productId) {
        lockCustomer(customerId);
        jdbc.update("DELETE FROM customer_favorite WHERE customer_id = ? AND product_id = ?", customerId, productId);
    }

    @Transactional
    public void addCart(Long customerId, Long productId, int quantity) {
        checkQuantity(quantity, false);
        lockCustomer(customerId);
        requireProduct(productId);
        List<Integer> quantities = jdbc.queryForList(
                "SELECT quantity FROM customer_cart WHERE customer_id = ? AND product_id = ?",
                Integer.class, customerId, productId);
        int sum = quantity + (quantities.isEmpty() ? 0 : quantities.get(0));
        checkQuantity(sum, false);
        LocalDateTime time = now();
        if (quantities.isEmpty()) {
            jdbc.update("""
                    INSERT INTO customer_cart(customer_id, product_id, quantity, create_time, update_time)
                    VALUES (?, ?, ?, ?, ?)
                    """, customerId, productId, quantity, time, time);
        } else {
            jdbc.update("UPDATE customer_cart SET quantity = ?, update_time = ? WHERE customer_id = ? AND product_id = ?",
                    sum, time, customerId, productId);
        }
    }

    @Transactional
    public void updateCart(Long customerId, Long id, int quantity) {
        checkQuantity(quantity, true);
        lockCustomer(customerId);
        int changed = quantity == 0
                ? jdbc.update("DELETE FROM customer_cart WHERE id = ? AND customer_id = ?", id, customerId)
                : jdbc.update("UPDATE customer_cart SET quantity = ?, update_time = ? WHERE id = ? AND customer_id = ?",
                        quantity, now(), id, customerId);
        if (changed == 0) throw new BizException("购物车条目不存在");
    }

    @Transactional
    public void removeCart(Long customerId, Long id) {
        lockCustomer(customerId);
        if (jdbc.update("DELETE FROM customer_cart WHERE id = ? AND customer_id = ?", id, customerId) == 0) {
            throw new BizException("购物车条目不存在");
        }
    }

    /** 订单与选中条目清理共用一个事务；重复提交不会再次创建订单。 */
    @Transactional
    public OrderDetail checkout(Long customerId, List<Long> cartItemIds) {
        lockCustomer(customerId);
        List<SelectedItem> selected = selectedItems(customerId, cartItemIds);
        OrderDetail order = orders.place(customerId, selected.stream()
                .map(item -> new OrderLine(item.productId(), item.quantity())).toList());
        deleteSelected(customerId, selected);
        return order;
    }

    /** 先验证整个选择集，再转收藏；任意条目失效时全部回滚。 */
    @Transactional
    public void toFavorite(Long customerId, List<Long> cartItemIds) {
        lockCustomer(customerId);
        List<SelectedItem> selected = selectedItems(customerId, cartItemIds);
        for (SelectedItem item : selected) {
            favorite(customerId, item.productId());
        }
        deleteSelected(customerId, selected);
    }

    private record SelectedItem(Long id, Long productId, int quantity) {}

    private List<SelectedItem> selectedItems(Long customerId, List<Long> ids) {
        if (ids == null || ids.isEmpty() || ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new BizException("请选择有效的购物车条目");
        }
        // 重复 id 只结算/转收藏一次。只查询本人的条目，不泄露其他客户的数据。
        var distinct = new LinkedHashSet<>(ids);
        String placeholders = String.join(",", Collections.nCopies(distinct.size(), "?"));
        var args = new java.util.ArrayList<Object>();
        args.add(customerId);
        args.addAll(distinct);
        List<SelectedItem> selected = jdbc.query(
                "SELECT id, product_id, quantity FROM customer_cart WHERE customer_id = ? AND id IN ("
                        + placeholders + ") ORDER BY id FOR UPDATE",
                (rs, index) -> new SelectedItem(rs.getLong("id"), rs.getLong("product_id"), rs.getInt("quantity")),
                args.toArray());
        if (selected.size() != distinct.size()) throw new BizException("购物车条目不存在，请刷新后重试");
        return selected;
    }

    private void deleteSelected(Long customerId, List<SelectedItem> selected) {
        for (SelectedItem item : selected) {
            if (jdbc.update("DELETE FROM customer_cart WHERE customer_id = ? AND id = ?", customerId, item.id()) != 1) {
                throw new BizException("购物车已变更，请刷新后重试");
            }
        }
    }

    private void lockCustomer(Long customerId) {
        if (customerId == null || jdbc.queryForList("SELECT id FROM customer WHERE id = ? FOR UPDATE",
                Long.class, customerId).isEmpty()) throw new BizException(401, "请先登录");
    }

    private void requireProduct(Long productId) {
        if (productId == null || jdbc.queryForObject("SELECT COUNT(*) FROM product WHERE id = ?",
                Long.class, productId) == 0) throw new BizException("商品不存在");
    }

    private static void checkQuantity(int quantity, boolean allowZero) {
        if (quantity < (allowZero ? 0 : 1) || quantity > 999) throw new BizException("商品数量超出范围，上限为999");
    }

    private static LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
