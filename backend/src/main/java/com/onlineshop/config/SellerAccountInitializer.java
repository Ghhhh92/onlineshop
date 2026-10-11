package com.onlineshop.config;

import com.onlineshop.entity.Seller;
import com.onlineshop.mapper.SellerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时检查卖家表：没有任何卖家时，按 shop.default-seller 创建初始账号。
 * 已经有卖家（包括改过密码的）时什么都不做，所以重复启动不会重复创建、也不会覆盖密码。
 *
 * @author 李帆
 * @date 2026-10-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SellerAccountInitializer implements ApplicationRunner {

    private final SellerMapper sellerMapper;
    private final ShopProperties shopProperties;

    @Override
    public void run(ApplicationArguments args) {
        if (sellerMapper.selectCount(null) > 0) {
            return;
        }
        ShopProperties.DefaultSeller defaultSeller = shopProperties.defaultSeller();
        Seller seller = new Seller();
        seller.setUsername(defaultSeller.username());
        seller.setPassword(defaultSeller.password());
        sellerMapper.insert(seller);
        log.info("卖家表为空，已创建初始卖家账号：{}", seller.getUsername());
    }
}
