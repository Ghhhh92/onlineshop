package com.onlineshop.controller;

import com.onlineshop.common.Result;
import com.onlineshop.dto.IntentQueryResult;
import com.onlineshop.dto.IntentSubmitResult;
import com.onlineshop.dto.SubmitIntentRequest;
import com.onlineshop.service.BuyerIntentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 买家购买意向接口：买家免注册，不需要登录。
 *
 * @author 李帆
 * @date 2026-10-19
 */
@RestController
@RequestMapping("/api/buyer/intent")
@RequiredArgsConstructor
public class BuyerIntentController {

    private final BuyerIntentService buyerIntentService;

    /**
     * 提交购买意向，返回唯一口令码和当前排队位次
     */
    @PostMapping
    public Result<IntentSubmitResult> submit(@Valid @RequestBody SubmitIntentRequest request) {
        return Result.success(buyerIntentService.submit(request));
    }

    /**
     * 凭口令码查询排队位次和状态
     */
    @GetMapping("/{code}")
    public Result<IntentQueryResult> query(@PathVariable String code) {
        return Result.success(buyerIntentService.query(code));
    }
}
