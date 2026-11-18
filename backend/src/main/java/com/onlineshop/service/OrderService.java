package com.onlineshop.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlineshop.common.BizException;
import com.onlineshop.common.Pages;
import com.onlineshop.dto.OrderDetail;
import com.onlineshop.dto.OrderLine;
import com.onlineshop.dto.PageResult;
import com.onlineshop.entity.Customer;
import com.onlineshop.entity.CustomerOrder;
import com.onlineshop.entity.CustomerOrderItem;
import com.onlineshop.entity.Product;
import com.onlineshop.enums.OrderStatus;
import com.onlineshop.enums.ProductStatus;
import com.onlineshop.mapper.CustomerMapper;
import com.onlineshop.mapper.CustomerOrderItemMapper;
import com.onlineshop.mapper.CustomerOrderMapper;
import com.onlineshop.mapper.ProductMapper;
import com.onlineshop.statemachine.OrderStateMachine;
import com.onlineshop.statemachine.ProductStateMachine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订单服务（挑战需求 B）：下单、商家推进、客户/商家取消、订单查询。
 * <p>
 * 状态推进全部经过 {@link OrderStateMachine} 校验；库存在「交易完成」时按明细数量原子扣减，
 * 「开始发货」只校验「已发货量 + 本单数量 ≤ 当前库存」，不扣库存。
 *
 * @author 高佳豪
 * @date 2026-11-17
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    /** 单件商品一次下单的数量上限，与购物车一致 */
    private static final int MAX_QUANTITY = 999;

    private static final DateTimeFormatter ORDER_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final CustomerOrderMapper orderMapper;
    private final CustomerOrderItemMapper itemMapper;
    private final CustomerMapper customerMapper;
    private final ProductMapper productMapper;
    private final OrderStateMachine orderStateMachine;
    private final ProductStateMachine productStateMachine;

    private final SecureRandom random = new SecureRandom();

    // ===== 下单 =====

    /**
     * 下单：逐商品校验「在售 + 库存不少于数量」，任一不满足整单拦截。
     * 下单只校验不扣库存，订单进入「客户下单」。
     *
     * @param customerId 下单客户
     * @param lines      商品与数量，同商品重复出现时合并
     */
    @Transactional
    public OrderDetail place(Long customerId, List<OrderLine> lines) {
        List<CustomerOrderItem> items = buildItems(lines);
        BigDecimal total = items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2);

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        CustomerOrder order = new CustomerOrder();
        order.setOrderNo(nextOrderNo(now));
        order.setCustomerId(customerId);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(total);
        order.setCreateTime(now);
        order.setUpdateTime(now);
        orderMapper.insert(order);

        for (CustomerOrderItem item : items) {
            item.setOrderId(order.getId());
            itemMapper.insert(item);
        }
        return OrderDetail.of(order, items);
    }

    /**
     * 校验商品并生成明细，同商品合并数量。商品名与单价在下单时固定下来作为快照。
     */
    private List<CustomerOrderItem> buildItems(List<OrderLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new BizException("请选择要购买的商品");
        }
        Map<Long, Integer> merged = new LinkedHashMap<>();
        for (OrderLine line : lines) {
            if (line == null || line.productId() == null) {
                throw new BizException("商品不能为空");
            }
            if (line.quantity() < 1 || line.quantity() > MAX_QUANTITY) {
                throw new BizException("每件商品的数量需在 1～" + MAX_QUANTITY + " 之间");
            }
            merged.merge(line.productId(), line.quantity(), Integer::sum);
        }

        List<CustomerOrderItem> items = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : merged.entrySet()) {
            Product product = productMapper.selectById(entry.getKey());
            if (product == null) {
                throw new BizException("商品不存在");
            }
            if (product.getStatus() != ProductStatus.ON_SALE) {
                throw new BizException("商品「" + product.getName() + "」已下架，不能下单");
            }
            if (product.getStock() == null || product.getStock() < entry.getValue()) {
                throw new BizException("商品「" + product.getName() + "」库存不足");
            }
            CustomerOrderItem item = new CustomerOrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setPrice(product.getPrice());
            item.setQuantity(entry.getValue());
            items.add(item);
        }
        return items;
    }

    // ===== 商家推进状态 =====

    /**
     * 商家确认：客户下单 → 商家确认
     */
    @Transactional
    public OrderDetail confirm(Long orderId) {
        CustomerOrder order = getForUpdate(orderId);
        orderStateMachine.confirm(order);
        orderMapper.updateById(order);
        return detailOf(order);
    }

    /**
     * 备货完成：商家确认 → 备货完成
     */
    @Transactional
    public OrderDetail prepare(Long orderId) {
        CustomerOrder order = getForUpdate(orderId);
        orderStateMachine.prepare(order);
        orderMapper.updateById(order);
        return detailOf(order);
    }

    /**
     * 开始发货：备货完成 → 开始发货。
     * 逐商品校验「本商品已发货量 + 本单数量 ≤ 当前库存」，不满足则拒绝发货，订单停在备货完成。
     */
    @Transactional
    public OrderDetail ship(Long orderId) {
        CustomerOrder order = getForUpdate(orderId);
        if (order.getStatus() != OrderStatus.PREPARED) {
            throw new BizException("只有备货完成的订单可以发货");
        }
        List<CustomerOrderItem> items = itemMapper.selectByOrderId(orderId);
        for (CustomerOrderItem item : items) {
            Product product = productMapper.selectById(item.getProductId());
            if (product == null) {
                throw new BizException("商品「" + item.getProductName() + "」不存在，无法发货");
            }
            int shipped = itemMapper.shippedQuantity(item.getProductId());
            int available = product.getStock() == null ? 0 : product.getStock();
            if (shipped + item.getQuantity() > available) {
                throw new BizException("商品「" + item.getProductName() + "」库存不足，无法发货");
            }
        }
        orderStateMachine.ship(order);
        orderMapper.updateById(order);
        return detailOf(order);
    }

    /**
     * 交易完成：开始发货 → 交易完成。
     * 逐商品原子扣减库存，扣到 0 的商品经商品状态机转为售罄；任一商品库存不足则整单拦截。
     */
    @Transactional
    public OrderDetail complete(Long orderId) {
        CustomerOrder order = getForUpdate(orderId);
        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new BizException("只有开始发货的订单可以标记交易完成");
        }
        List<CustomerOrderItem> items = itemMapper.selectByOrderId(orderId);
        for (CustomerOrderItem item : items) {
            if (!productMapper.decreaseStock(item.getProductId(), item.getQuantity())) {
                throw new BizException("商品「" + item.getProductName() + "」库存不足，无法完成交易");
            }
        }
        for (CustomerOrderItem item : items) {
            Product product = productMapper.selectById(item.getProductId());
            if (product != null && product.getStatus() == ProductStatus.ON_SALE
                    && product.getStock() != null && product.getStock() == 0) {
                productStateMachine.sellOut(product);
                productMapper.updateById(product);
            }
        }
        orderStateMachine.complete(order);
        orderMapper.updateById(order);
        return detailOf(order);
    }

    // ===== 取消 =====

    /**
     * 客户取消：只能取消自己的订单，且只能在「开始发货」之前
     */
    @Transactional
    public OrderDetail cancelByCustomer(Long customerId, Long orderId) {
        CustomerOrder order = getForUpdate(orderId);
        if (!order.getCustomerId().equals(customerId)) {
            throw new BizException(404, "订单不存在");
        }
        orderStateMachine.cancel(order, true);
        orderMapper.updateById(order);
        return detailOf(order);
    }

    /**
     * 商家取消：交易完成之前都可以取消，含已发货
     */
    @Transactional
    public OrderDetail cancelBySeller(Long orderId) {
        CustomerOrder order = getForUpdate(orderId);
        orderStateMachine.cancel(order, false);
        orderMapper.updateById(order);
        return detailOf(order);
    }

    // ===== 查询 =====

    /**
     * 客户自己的订单，按下单时间倒序分页
     */
    public PageResult<OrderDetail> customerOrders(Long customerId, int page, int size, OrderStatus status) {
        return toPageResult(orderMapper.selectCustomerPage(Pages.of(page, size), customerId, status), false);
    }

    /**
     * 卖家看到的全部订单（单卖家），按下单时间倒序分页，带下单客户信息
     */
    public PageResult<OrderDetail> sellerOrders(int page, int size, OrderStatus status) {
        return toPageResult(orderMapper.selectSellerPage(Pages.of(page, size), status), true);
    }

    /**
     * 客户查看自己的订单详情，别人的订单按不存在处理
     */
    public OrderDetail customerOrderDetail(Long customerId, Long orderId) {
        CustomerOrder order = orderMapper.selectById(orderId);
        if (order == null || !order.getCustomerId().equals(customerId)) {
            throw new BizException(404, "订单不存在");
        }
        return detailOf(order);
    }

    /**
     * 卖家查看订单详情，带下单客户信息
     */
    public OrderDetail sellerOrderDetail(Long orderId) {
        CustomerOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(404, "订单不存在");
        }
        Customer customer = customerMapper.selectById(order.getCustomerId());
        return OrderDetail.of(order, itemMapper.selectByOrderId(order.getId()),
                customer == null ? null : customer.getUsername(),
                customer == null ? null : customer.getPhone());
    }

    // ===== 内部方法 =====

    private CustomerOrder getForUpdate(Long orderId) {
        CustomerOrder order = orderMapper.selectByIdForUpdate(orderId);
        if (order == null) {
            throw new BizException(404, "订单不存在");
        }
        return order;
    }

    private OrderDetail detailOf(CustomerOrder order) {
        return OrderDetail.of(order, itemMapper.selectByOrderId(order.getId()));
    }

    /**
     * 一页订单的明细一次查出来再按订单分组，避免逐条查询；
     * 卖家视角再批量查一次客户，填充下单人信息。
     */
    private PageResult<OrderDetail> toPageResult(Page<CustomerOrder> result, boolean withCustomer) {
        List<Long> orderIds = result.getRecords().stream().map(CustomerOrder::getId).toList();
        Map<Long, List<CustomerOrderItem>> itemsByOrder = itemMapper.selectByOrderIds(orderIds).stream()
                .collect(Collectors.groupingBy(CustomerOrderItem::getOrderId, LinkedHashMap::new,
                        Collectors.toCollection(ArrayList::new)));
        Map<Long, Customer> customers = withCustomer ? customersOf(result.getRecords()) : Map.of();
        List<OrderDetail> details = result.getRecords().stream()
                .map(order -> {
                    Customer customer = customers.get(order.getCustomerId());
                    return OrderDetail.of(order, itemsByOrder.getOrDefault(order.getId(), List.of()),
                            customer == null ? null : customer.getUsername(),
                            customer == null ? null : customer.getPhone());
                })
                .toList();
        return new PageResult<>(details, result.getTotal());
    }

    /**
     * 这一页订单的下单客户，一次查完
     */
    private Map<Long, Customer> customersOf(List<CustomerOrder> orders) {
        List<Long> customerIds = orders.stream().map(CustomerOrder::getCustomerId).distinct().toList();
        if (customerIds.isEmpty()) {
            return Map.of();
        }
        return customerMapper.selectBatchIds(customerIds).stream()
                .collect(Collectors.toMap(Customer::getId, Function.identity()));
    }

    /**
     * 订单号：下单时间到秒 + 4 位随机数字，唯一性由数据库唯一索引兜底
     */
    private String nextOrderNo(LocalDateTime now) {
        return now.format(ORDER_NO_TIME) + String.format("%04d", random.nextInt(10000));
    }
}
