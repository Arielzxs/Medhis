package com.neusoft.his.common.api;

/**
 * 后端接口统一响应结构。
 *
 * @param success 请求是否处理成功
 * @param message 面向前端展示的提示信息
 * @param data    业务响应数据
 */
public record ApiResponse<T>(boolean success, String message, T data) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", data);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
