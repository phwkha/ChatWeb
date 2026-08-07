package com.web.backend.config;

import java.util.UUID;

public class ServerIdentity {
    public static final String SERVER_ID = UUID.randomUUID().toString();

    private ServerIdentity() {
    }
}
