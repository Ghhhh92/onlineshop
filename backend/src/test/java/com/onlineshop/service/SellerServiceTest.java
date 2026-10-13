package com.onlineshop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private SellerService sellerService;

    private Seller admin;

    @BeforeEach
    void setUp() {
        admin = new Seller();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setPassword("123456");
    }

    @Test
    @DisplayName("登录成功返回卖家")
    void loginSuccess() {
        when(sellerMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(admin);

        Seller result = sellerService.login("admin", "123456");

        assertThat(result.getUsername()).isEqualTo("admin");
    }

    @Test
    @DisplayName("用户名不存在抛业务异常")
    void loginUserNotFound() {
        when(sellerMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> sellerService.login("nobody", "123456"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("用户名不存在");
    }

    @Test
    @DisplayName("密码错误抛业务异常")
    void loginWrongPassword() {
        when(sellerMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(admin);

        assertThatThrownBy(() -> sellerService.login("admin", "wrong"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("密码错误");
    }
}
