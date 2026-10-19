package com.onlineshop.service;

import com.onlineshop.common.BizException;
import com.onlineshop.dto.IntentQueryResult;
import com.onlineshop.dto.IntentSubmitResult;
import com.onlineshop.dto.SubmitIntentRequest;
import com.onlineshop.entity.Product;
import com.onlineshop.entity.PurchaseIntent;
import com.onlineshop.enums.IntentStatus;
import com.onlineshop.enums.ProductStatus;
import com.onlineshop.mapper.ProductMapper;
import com.onlineshop.mapper.PurchaseIntentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * 买家侧购买意向：提交意向（US-04、US-05、US-29、US-30），凭口令码查询位次和状态（US-06、US-07）。
 *
 * @author 李帆
 * @date 2026-10-19
 */
@Service
@RequiredArgsConstructor
public class BuyerIntentService {

    /** 生成的口令码和已有的撞上时最多重试几次，正常情况下第一次就不会重复 */
    private static final int MAX_CODE_ATTEMPTS = 5;

    private final ProductMapper productMapper;
    private final PurchaseIntentMapper intentMapper;
    private final IntentCodeGenerator codeGenerator;

    /**
     * 提交购买意向，返回口令码和当前位次。
     * <p>
     * 先锁住当前商品这一行，再检查状态、计算序号、写入意向：多人同时提交时一个接一个执行，
     * 序号严格按提交顺序递增（先到先得）；商品被冻结的那一刻之后，不会再混进新的意向。
     */
    @Transactional
    public IntentSubmitResult submit(SubmitIntentRequest request) {
        Product product = productMapper.selectCurrentForUpdate();
        if (product == null) {
            throw new BizException("暂无在售商品");
        }
        if (product.getStatus() == ProductStatus.FROZEN) {
            throw new BizException("商品交易中");
        }
        String phone = request.getPhone().trim();
        if (intentMapper.existsActiveByPhone(product.getId(), phone)) {
            throw new BizException("该手机号已在排队中");
        }

        PurchaseIntent intent = new PurchaseIntent();
        intent.setProductId(product.getId());
        intent.setName(request.getName().trim());
        intent.setPhone(phone);
        intent.setCode(newUniqueCode());
        intent.setPosition(intentMapper.selectMaxPosition(product.getId()) + 1);
        intent.setStatus(IntentStatus.QUEUING);
        intent.setCreateTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        intentMapper.insert(intent);

        return new IntentSubmitResult(intent.getCode(), currentPosition(intent), intent.getCreateTime());
    }

    /**
     * 凭口令码查询位次和状态。口令码不区分大小写，前后空格忽略。
     * 交易结束（成功、失败）、已撤销或商品已下架时，口令码失效（US-10）。
     */
    public IntentQueryResult query(String rawCode) {
        String code = rawCode == null ? "" : rawCode.trim().toUpperCase(Locale.ROOT);
        PurchaseIntent intent = intentMapper.selectByCode(code);
        if (intent == null) {
            throw new BizException(404, "口令码不存在");
        }
        Product product = productMapper.selectById(intent.getProductId());
        if (!PurchaseIntentMapper.ACTIVE_STATUSES.contains(intent.getStatus())
                || product == null || product.getStatus() == ProductStatus.DELISTED) {
            throw new BizException("口令码已失效");
        }
        return new IntentQueryResult(intent.getCode(), currentPosition(intent), intent.getStatus(),
                intent.getStatus().getDesc(), intent.getName(), maskPhone(intent.getPhone()),
                product.getName(), intent.getCreateTime());
    }

    /** 当前位次 = 排在它前面、仍在队列中的意向数 + 1 */
    private int currentPosition(PurchaseIntent intent) {
        return (int) intentMapper.countActiveBefore(intent.getProductId(), intent.getPosition()) + 1;
    }

    private String newUniqueCode() {
        for (int i = 0; i < MAX_CODE_ATTEMPTS; i++) {
            String code = codeGenerator.next();
            if (intentMapper.selectByCode(code) == null) {
                return code;
            }
        }
        throw new IllegalStateException("连续 " + MAX_CODE_ATTEMPTS + " 次生成的口令码都已存在");
    }

    /** 13800001234 → 138****1234 */
    static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
