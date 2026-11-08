package com.shoplab.order.saga.downstream;

/**
 * Response của một call ra ngoài: **body + status HTTP thật**.
 *
 * <p>Vì sao cần cả status: `reserve`/`lock` trả **201** khi tạo mới và **200** khi replay (cùng
 * `orderId`). Bảng audit {@code checkout_saga_steps} ghi đúng con số đó — nhờ vậy nhìn lại biết
 * được "lần này retry hay tạo mới" mà không phải đọc thêm log.
 */
public record DownstreamResult<T>(T body, int status) {}
