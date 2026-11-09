package com.shoplab.search.index;

import java.util.List;

/**
 * Body của `GET /v1/search` — cùng shape với list endpoint của catalog (`PageResponse`) để client
 * không phải học hai kiểu phân trang.
 */
public record SearchPage<T>(
    List<T> content, int page, int size, long totalElements, int totalPages) {}
