package com.onlineshop.config;

import com.onlineshop.entity.Seller;
import com.onlineshop.mapper.SellerMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 卖家账号初始化：密码一律加密存储。每个用例结束后回滚。
 *
 * @author 李帆
 * @date 2026-10-11
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SellerAccountInitializerTest {

    @Autowired
    private SellerAccountInitializer initializer;

    @Autowired
    private SellerMapper sellerMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("卖家表为空时创建初始账号，密码加密存储")
    void createsDefaultSellerWithEncryptedPassword() throws Exception {
        jdbcTemplate.update("DELETE FROM seller");

        initializer.run(null);

        Seller admin = sellerMapper.selectByUsername("admin");
        assertThat(admin.getPassword()).startsWith("$2").isNotEqualTo("123456");
        assertThat(passwordEncoder.matches("123456", admin.getPassword())).isTrue();
    }

    @Test
    @DisplayName("库里的明文密码在启动时转成加密存储，原密码照样能登录")
    void encryptsPlainPassword() throws Exception {
        jdbcTemplate.update("INSERT INTO seller (username, password) VALUES ('plain_seller', 'oldpass123')");

        initializer.run(null);

        String stored = sellerMapper.selectByUsername("plain_seller").getPassword();
        assertThat(stored).startsWith("$2").isNotEqualTo("oldpass123");
        assertThat(passwordEncoder.matches("oldpass123", stored)).isTrue();
    }

    @Test
    @DisplayName("已经加密的密码不会被改动")
    void keepsEncryptedPassword() throws Exception {
        String before = sellerMapper.selectByUsername("admin").getPassword();

        initializer.run(null);

        assertThat(sellerMapper.selectByUsername("admin").getPassword()).isEqualTo(before);
    }
}
