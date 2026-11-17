package com.onlineshop.controller;

import com.onlineshop.auth.CustomerSession;
import com.onlineshop.service.CustomerShoppingService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerShoppingTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired CustomerShoppingService shopping;
    Long owner, other, product, second;
    MockHttpSession session, stranger;

    @BeforeEach void setup() {
        owner = customer(); other = customer();
        session = session(owner); stranger = session(other);
        String name = "shopping-" + UUID.randomUUID();
        jdbc.update("INSERT INTO product(name, price, stock, status, create_time) VALUES (?, 12.50, 10, 'ON_SALE', CURRENT_TIMESTAMP)", name);
        product = jdbc.queryForObject("SELECT id FROM product WHERE name = ?", Long.class, name);
    }
    Long customer() {
        String name = "shopping-" + UUID.randomUUID();
        jdbc.update("INSERT INTO customer(username,password,phone,default_location,create_time) VALUES (?, 'test', '13800000000', '校门', CURRENT_TIMESTAMP)", name);
        return jdbc.queryForObject("SELECT id FROM customer WHERE username = ?", Long.class, name);
    }
    MockHttpSession session(Long id) {
        MockHttpSession s = new MockHttpSession(); s.setAttribute(CustomerSession.CUSTOMER_ID,id); return s;
    }
    @AfterEach void cleanup() {
        for (Long id : List.of(owner,other)) {
            jdbc.update("DELETE FROM customer_cart WHERE customer_id = ?",id);
            jdbc.update("DELETE FROM customer_favorite WHERE customer_id = ?",id);
            jdbc.update("DELETE FROM customer_order_item WHERE order_id IN (SELECT id FROM customer_order WHERE customer_id = ?)",id);
            jdbc.update("DELETE FROM customer_order WHERE customer_id = ?",id);
            jdbc.update("DELETE FROM customer WHERE id = ?",id);
        }
        jdbc.update("DELETE FROM product WHERE id = ?",product);
        if (second != null) jdbc.update("DELETE FROM product WHERE id = ?",second);
    }
    String body(int quantity) { return "{\"productId\":"+product+",\"quantity\":"+quantity+"}"; }
    long cartId() { return jdbc.queryForObject("SELECT id FROM customer_cart WHERE customer_id = ?",Long.class,owner); }

    @Test void allEndpointsRequireCustomerLogin() throws Exception {
        for (var request : List.of(get("/api/customer/cart"),get("/api/customer/favorite"),
                post("/api/customer/cart").content(body(1)),post("/api/customer/favorite").content(body(1)),
                put("/api/customer/cart/1").content("{\"quantity\":1}"),
                delete("/api/customer/cart/1"),delete("/api/customer/favorite/1"))) {
            mvc.perform(request.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
        }
    }
    @Test void favoriteIsIdempotentAndUsesContractPagination() throws Exception {
        for(int i=0;i<2;i++) mvc.perform(post("/api/customer/favorite").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(body(1))).andExpect(status().isOk());
        mvc.perform(get("/api/customer/favorite?page=1&size=1").session(session))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].price").value(12.5));
        mvc.perform(get("/api/customer/favorite").session(stranger)).andExpect(jsonPath("$.data.total").value(0));
        mvc.perform(delete("/api/customer/favorite/"+product).session(stranger)).andExpect(status().isOk());
        assertThat(shopping.favorites(owner,1,10).total()).isEqualTo(1);
        mvc.perform(delete("/api/customer/favorite/"+product).session(session)).andExpect(status().isOk());
        mvc.perform(delete("/api/customer/favorite/"+product).session(session)).andExpect(status().isOk());
        assertThat(shopping.favorites(owner,1,10).total()).isZero();
    }
    @Test void favoritePaginationSortsNewestFirstAndClampsBounds() {
        shopping.favorite(owner,product);
        jdbc.update("UPDATE customer_favorite SET create_time = '2020-01-01 00:00:00' WHERE customer_id = ?",owner);
        jdbc.update("INSERT INTO customer_favorite(customer_id,product_id,create_time) VALUES (?,999999999,CURRENT_TIMESTAMP)",owner);
        var page=shopping.favorites(owner,1,1);
        assertThat(page.total()).isEqualTo(2); assertThat(page.records().get(0).productId()).isEqualTo(999999999L);
        assertThat(shopping.favorites(owner,2,1).records().get(0).productId()).isEqualTo(product);
        assertThat(shopping.favorites(owner,Integer.MAX_VALUE,50).records()).isEmpty();
        assertThat(shopping.favorites(owner,0,100).records()).hasSize(2);
    }
    @Test void cartDefaultsToOneAndAccumulates() throws Exception {
        mvc.perform(post("/api/customer/cart").session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":"+product+"}")).andExpect(status().isOk());
        mvc.perform(post("/api/customer/cart").session(session).contentType(MediaType.APPLICATION_JSON)
                .content(body(2))).andExpect(status().isOk());
        mvc.perform(get("/api/customer/cart").session(session)).andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].quantity").value(3)).andExpect(jsonPath("$.data[0].stock").value(10));
        mvc.perform(get("/api/customer/cart").session(stranger)).andExpect(jsonPath("$.data.length()").value(0));
    }
    @Test void cartUpdateZeroDeletesAndDeleteWorks() throws Exception {
        shopping.addCart(owner,product,1);
        mvc.perform(put("/api/customer/cart/"+cartId()).session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":999}")).andExpect(status().isOk());
        assertThat(shopping.cart(owner).get(0).quantity()).isEqualTo(999);
        mvc.perform(put("/api/customer/cart/"+cartId()).session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":0}")).andExpect(status().isOk());
        assertThat(shopping.cart(owner)).isEmpty();
        shopping.addCart(owner,product,1);
        mvc.perform(delete("/api/customer/cart/"+cartId()).session(session)).andExpect(status().isOk());
        assertThat(shopping.cart(owner)).isEmpty();
    }
    @Test void cannotMutateAnotherCustomersCart() throws Exception {
        shopping.addCart(owner,product,2);
        mvc.perform(put("/api/customer/cart/"+cartId()).session(stranger).contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":0}")).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/customer/cart/"+cartId()).session(stranger)).andExpect(status().isBadRequest());
        assertThat(shopping.cart(owner).get(0).quantity()).isEqualTo(2);
    }
    @Test void invalidQuantitiesAreRejectedAndOverflowPreservesCart() throws Exception {
        for(int n : new int[]{-1,0,1000}) mvc.perform(post("/api/customer/cart").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(body(n))).andExpect(status().isBadRequest());
        shopping.addCart(owner,product,999);
        mvc.perform(post("/api/customer/cart").session(session).contentType(MediaType.APPLICATION_JSON)
                .content(body(1))).andExpect(status().isBadRequest());
        for(String value : new String[]{"-1","1000","null"}) mvc.perform(put("/api/customer/cart/"+cartId()).session(session)
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":"+value+"}")).andExpect(status().isBadRequest());
        assertThat(shopping.cart(owner).get(0).quantity()).isEqualTo(999);
    }
    @Test void missingProductOrQuantityIsRejected() throws Exception {
        for(String path : List.of("cart","favorite")) {
            for(String body : List.of("{}","{\"productId\":-1}","{\"productId\":999999999}"))
                mvc.perform(post("/api/customer/"+path).session(session).contentType(MediaType.APPLICATION_JSON)
                        .content(body)).andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/customer/cart").session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":"+product+",\"quantity\":null}")).andExpect(status().isBadRequest());
    }
    @Test void productChangesAndRemovalRemainVisible() {
        shopping.addCart(owner,product,1); shopping.favorite(owner,product);
        jdbc.update("UPDATE product SET price=20, status='DELISTED',stock=0 WHERE id=?",product);
        var item=shopping.cart(owner).get(0);
        assertThat(item.delisted()).isTrue(); assertThat(item.available()).isFalse();
        assertThat(item.price()).isEqualByComparingTo("20");
        assertThat(shopping.favorites(owner,1,10).records().get(0).delisted()).isTrue();
        jdbc.update("DELETE FROM product WHERE id=?",product);
        assertThat(shopping.cart(owner).get(0).available()).isFalse();
        shopping.removeCart(owner,cartId()); shopping.unfavorite(owner,product);
    }
    @Test void concurrentAddsAndFavoritesDoNotLoseUpdatesOrDuplicate() throws Exception {
        var pool=Executors.newFixedThreadPool(8);
        try {
            var gate=new CountDownLatch(1); var jobs=new ArrayList<Future<?>>();
            for(int i=0;i<8;i++) jobs.add(pool.submit(() -> {
                try { gate.await(); } catch(InterruptedException e) { throw new RuntimeException(e); }
                for(int n=0;n<5;n++) { shopping.addCart(owner,product,1); shopping.favorite(owner,product); }
            }));
            gate.countDown(); for(var job:jobs) job.get(30,TimeUnit.SECONDS);
        } finally { pool.shutdownNow(); }
        assertThat(shopping.cart(owner)).hasSize(1);
        assertThat(shopping.cart(owner).get(0).quantity()).isEqualTo(40);
        assertThat(shopping.favorites(owner,1,10).total()).isEqualTo(1);
    }

    Long secondProduct() {
        String name="batch-"+UUID.randomUUID();
        jdbc.update("INSERT INTO product(name,price,stock,status,create_time) VALUES (?,5,10,'ON_SALE',CURRENT_TIMESTAMP)",name);
        second=jdbc.queryForObject("SELECT id FROM product WHERE name=?",Long.class,name);
        return second;
    }
    long cartId(Long customerId,Long productId) {
        return jdbc.queryForObject("SELECT id FROM customer_cart WHERE customer_id=? AND product_id=?",Long.class,customerId,productId);
    }
    long orderCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM customer_order WHERE customer_id=?",Long.class,owner);
    }
    String selection(long... ids) {
        return "{\"cartItemIds\":"+Arrays.toString(ids)+"}";
    }
    @Test void checkoutCreatesSingleOrderWithSnapshotsAndClearsOnlySelection() throws Exception {
        shopping.addCart(owner,product,2); shopping.addCart(owner,secondProduct(),3);
        long selected=cartId(owner,product);
        mvc.perform(post("/api/customer/cart/checkout").session(session).contentType(MediaType.APPLICATION_JSON)
                .content(selection(selected,selected)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.orderNo").isNotEmpty()).andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.totalAmount").value(25))
                .andExpect(jsonPath("$.data.items.length()").value(1)).andExpect(jsonPath("$.data.items[0].quantity").value(2));
        assertThat(orderCount()).isEqualTo(1);
        assertThat(shopping.cart(owner)).hasSize(1); assertThat(shopping.cart(owner).get(0).productId()).isEqualTo(second);
        assertThat(jdbc.queryForObject("SELECT stock FROM product WHERE id=?",Integer.class,product)).isEqualTo(10);
        mvc.perform(post("/api/customer/cart/checkout").session(session).contentType(MediaType.APPLICATION_JSON)
                .content(selection(selected))).andExpect(status().isBadRequest());
        assertThat(orderCount()).isEqualTo(1);
    }
    @Test void checkoutMultipleProductsUsesCurrentPrices() {
        shopping.addCart(owner,product,2); shopping.addCart(owner,secondProduct(),3);
        jdbc.update("UPDATE product SET price=15 WHERE id=?",product);
        var result=shopping.checkout(owner,shopping.cart(owner).stream().map(i -> i.id()).toList());
        assertThat(result.items()).hasSize(2); assertThat(result.totalAmount()).isEqualByComparingTo("45.00");
        assertThat(shopping.cart(owner)).isEmpty();
    }
    @Test void checkoutRejectsWholeSelectionForStockOrUnavailableProduct() throws Exception {
        shopping.addCart(owner,product,1); shopping.addCart(owner,secondProduct(),2);
        long first=cartId(owner,product),last=cartId(owner,second);
        jdbc.update("UPDATE product SET stock=1 WHERE id=?",second);
        mvc.perform(post("/api/customer/cart/checkout").session(session).contentType(MediaType.APPLICATION_JSON)
                .content(selection(first,last))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("库存不足")));
        assertThat(orderCount()).isZero(); assertThat(shopping.cart(owner)).hasSize(2);
        for(String state:List.of("FROZEN","SALES_OUT","DELISTED")) {
            jdbc.update("UPDATE product SET stock=10,status=? WHERE id=?",state,second);
            mvc.perform(post("/api/customer/cart/checkout").session(session).contentType(MediaType.APPLICATION_JSON)
                    .content(selection(first,last))).andExpect(status().isBadRequest());
            assertThat(orderCount()).isZero(); assertThat(shopping.cart(owner)).hasSize(2);
        }
    }
    @Test void batchEndpointsRejectInvalidAndForeignSelectionsWithoutPartialWrites() throws Exception {
        shopping.addCart(owner,product,1); shopping.addCart(other,product,1);
        long mine=cartId(owner,product), foreign=cartId(other,product);
        for(String endpoint:List.of("checkout","to-favorite")) {
            mvc.perform(post("/api/customer/cart/"+endpoint).contentType(MediaType.APPLICATION_JSON)
                    .content(selection(mine))).andExpect(status().isUnauthorized());
            for(String body:List.of("{}","{\"cartItemIds\":null}","{\"cartItemIds\":[]}",
                    "{\"cartItemIds\":[null]}","{\"cartItemIds\":[-1]}",selection(mine,foreign),selection(mine,999999999))) {
                mvc.perform(post("/api/customer/cart/"+endpoint).session(session).contentType(MediaType.APPLICATION_JSON)
                        .content(body)).andExpect(status().isBadRequest());
                assertThat(orderCount()).isZero(); assertThat(shopping.cart(owner)).hasSize(1);
                assertThat(shopping.cart(other)).hasSize(1); assertThat(shopping.favorites(owner,1,10).total()).isZero();
            }
        }
    }
    @Test void transferSkipsExistingFavoritesAndClearsOnlySelectedItems() throws Exception {
        shopping.addCart(owner,product,1); shopping.addCart(owner,secondProduct(),2);
        shopping.favorite(owner,product);
        long mine=cartId(owner,product);
        mvc.perform(post("/api/customer/cart/to-favorite").session(session).contentType(MediaType.APPLICATION_JSON)
                .content(selection(mine,mine))).andExpect(status().isOk());
        assertThat(shopping.favorites(owner,1,10).total()).isEqualTo(1);
        assertThat(shopping.cart(owner)).hasSize(1);
        shopping.addCart(owner,product,1);
        shopping.toFavorite(owner,shopping.cart(owner).stream().map(i -> i.id()).toList());
        assertThat(shopping.favorites(owner,1,10).total()).isEqualTo(2); assertThat(shopping.cart(owner)).isEmpty();
    }
    @Test void missingProductRollsBackTransferAndCheckout() throws Exception {
        shopping.addCart(owner,product,1); shopping.addCart(owner,secondProduct(),2);
        long first=cartId(owner,product), last=cartId(owner,second);
        jdbc.update("DELETE FROM product WHERE id=?",second);
        for(String endpoint:List.of("checkout","to-favorite")) {
            mvc.perform(post("/api/customer/cart/"+endpoint).session(session).contentType(MediaType.APPLICATION_JSON)
                    .content(selection(first,last))).andExpect(status().isBadRequest());
            assertThat(shopping.cart(owner)).hasSize(2); assertThat(orderCount()).isZero();
            assertThat(shopping.favorites(owner,1,10).total()).isZero();
        }
    }
    /** 模拟条目清理时数据库失败，验证已落库的订单/收藏和之前的删除一并回滚。 */
    public static class RejectDelete implements org.h2.api.Trigger {
        static volatile Long blockedId;
        @Override public void fire(java.sql.Connection connection,Object[] oldRow,Object[] newRow) throws java.sql.SQLException {
            if (Objects.equals(((Number)oldRow[0]).longValue(),blockedId)) throw new java.sql.SQLException("test cleanup failure");
        }
    }
    @Test void cleanupFailureRollsBackOrderFavoritesAndPriorDeletes() {
        shopping.addCart(owner,product,1); shopping.addCart(owner,secondProduct(),2);
        long first=cartId(owner,product), last=cartId(owner,second);
        RejectDelete.blockedId=last;
        jdbc.execute("CREATE TRIGGER shopping_reject_delete BEFORE DELETE ON customer_cart FOR EACH ROW CALL 'com.onlineshop.controller.CustomerShoppingTest$RejectDelete'");
        try {
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> shopping.checkout(owner,List.of(first,last))).isInstanceOf(RuntimeException.class);
            assertThat(orderCount()).isZero(); assertThat(shopping.cart(owner)).hasSize(2);
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> shopping.toFavorite(owner,List.of(first,last))).isInstanceOf(RuntimeException.class);
            assertThat(shopping.favorites(owner,1,10).total()).isZero(); assertThat(shopping.cart(owner)).hasSize(2);
        } finally { jdbc.execute("DROP TRIGGER shopping_reject_delete"); RejectDelete.blockedId=null; }
    }
    @Test void simultaneousCheckoutsCreateOnlyOneOrder() throws Exception {
        shopping.addCart(owner,product,2); long id=cartId();
        var pool=Executors.newFixedThreadPool(2); var gate=new CountDownLatch(1);
        try {
            List<Future<Boolean>> jobs=new ArrayList<>();
            for(int i=0;i<2;i++) jobs.add(pool.submit(() -> {
                gate.await();
                try { shopping.checkout(owner,List.of(id)); return true; }
                catch(com.onlineshop.common.BizException ex) { return false; }
            }));
            gate.countDown(); int succeeded=0;
            for(var job:jobs) if(job.get(30,TimeUnit.SECONDS)) succeeded++;
            assertThat(succeeded).isEqualTo(1); assertThat(orderCount()).isEqualTo(1); assertThat(shopping.cart(owner)).isEmpty();
        } finally { pool.shutdownNow(); }
    }
}
