package com.web.backend.controller.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RsaKeyResponse {
    private String privateKey;
}
