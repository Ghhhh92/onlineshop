package com.onlineshop.dto;

import java.util.List;

/**
 * 分页结果：当前页的数据和总条数
 *
 * @param records 当前页的数据
 * @param total   总条数
 * @param <T>     数据类型
 * @author 李帆
 * @date 2026-10-19
 */
public record PageResult<T>(List<T> records, long total) {
}
