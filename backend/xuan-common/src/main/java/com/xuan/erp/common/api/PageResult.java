package com.xuan.erp.common.api;

import java.util.List;

public record PageResult<T>(List<T> records, long total, long pageNum, long pageSize) {

    public static <T> PageResult<T> empty(long pageNum, long pageSize) {
        return new PageResult<>(List.of(), 0, pageNum, pageSize);
    }
}
