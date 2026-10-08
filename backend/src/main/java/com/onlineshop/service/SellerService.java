package com.onlineshop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.onlineshop.common.BizException;
import com.onlineshop.entity.Seller;
import com.onlineshop.mapper.SellerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 卖家服务
 *
 * @author 李帆
 * @date 2026-10-08
 */
@Service
@RequiredArgsConstructor
public class SellerService {

    private final SellerMapper sellerMapper;

    /**
     * 卖家登录
     */
    public Seller login(String username, String password) {
        Seller seller = sellerMapper.selectOne(
                new LambdaQueryWrapper<Seller>().eq(Seller::getUsername, username));
        if (seller == null) {
            throw new BizException("用户名不存在");
        }
        // 迭代 2 暂用明文比对，后续迭代接入 BCrypt
        if (!seller.getPassword().equals(password)) {
            throw new BizException("密码错误");
        }
        return seller;
    }
}
