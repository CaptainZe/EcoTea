package com.appsinnova.admin.business.common.utils.chai;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.EAN13Writer;
import org.springframework.util.StringUtils;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * chai 域 EAN-13 条形码：拼内部码、校验、出图。校验位一律走 ZXing，不自写加权公式。
 * <ul>
 *   <li>系统码：前缀 {@code 29} + {@code spuId} 10 位 + ZXing 校验位</li>
 *   <li>允许录入：国标 {@code 69…}，或内部 {@code 22～29…}</li>
 * </ul>
 */
public final class ChaiBarcodeUtil {

    /** 系统码固定前缀（落在 GS1 22～29） */
    public static final String INTERNAL_PREFIX = "29";

    private static final int EAN13_LEN = 13;
    private static final int BODY12_LEN = 12;
    private static final int SPU_ID_DIGITS = 10;

    private ChaiBarcodeUtil() {
    }

    /**
     * 生成内部 EAN-13（不含落库）。{@code spuId} 须已落库。
     */
    public static String generateInternal(Long spuId) {
        if (spuId == null) {
            throw new IllegalArgumentException("SPU未落库，无法生成条码");
        }
        if (spuId < 0 || spuId > 9_999_999_999L) {
            throw new IllegalArgumentException("spuId超出条码可编码范围");
        }
        String body12 = INTERNAL_PREFIX + String.format("%0" + SPU_ID_DIGITS + "d", spuId);
        return body12 + checksumDigit(body12);
    }

    /**
     * 规范化并校验可保存的条码；空串表示清空。
     * 非空须为 13 位数字，前缀 69 或 22～29，且通过 ZXing 校验位。
     */
    public static String normalizeForSave(String raw) {
        String code = raw != null ? raw.trim() : "";
        if (!StringUtils.hasText(code)) {
            return "";
        }
        if (!code.matches("\\d{" + EAN13_LEN + "}")) {
            throw new IllegalArgumentException("条码须为13位数字");
        }
        if (!isAllowedPrefix(code)) {
            throw new IllegalArgumentException("条码须以69（国标）或22～29（系统码）开头");
        }
        if (!isValidChecksum(code)) {
            throw new IllegalArgumentException("条码校验位不正确");
        }
        return code;
    }

    public static boolean isAllowedPrefix(String code13) {
        if (code13 == null || code13.length() < 2) {
            return false;
        }
        if (code13.startsWith("69")) {
            return true;
        }
        try {
            int prefix = Integer.parseInt(code13.substring(0, 2));
            return prefix >= 22 && prefix <= 29;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    /**
     * 13 位是否通过 ZXing EAN-13 校验（由 {@link EAN13Writer} 内部调用标准 checksum）。
     */
    public static boolean isValidChecksum(String code13) {
        if (code13 == null || code13.length() != EAN13_LEN || !code13.matches("\\d{" + EAN13_LEN + "}")) {
            return false;
        }
        try {
            new EAN13Writer().encode(code13);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * 前 12 位 → 校验数字符。通过 ZXing：对 0～9 试拼 13 位，由 {@link EAN13Writer} 判定 checksum。
     */
    public static char checksumDigit(String body12) {
        if (body12 == null || body12.length() != BODY12_LEN || !body12.matches("\\d{" + BODY12_LEN + "}")) {
            throw new IllegalArgumentException("校验位计算需要12位数字");
        }
        for (int d = 0; d <= 9; d++) {
            String candidate = body12 + d;
            try {
                new EAN13Writer().encode(candidate);
                return (char) ('0' + d);
            } catch (IllegalArgumentException ignored) {
                // 校验不通过，试下一位
            }
        }
        throw new IllegalArgumentException("无法计算校验位");
    }

    /**
     * 生成 EAN-13 条码图（黑白）。{@code code13} 须含正确校验位。
     */
    public static BufferedImage toImage(String code13, int width, int height) {
        String code = normalizeForSave(code13);
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("条码不能为空");
        }
        Map<EncodeHintType, Object> hints = new HashMap<>(2);
        hints.put(EncodeHintType.MARGIN, 2);
        try {
            BitMatrix matrix = new EAN13Writer().encode(code, BarcodeFormat.EAN_13, width, height, hints);
            return MatrixToImageWriter.toBufferedImage(matrix);
        } catch (Exception ex) {
            throw new IllegalArgumentException("条码图生成失败：" + ex.getMessage(), ex);
        }
    }
}
