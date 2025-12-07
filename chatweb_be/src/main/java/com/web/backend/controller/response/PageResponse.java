package com.web.backend.controller.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PageResponse<T> {
    private List<T> content;      // Danh sách dữ liệu (VD: List<UserDTO>)
    private int pageNo;           // Trang hiện tại (bắt đầu từ 0)
    private int pageSize;         // Số lượng item trên 1 trang
    private long totalElements;   // Tổng số item trong DB
    private int totalPages;       // Tổng số trang
    private boolean last;         // Có phải trang cuối cùng không?
}