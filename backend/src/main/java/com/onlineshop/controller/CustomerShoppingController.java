package com.onlineshop.controller;

import com.onlineshop.auth.CustomerSession;
import com.onlineshop.auth.RequireCustomerLogin;
import com.onlineshop.common.Result;
import com.onlineshop.dto.AddCartRequest;
import com.onlineshop.dto.CartSelectionRequest;
import com.onlineshop.dto.FavoriteRequest;
import com.onlineshop.dto.OrderDetail;
import com.onlineshop.dto.PageResult;
import com.onlineshop.dto.ShoppingItem;
import com.onlineshop.dto.UpdateCartRequest;
import com.onlineshop.service.CustomerShoppingService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/** 挑战 B 收藏与购物车管理；客户身份只从会话获取。 */
@RestController
@RequestMapping("/api/customer")
@RequireCustomerLogin
@RequiredArgsConstructor
public class CustomerShoppingController {
    private final CustomerShoppingService shopping;

    @GetMapping("/favorite")
    public Result<PageResult<ShoppingItem>> favorites(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size, HttpSession session) {
        return Result.success(shopping.favorites(CustomerSession.currentCustomerId(session), page, size));
    }

    @PostMapping("/favorite")
    public Result<Void> favorite(@Valid @RequestBody FavoriteRequest request, HttpSession session) {
        shopping.favorite(CustomerSession.currentCustomerId(session), request.getProductId());
        return Result.success();
    }

    @DeleteMapping("/favorite/{productId}")
    public Result<Void> unfavorite(@PathVariable Long productId, HttpSession session) {
        shopping.unfavorite(CustomerSession.currentCustomerId(session), productId);
        return Result.success();
    }

    @GetMapping("/cart")
    public Result<List<ShoppingItem>> cart(HttpSession session) {
        return Result.success(shopping.cart(CustomerSession.currentCustomerId(session)));
    }

    @PostMapping("/cart")
    public Result<Void> addCart(@Valid @RequestBody AddCartRequest request, HttpSession session) {
        shopping.addCart(CustomerSession.currentCustomerId(session), request.getProductId(), request.getQuantity());
        return Result.success();
    }

    @PutMapping("/cart/{id}")
    public Result<Void> updateCart(@PathVariable Long id, @Valid @RequestBody UpdateCartRequest request, HttpSession session) {
        shopping.updateCart(CustomerSession.currentCustomerId(session), id, request.getQuantity());
        return Result.success();
    }

    @DeleteMapping("/cart/{id}")
    public Result<Void> removeCart(@PathVariable Long id, HttpSession session) {
        shopping.removeCart(CustomerSession.currentCustomerId(session), id);
        return Result.success();
    }

    @PostMapping("/cart/checkout")
    public Result<OrderDetail> checkout(@Valid @RequestBody CartSelectionRequest request, HttpSession session) {
        return Result.success(shopping.checkout(CustomerSession.currentCustomerId(session), request.getCartItemIds()));
    }

    @PostMapping("/cart/to-favorite")
    public Result<Void> toFavorite(@Valid @RequestBody CartSelectionRequest request, HttpSession session) {
        shopping.toFavorite(CustomerSession.currentCustomerId(session), request.getCartItemIds());
        return Result.success();
    }
}
