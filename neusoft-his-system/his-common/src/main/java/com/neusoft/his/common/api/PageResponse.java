package com.neusoft.his.common.api;

import java.util.List;

/**
 * 通用分页响应结构，封装 MyBatis-Plus 分页查询结果。
 *
 * @param page    当前页码，从 1 开始
 * @param size    每页条数
 * @param total   满足查询条件的总记录数
 * @param records 当前页数据列表
 */
public record PageResponse<T>(long page, long size, long total, List<T> records) {
}
