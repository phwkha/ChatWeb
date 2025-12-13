package com.web.backend.service;

import com.web.backend.common.TokenType;
import io.jsonwebtoken.Claims;

import java.util.List;
import java.util.function.Function;

public interface JwtService {

    String generateAccessToken(String username, List<String> authorities, Integer tokenVersion);

    String generateRefreshToken(String username, List<String> authorities, Integer tokenVersion);

    String extractUsername(String token, TokenType type);

    <T> T extractClaim(String token, TokenType type, Function<Claims, T> claimsResolver);

    public long getRemainingTime(String token);
}
