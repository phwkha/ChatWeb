package com.web.backend.JWT;

import com.web.backend.model.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    // method to extract the user id
    public Long extractUserId(String jwtToken) {
        Object userIdObj = extractClaim(jwtToken, claims -> claims.get("userId"));
        if (userIdObj == null) return null;

        if (userIdObj instanceof Integer) {
            return ((Integer) userIdObj).longValue();
        } else if (userIdObj instanceof Long) {
            return (Long) userIdObj;
        }
        try {
            return Long.parseLong(userIdObj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }


    private <T> T extractClaim(String jwtToken, Function<Claims, T> claimsResolver) {

        final Claims claims = extractAllClaims(jwtToken);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String jwtToken) {

        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(jwtToken)
                .getPayload();
    }

    public SecretKey getSignInKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    public String generateToken(UserEntity userEntity) {

        return generateToken(new HashMap<>(), userEntity);
    }

    public String generateToken(Map<String, Object> extraClaims, UserEntity userEntity) {

        Map<String, Object> claims = new HashMap<>(extraClaims);
        claims.put("userId", userEntity.getId());

        return Jwts.builder().claims(claims)
                .subject(userEntity.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis()+jwtExpiration))
                .signWith(getSignInKey())
                .compact();
    }

    public boolean isTokenValid(String jwtToken, UserEntity userEntity) {

        final Long userIdFromToken = extractUserId(jwtToken);

        final Long userId = userEntity.getId();

        return (userIdFromToken != null && userIdFromToken.equals(userId) && !isTokenExpired(jwtToken));
    }

    private boolean isTokenExpired(String jwtToken) {
        return extractExpiration(jwtToken).before(new Date());
    }

    private Date extractExpiration(String jwtToken) {
        return extractClaim(jwtToken, Claims::getExpiration);
    }
}