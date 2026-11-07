package com.shoplab.inventory.stock;

import com.shoplab.commons.error.ErrorCode;
import com.shoplab.commons.web.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Trả <b>chi tiết theo SKU</b> khi hết hàng — {@code GlobalExceptionHandler} của commons chỉ trả
 * {@code code + message}, còn client cần biết <b>SKU nào</b> thiếu và còn bao nhiêu để hiển thị.
 *
 * <p>Đây là lý do tồn tại của advice riêng: thêm {@code details} mà <b>không</b> phải sửa handler
 * dùng chung (giữ commons không phụ thuộc domain của inventory).
 */
@RestControllerAdvice
public class StockApiExceptionHandler {

  @ExceptionHandler(InsufficientStockException.class)
  public ResponseEntity<ApiError> handleInsufficientStock(
      InsufficientStockException ex, HttpServletRequest request) {
    List<ApiError.FieldViolation> details =
        List.of(
            new ApiError.FieldViolation(
                ex.skuId().toString(), "còn " + ex.available() + ", cần " + ex.requested()));
    return ResponseEntity.status(ErrorCode.CONFLICT.httpStatus())
        .body(ApiError.of(ErrorCode.CONFLICT, ex.getMessage(), request.getRequestURI(), details));
  }
}
