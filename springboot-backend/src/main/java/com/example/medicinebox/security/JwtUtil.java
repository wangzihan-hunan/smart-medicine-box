package com.example.medicinebox.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT（JSON Web Token）工具类
 *
 * <p>提供JWT token的生成、解析和验证功能。
 * 使用HS256（HMAC-SHA256）签名算法对token进行签名，
 * 确保token的完整性和不可篡改性。
 *
 * <p>主要功能包括：
 * <ul>
 *   <li>生成JWT token</li>
 *   <li>从token中提取用户手机号（subject）</li>
 *   <li>从token中提取过期时间</li>
 *   <li>验证token的有效性（是否过期、签名是否正确）</li>
 * </ul>
 *
 * @author medicine-box
 * @since 1.0.0
 */
@Component
public class JwtUtil {

    /**
     * JWT签名密钥
     * 从配置文件jwt.secret读取，用于生成和验证token签名
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * JWT token过期时间（毫秒）
     * 从配置文件jwt.expiration读取
     */
    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * 获取JWT签名密钥
     *
     * <p>将配置的密钥字符串转换为HMAC-SHA256算法所需的Key对象。
     * 使用UTF-8编码将密钥字符串转换为字节数组。
     *
     * @return 用于JWT签名和验证的密钥对象
     */
    private Key getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, SignatureAlgorithm.HS256.getJcaName());
    }

    /**
     * 从JWT token中提取用户手机号
     *
     * <p>手机号存储在JWT的subject字段中。
     *
     * @param token JWT token字符串
     * @return 用户手机号
     */
    public String extractPhone(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * 从JWT token中提取过期时间
     *
     * @param token JWT token字符串
     * @return token的过期时间
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * 从JWT token中提取指定的声明（Claim）
     *
     * <p>这是一个通用方法，通过传入Function函数式接口来提取任意声明。
     *
     * @param token JWT token字符串
     * @param claimsResolver 声明解析函数，用于从Claims中提取特定字段
     * @param <T> 返回值类型
     * @return 提取出的声明值
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 解析JWT token，获取所有声明（Claims）
     *
     * <p>使用签名密钥验证token的完整性，如果token被篡改或签名不正确，
     * 将抛出异常。
     *
     * @param token JWT token字符串
     * @return 包含所有声明的Claims对象
     * @throws io.jsonwebtoken.JwtException 如果token无效、签名错误或已过期
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 判断JWT token是否已过期
     *
     * @param token JWT token字符串
     * @return 如果token已过期返回true，否则返回false
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * 生成JWT token
     *
     * <p>根据用户手机号生成包含基本声明的JWT token。
     * token中包含：
     * <ul>
     *   <li>subject: 用户手机号</li>
     *   <li>issuedAt: token签发时间</li>
     *   <li>expiration: token过期时间</li>
     * </ul>
     *
     * @param phone 用户手机号，作为token的subject
     * @return 生成的JWT token字符串
     */
    public String generateToken(String phone) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, phone);
    }

    /**
     * 创建JWT token
     *
     * <p>使用JJWT库构建JWT token，设置声明、主题、签发时间、
     * 过期时间，并使用HS256算法进行签名。
     *
     * @param claims 自定义声明（键值对）
     * @param subject token主题（存储用户手机号）
     * @return 生成的JWT token字符串
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 验证JWT token的有效性
     *
     * <p>验证内容包括：
     * <ul>
     *   <li>token格式是否正确</li>
     *   <li>签名是否有效（未被篡改）</li>
     *   <li>token是否已过期</li>
     * </ul>
     *
     * <p>注意：此方法不验证token中的用户是否存在于数据库中，
     * 用户存在性验证由JwtAuthenticationFilter完成。
     *
     * @param token JWT token字符串
     * @return 如果token有效返回true，无效或过期返回false
     */
    public Boolean validateToken(String token) {
        try {
            final String phone = extractPhone(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            // token解析失败（格式错误、签名错误、过期等）均视为无效
            return false;
        }
    }
}
