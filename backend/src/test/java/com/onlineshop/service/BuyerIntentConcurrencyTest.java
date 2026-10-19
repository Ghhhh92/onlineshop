package com.onlineshop.service;

import com.onlineshop.common.BizException;
import com.onlineshop.dto.IntentSubmitResult;
import com.onlineshop.dto.SubmitIntentRequest;
import com.onlineshop.entity.Product;
import com.onlineshop.entity.PurchaseIntent;
import com.onlineshop.enums.ProductStatus;
import com.onlineshop.mapper.ProductMapper;
import com.onlineshop.mapper.PurchaseIntentMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 先到先得（US-30）在并发下也成立：很多买家同时提交，入队序号不重复、不跳号，口令码各不相同；
 * 同一手机号同时提交多次，只有一次成功。
 * <p>
 * 这里不加 @Transactional：每个线程的提交都是真实的独立事务，靠 SELECT … FOR UPDATE 排队。
 *
 * @author 李帆
 * @date 2026-10-19
 */
@SpringBootTest
@ActiveProfiles("test")
class BuyerIntentConcurrencyTest {

    private static final int BUYERS = 20;

    @Autowired
    private BuyerIntentService buyerIntentService;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private PurchaseIntentMapper intentMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Product product;

    @BeforeEach
    void setUp() {
        clean();
        product = new Product();
        product.setName("并发测试商品");
        product.setPrice(new BigDecimal("1.00"));
        product.setStatus(ProductStatus.ON_SALE);
        product.setCreateTime(LocalDateTime.now());
        productMapper.insert(product);
    }

    @AfterEach
    void clean() {
        jdbcTemplate.update("DELETE FROM purchase_intent");
        jdbcTemplate.update("DELETE FROM product");
    }

    @Test
    @DisplayName("20 个买家同时提交：序号 1～20 各不相同，口令码各不相同")
    void concurrentBuyersGetDistinctPositions() throws Exception {
        List<Future<IntentSubmitResult>> futures = runConcurrently(BUYERS, i -> "139000000" + String.format("%02d", i));

        for (Future<IntentSubmitResult> future : futures) {
            assertThat(future.get()).isNotNull();
        }
        List<PurchaseIntent> saved = intentMapper.selectByProductInOrder(product.getId());
        assertThat(saved).extracting(PurchaseIntent::getPosition)
                .containsExactlyElementsOf(java.util.stream.IntStream.rangeClosed(1, BUYERS).boxed().toList());
        assertThat(saved).extracting(PurchaseIntent::getCode).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("同一手机号同时提交 5 次：只有 1 次成功")
    void samePhoneOnlyOnce() throws Exception {
        List<Future<IntentSubmitResult>> futures = runConcurrently(5, i -> "13900000099");

        int success = 0;
        for (Future<IntentSubmitResult> future : futures) {
            if (future.get() != null) {
                success++;
            }
        }
        assertThat(success).isEqualTo(1);
        assertThat(intentMapper.selectByProductInOrder(product.getId())).hasSize(1);
    }

    /** 所有线程准备好后同时开始提交；业务上被拒绝的返回 null */
    private List<Future<IntentSubmitResult>> runConcurrently(int count, java.util.function.IntFunction<String> phoneOf)
            throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(count);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<IntentSubmitResult>> futures = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            SubmitIntentRequest request = new SubmitIntentRequest();
            request.setName("买家" + i);
            request.setPhone(phoneOf.apply(i));
            Callable<IntentSubmitResult> task = () -> {
                start.await();
                try {
                    return buyerIntentService.submit(request);
                } catch (BizException e) {
                    return null;
                }
            };
            futures.add(pool.submit(task));
        }
        start.countDown();
        pool.shutdown();
        return futures;
    }
}
