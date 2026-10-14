package com.onlineshop.config;

import com.onlineshop.entity.Seller;
import com.onlineshop.mapper.SellerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 启动时检查卖家账号：
 * <ul>
 *     <li>卖家表为空：按 shop.default-seller 创建初始账号，密码用 BCrypt 加密后保存；</li>
 *     <li>已有卖家但密码还是明文：转成 BCrypt 加密后保存，原密码照样能登录。</li>
 * </ul>
 * 已经加密的密码不会改动，所以重复启动不会重复创建账号，也不会覆盖改过的密码。
 *
 * @author 李帆
 * @date 2026-10-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SellerAccountInitializer implements ApplicationRunner {

    /** BCrypt 加密结果的格式：$2a$10$ 加 53 位盐和哈希 */
    private static final Pattern BCRYPT_HASH = Pattern.compile("^\\$2[aby]?\\$\\d{2}\\$[./0-9A-Za-z]{53}$");

    private final SellerMapper sellerMapper;
    private final ShopProperties shopProperties;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (sellerMapper.selectCount(null) == 0) {
            createDefaultSeller();
        } else {
            encryptPlainPasswords();
        }
    }

    private void createDefaultSeller() {
        ShopProperties.DefaultSeller defaultSeller = shopProperties.defaultSeller();
        Seller seller = new Seller();
        seller.setUsername(defaultSeller.username());
        seller.setPassword(passwordEncoder.encode(defaultSeller.password()));
        seller.setPasswordVersion(0);
        sellerMapper.insert(seller);
        log.info("卖家表为空，已创建初始卖家账号：{}", seller.getUsername());
    }

    private void encryptPlainPasswords() {
        for (Seller seller : sellerMapper.selectList(null)) {
            if (BCRYPT_HASH.matcher(seller.getPassword()).matches()) {
                continue;
            }
            Seller update = new Seller();
            update.setId(seller.getId());
            update.setPassword(passwordEncoder.encode(seller.getPassword()));
            sellerMapper.updateById(update);
            log.info("卖家 {} 的密码已从明文转为加密存储", seller.getUsername());
        }
    }
}
