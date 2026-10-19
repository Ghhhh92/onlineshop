package com.onlineshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.onlineshop.entity.PurchaseIntent;
import com.onlineshop.enums.IntentStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 购买意向 Mapper
 * <p>
 * position 列存的是「入队序号」：同一商品下按提交顺序从 1 递增，不随撤销改变；
 * 买家当前排第几，按「序号更小、仍在队列中的意向数 + 1」实时计算。
 *
 * @author 高佳豪
 * @date 2026-10-08
 */
@Mapper
public interface PurchaseIntentMapper extends BaseMapper<PurchaseIntent> {

    /** 仍在队列中的状态：排队中、交易中 */
    List<IntentStatus> ACTIVE_STATUSES = List.of(IntentStatus.QUEUING, IntentStatus.TRADING);

    /**
     * 同一商品下最大的入队序号，没有意向时为 0
     */
    @Select("SELECT COALESCE(MAX(position), 0) FROM purchase_intent WHERE product_id = #{productId}")
    int selectMaxPosition(@Param("productId") Long productId);

    /**
     * 排在指定入队序号之前、仍在队列中的意向数
     */
    default long countActiveBefore(Long productId, int position) {
        return selectCount(Wrappers.<PurchaseIntent>lambdaQuery()
                .eq(PurchaseIntent::getProductId, productId)
                .in(PurchaseIntent::getStatus, ACTIVE_STATUSES)
                .lt(PurchaseIntent::getPosition, position));
    }

    /**
     * 这个手机号在该商品下是否已有仍在队列中的意向
     */
    default boolean existsActiveByPhone(Long productId, String phone) {
        return selectCount(Wrappers.<PurchaseIntent>lambdaQuery()
                .eq(PurchaseIntent::getProductId, productId)
                .eq(PurchaseIntent::getPhone, phone)
                .in(PurchaseIntent::getStatus, ACTIVE_STATUSES)) > 0;
    }

    /**
     * 按口令码查意向，不存在时返回 null
     */
    default PurchaseIntent selectByCode(String code) {
        return selectOne(Wrappers.<PurchaseIntent>lambdaQuery().eq(PurchaseIntent::getCode, code));
    }

    /**
     * 某件商品的全部意向，按入队顺序（先到先得）排列
     */
    default List<PurchaseIntent> selectByProductInOrder(Long productId) {
        return selectList(Wrappers.<PurchaseIntent>lambdaQuery()
                .eq(PurchaseIntent::getProductId, productId)
                .orderByAsc(PurchaseIntent::getPosition)
                .orderByAsc(PurchaseIntent::getId));
    }
}
