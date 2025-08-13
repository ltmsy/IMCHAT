package com.acme.im.common.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import com.acme.im.common.security.jwt.JwtConfig.JwtProperties;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Token提供者
 * 负责JWT令牌的生成、验证和解析
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final UserDetailsService userDetailsService;

    /**
     * 生成访问令牌
     */
    public String generateAccessToken(String username, String deviceId) {
        return generateToken(username, deviceId, jwtProperties.getAccessTokenExpiration());
    }

    /**
     * 生成刷新令牌
     */
    public String generateRefreshToken(String username, String deviceId) {
        return generateToken(username, deviceId, jwtProperties.getRefreshTokenExpiration());
    }

    /**
     * 生成令牌
     */
    private String generateToken(String username, String deviceId, long expirationMinutes) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMinutes * 60 * 1000);

        Map<String, Object> claims = new HashMap<>();
        claims.put("deviceId", deviceId);
        claims.put("type", "access");

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuer(jwtProperties.getIssuer())
                .audience().add(jwtProperties.getAudience()).and()
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 从令牌中获取用户名
     */
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    /**
     * 从令牌中获取设备ID
     */
    public String getDeviceIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("deviceId", String.class);
    }

    /**
     * 从令牌中获取过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    /**
     * 从令牌中获取指定声明
     */
    public <T> T getClaimFromToken(String token, java.util.function.Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 从令牌中获取所有声明
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 检查令牌是否过期
     */
    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    /**
     * 验证令牌
     */
    public Boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT令牌验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从令牌创建认证对象
     */
    public Authentication getAuthentication(String token) {
        String username = getUsernameFromToken(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        // 从配置文件获取密钥
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("JWT secret未配置，请在application.yml中设置jwt.secret");
        }
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
} 