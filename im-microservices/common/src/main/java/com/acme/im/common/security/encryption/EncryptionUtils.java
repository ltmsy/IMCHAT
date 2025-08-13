package com.acme.im.common.security.encryption;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 加密工具类
 * 只保留实际使用的功能：SHA-256哈希和盐值生成
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Component
@Slf4j
public class EncryptionUtils {

    private static final String SHA_256 = "SHA-256";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 生成随机盐值
     * 
     * @param length 盐值长度
     * @return Base64编码的盐值
     */
    public String generateSalt(int length) {
        byte[] salt = new byte[length];
        SECURE_RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * 使用SHA-256算法计算哈希值
     * 
     * @param data 要哈希的数据
     * @param algorithm 算法名称（目前只支持SHA-256）
     * @return Base64编码的哈希值
     */
    public String hash(String data, String algorithm) {
        if (!SHA_256.equals(algorithm)) {
            throw new IllegalArgumentException("目前只支持SHA-256算法");
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] hashBytes = digest.digest(data.getBytes());
            String hash = Base64.getEncoder().encodeToString(hashBytes);
            
            log.debug("哈希计算成功: algorithm={}, dataLength={}, hashLength={}", 
                     algorithm, data.length(), hash.length());
            
            return hash;
        } catch (NoSuchAlgorithmException e) {
            log.error("哈希计算失败: algorithm={}, error={}", algorithm, e.getMessage());
            throw new RuntimeException("哈希计算失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证密码
     * 
     * @param password 原始密码
     * @param salt 盐值
     * @param expectedHash 期望的哈希值
     * @return 是否匹配
     */
    public boolean verifyPassword(String password, String salt, String expectedHash) {
        String passwordWithSalt = password + salt;
        String actualHash = hash(passwordWithSalt, SHA_256);
        return actualHash.equals(expectedHash);
    }
} 