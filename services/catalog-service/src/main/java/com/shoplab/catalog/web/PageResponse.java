package com.shoplab.catalog.web;

import java.util.List;

/**
 * Body của mọi endpoint list có phân trang — cùng một shape cho client ở mọi service.
 *
 * <p>Cố ý KHÔNG trả thẳng {@code Page} của Spring: nó serialize cả chục field nội bộ ({@code
 * pageable}, {@code sort}, {@code first}, {@code numberOfElements}...) ⇒ contract loang ra theo
 * version Spring. Đây là DTO tối thiểu, ổn định.
 *
 * @param content phần tử của trang hiện tại
 * @param page số trang (bắt đầu từ 0)
 * @param size số phần tử mỗi trang
 * @param totalElements tổng số phần tử khớp filter (để client tự tính số trang)
 * @param totalPages tổng số trang
 */
public record PageResponse<T>(
    List<T> content, int page, int size, long totalElements, int totalPages) {}
