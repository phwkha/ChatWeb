package com.web.backend.controller.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CursorResponse<T> {
    private List<T> content;
    private String nextCursor; // Cursor để client gọi lần sau (có thể null nếu hết tin)
    private boolean hasMore;   // Còn tin nhắn nữa không?
}