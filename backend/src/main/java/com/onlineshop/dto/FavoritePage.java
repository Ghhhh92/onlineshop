package com.onlineshop.dto;

import java.util.List;

/** 挑战 B 契约分页结构，不影响旧接口的 records 格式。 */
public record FavoritePage(List<ShoppingItem> items, long total, int page, int size) {
}
