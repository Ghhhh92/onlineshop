package com.onlineshop.dto;

/**
 * 图片上传结果
 *
 * @param url 图片的访问地址，发布商品时填进 image 字段
 * @author 李帆
 * @date 2026-10-31
 */
public record ImageUploadResult(String url) {
}
