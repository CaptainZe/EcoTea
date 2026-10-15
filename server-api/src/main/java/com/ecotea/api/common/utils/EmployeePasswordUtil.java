package com.ecotea.api.common.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * 员工密码校验，算法与 Admin {@code EncryptUtil} 对齐（SHA-256 + salt + 1024 次），不依赖 admin jar。
 */
public final class EmployeePasswordUtil {

    public static final String HASH_ALGORITHM = "SHA-256";
    public static final int HASH_ITERATIONS = 1024;
    /** 与 Admin {@code EncryptUtil#getRandomSalt} 长度一致 */
    public static final int SALT_LENGTH = 6;

    private static final char[] SALT_CHARS =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private EmployeePasswordUtil() {
    }

    /** 随机 salt，长度 {@link #SALT_LENGTH} */
    public static String randomSalt() {
        char[] buf = new char[SALT_LENGTH];
        for (int i = 0; i < SALT_LENGTH; i++) {
            buf[i] = SALT_CHARS[RANDOM.nextInt(SALT_CHARS.length)];
        }
        return new String(buf);
    }

    public static String encrypt(String password, String salt) {
        if (password == null || salt == null) {
            return null;
        }
        byte[] byteSalt = salt.getBytes(StandardCharsets.UTF_8);
        byte[] bytePassword = password.getBytes(StandardCharsets.UTF_8);
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            digest.reset();
            digest.update(byteSalt);
            byte[] hashed = digest.digest(bytePassword);
            for (int i = 0; i < HASH_ITERATIONS - 1; i++) {
                digest.reset();
                hashed = digest.digest(hashed);
            }
            return bytesToHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static boolean matches(String plainPassword, String salt, String storedPassword) {
        if (plainPassword == null || salt == null || storedPassword == null) {
            return false;
        }
        String hashed = encrypt(plainPassword, salt);
        return hashed != null && hashed.equals(storedPassword);
    }

    private static String bytesToHex(byte[] data) {
        char[] digits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        char[] out = new char[data.length << 1];
        for (int i = 0, j = 0; i < data.length; i++) {
            out[j++] = digits[(0xF0 & data[i]) >>> 4];
            out[j++] = digits[0x0F & data[i]];
        }
        return new String(out);
    }
}
