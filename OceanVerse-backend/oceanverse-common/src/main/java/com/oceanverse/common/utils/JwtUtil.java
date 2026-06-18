package com.oceanverse.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;

public class JwtUtil {

    public static final String TOKEN_TYPE_ACCESS = "ACCESS";
    public static final String TOKEN_TYPE_REFRESH = "REFRESH";

    private static final long ACCESS_TOKEN_EXPIRE_MS = 2 * 60 * 60 * 1000L;
    private static final long REFRESH_TOKEN_EXPIRE_MS = 7 * 24 * 60 * 60 * 1000L;

    private static PrivateKey getPrivateKey() {
        return RsaKeyUtil.getPrivateKey();
    }

    private static PublicKey getPublicKey() {
        return RsaKeyUtil.getPublicKey();
    }

    public static String generateAccessToken(Long userId, String username, String role) {
        return generateToken(userId, username, role, null, TOKEN_TYPE_ACCESS, ACCESS_TOKEN_EXPIRE_MS);
    }

    public static String generateAccessToken(Long userId, String username, String role, Integer dataScope) {
        return generateToken(userId, username, role, dataScope, TOKEN_TYPE_ACCESS, ACCESS_TOKEN_EXPIRE_MS);
    }

    public static String generateRefreshToken(Long userId, String username, String role) {
        return generateToken(userId, username, role, null, TOKEN_TYPE_REFRESH, REFRESH_TOKEN_EXPIRE_MS);
    }

    public static String generateRefreshToken(Long userId, String username, String role, Integer dataScope) {
        return generateToken(userId, username, role, dataScope, TOKEN_TYPE_REFRESH, REFRESH_TOKEN_EXPIRE_MS);
    }

    private static String generateToken(Long userId, String username, String role,
                                        Integer dataScope, String tokenType, long expireMs) {
        var builder = Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("role", role)
                .claim("tokenType", tokenType)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expireMs));
        if (dataScope != null) {
            builder.claim("dataScope", dataScope);
        }
        return builder.signWith(getPrivateKey()).compact();
    }

    public static Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getPublicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static Long getUserId(String token) {
        return Long.parseLong(parseToken(token).getSubject());
    }

    public static String getUsername(String token) {
        return parseToken(token).get("username", String.class);
    }

    public static String getRole(String token) {
        return parseToken(token).get("role", String.class);
    }

    public static Integer getDataScope(String token) {
        return parseToken(token).get("dataScope", Integer.class);
    }

    public static String getTokenType(String token) {
        return parseToken(token).get("tokenType", String.class);
    }

    public static boolean isAccessToken(String token) {
        return TOKEN_TYPE_ACCESS.equals(getTokenType(token));
    }

    public static boolean isRefreshToken(String token) {
        return TOKEN_TYPE_REFRESH.equals(getTokenType(token));
    }

    public static boolean validate(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static long getRemainingSeconds(String token) {
        Claims claims = parseToken(token);
        Date expiration = claims.getExpiration();
        return Math.max(0, (expiration.getTime() - System.currentTimeMillis()) / 1000);
    }
}
