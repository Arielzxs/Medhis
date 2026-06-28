package com.neusoft.his.common.api;

/**
 * 分页参数保护工具。
 *
 * <p>接口调试或前端传参时可能把 size 传得过大，统一限制后可以避免一次查询和返回过多数据。</p>
 */
public final class PageSupport {
    public static final long DEFAULT_PAGE = 1L;
    public static final long DEFAULT_SIZE = 10L;
    public static final long MAX_SIZE = 50L;

    private PageSupport() {
    }

    public static long page(long page) {
        return Math.max(page, DEFAULT_PAGE);
    }

    public static long size(long size) {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
