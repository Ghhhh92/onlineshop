package com.onlineshop.controller;

import com.onlineshop.auth.RequireLogin;
import com.onlineshop.common.Result;
import com.onlineshop.dto.ImageUploadResult;
import com.onlineshop.dto.PageResult;
import com.onlineshop.dto.ProductInfo;
import com.onlineshop.dto.PublishProductRequest;
import com.onlineshop.entity.Product;
import com.onlineshop.service.BuyerProductService;
import com.onlineshop.service.ProductImageService;
import com.onlineshop.service.ProductInfoAssembler;
import com.onlineshop.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 商品接口。商品发布后不提供修改接口（US-28），只能下架后重新发布。
 *
 * @author 李帆
 * @date 2026-10-11
 */
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductImageService productImageService;
    private final BuyerProductService buyerProductService;
    private final ProductInfoAssembler productInfoAssembler;

    /**
     * 发布商品（卖家后台，需要登录）。已有在售商品时也可以继续发布（US-31）。
     */
    @RequireLogin
    @PostMapping("/publish")
    public Result<ProductInfo> publish(@Valid @RequestBody PublishProductRequest request) {
        return Result.success(productInfoAssembler.toInfo(productService.publish(request)));
    }

    /**
     * 上传一张商品图片（US-27，卖家后台，需要登录）：表单字段 file，JPG 或 PNG，不超过 5MB。
     * 返回图片地址：发布商品时按顺序放进 images；富文本描述里插图也用这个地址。
     */
    @RequireLogin
    @PostMapping("/image")
    public Result<ImageUploadResult> uploadImage(@RequestParam(value = "file", required = false) MultipartFile file) {
        return Result.success(new ImageUploadResult(productImageService.store(file)));
    }

    /**
     * 买家浏览商品（US-01、US-31）：在售和交易中的商品，按发布时间倒序分页，可按类别筛选。
     * 交易中的商品 status 为 FROZEN，买家端据此显示「商品交易中」；一件都没有时 records 为空数组。
     */
    @GetMapping("/list")
    public Result<PageResult<ProductInfo>> list(@RequestParam(required = false) Long categoryId,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        return Result.success(buyerProductService.list(categoryId, page, size));
    }

    /**
     * 买家查看商品详情：不存在或已下架时返回 404
     */
    @GetMapping("/{id}")
    public Result<ProductInfo> detail(@PathVariable Long id) {
        return Result.success(buyerProductService.detail(id));
    }

    /**
     * 只展示一件商品的买家首页（US-01、US-02）：最新发布的在售或交易中商品，都没有时 data 为 null。
     * 商品冻结时照常返回名称、描述、图片、价格，status 为 FROZEN，买家端据此显示「商品交易中」。
     * 多件商品用 {@link #list}。
     */
    @GetMapping("/on-sale")
    public Result<ProductInfo> getOnSale() {
        Product product = productService.getCurrentProduct();
        return Result.success(product == null ? null : productInfoAssembler.toInfo(product));
    }
}
