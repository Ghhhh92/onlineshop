package com.onlineshop.service;

import com.onlineshop.common.BizException;
import com.onlineshop.entity.Seller;
import com.onlineshop.mapper.SellerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 卖家服务：登录校验、查询当前卖家、修改密码
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

    /**
     * 修改密码（US-12）：先核对原密码，新密码加密保存，并把密码版本号加 1，
     * 让修改之前的所有登录（包括其他浏览器）失效。
     */
    public void changePassword(Long sellerId, String oldPassword, String newPassword) {
        Seller seller = getById(sellerId);
        if (!passwordEncoder.matches(oldPassword, seller.getPassword())) {
            throw new BizException("原密码错误");
        }
        if (oldPassword.equals(newPassword)) {
            throw new BizException("新密码不能和原密码相同");
        }
        Seller update = new Seller();
        update.setId(sellerId);
        update.setPassword(passwordEncoder.encode(newPassword));
        update.setPasswordVersion(seller.getPasswordVersion() + 1);
        sellerMapper.updateById(update);
    }
}
