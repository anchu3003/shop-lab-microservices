package com.shoplab.order.order;

import java.util.List;

/** Một trang đơn hàng — cùng hình dạng với page của catalog để client dùng chung một kiểu parse. */
public record OrderPage(
    List<OrderResponse> content, int page, int size, long totalElements, int totalPages) {}
