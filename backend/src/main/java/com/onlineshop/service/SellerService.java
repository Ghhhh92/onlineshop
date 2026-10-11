package com.onlineshop.service;

import com.onlineshop.common.BizException;
import com.onlineshop.entity.Seller;
import com.onlineshop.mapper.SellerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 卖家服务：登录校验、查询当前卖家
 *
 * @author 李帆
 * @date 2026-10-11
 */
@Service
@RequiredArgsConstructor
public class SellerService {

    private final SellerMapper sellerMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 校验用户名和密码，成功返回卖家。
     * 用户名不存在和密码错误给同一个提示，避免别人借此试出用户名。
     */
    public Seller login(String username, String password) {
        Seller seller = sellerMapper.selectByUsername(username);
        if (seller == null || !passwordEncoder.matches(password, seller.getPassword())) {
            throw new BizException("用户名或密码错误");
        }
        return seller;
    }

    /**
     * 按 id 查卖家。登录后账号被删掉等情况下查不到，按未登录处理。
     */
    public Seller getById(Long id) {
        Seller seller = sellerMapper.selectById(id);
        if (seller == null) {
            throw new BizException(401, "请先登录");
        }
        return seller;
    }
}
