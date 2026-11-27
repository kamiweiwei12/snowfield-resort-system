package edu.sysu.snowfield.common.web;

import java.util.List;

/**
 * 统一分页响应，符合 {@code docs/API_CONTRACT.md}：分页从 {@code page=0} 开始，
 * 响应含 {@code page/size/totalElements/totalPages}。数据放在 {@link ApiResponse#data()} 中。
 */
public record PageResponse<T>(int page, int size, long totalElements, int totalPages, List<T> content) {
}
