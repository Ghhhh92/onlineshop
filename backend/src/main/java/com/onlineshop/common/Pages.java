package com.onlineshop.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 分页参数：页码从 1 开始，每页最多 50 条；超出范围的页码、条数按边界处理，不报错。
 *
 * @author 李帆
 * @date 2026-11-02
 */
public final class Pages {

    /** 每页最多条数 */
    public static final int MAX_SIZE = 50;

    private Pages() {
    }

    /**
     * 按边界处理后的分页参数，传给 mapper.selectPage
     */
    public static <T> Page<T> of(int page, int size) {
        return new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), MAX_SIZE));
    }
}
