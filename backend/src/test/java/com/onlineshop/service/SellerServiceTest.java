package com.onlineshop.service;

import com.onlineshop.common.BizException;
import com.onlineshop.entity.Seller;
import com.onlineshop.mapper.SellerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * 卖家登录 Service 单元测试（Mockito）
 *
 * @author 叶文锦
 * @date 2026-10-13
 */
@ExtendWith(MockitoExtension.class)
class SellerServiceTest {

    @Mock
    private SellerMapper sellerMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private SellerService sellerService;

    private Seller admin;

    @BeforeEach
    void setUp() {
        admin = new Seller();
        admin.setId(1L);
        admin.setUsername("admin");
        // 库里存的是 BCrypt 加密后的密码，这里只用一个占位串，校验交给 mock 的 passwordEncoder
        admin.setPassword("$2a$10$hashedplaceholder");
    }

    @Test
    @DisplayName("登录成功返回卖家")
    void loginSuccess() {
        when(sellerMapper.selectByUsername("admin")).thenReturn(admin);
        when(passwordEncoder.matches("123456", admin.getPassword())).thenReturn(true);

        Seller result = sellerService.login("admin", "123456");

        assertThat(result.getUsername()).isEqualTo("admin");
    }

    @Test
    @DisplayName("用户名不存在抛业务异常，提示和密码错误一致")
    void loginUserNotFound() {
        when(sellerMapper.selectByUsername("nobody")).thenReturn(null);

        assertThatThrownBy(() -> sellerService.login("nobody", "123456"))
                .isInstanceOf(BizException.class)
                .hasMessage("用户名或密码错误");
    }

    @Test
    @DisplayName("密码错误抛业务异常")
    void loginWrongPassword() {
        when(sellerMapper.selectByUsername("admin")).thenReturn(admin);
        when(passwordEncoder.matches("wrong", admin.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> sellerService.login("admin", "wrong"))
                .isInstanceOf(BizException.class)
                .hasMessage("用户名或密码错误");
    }
}
