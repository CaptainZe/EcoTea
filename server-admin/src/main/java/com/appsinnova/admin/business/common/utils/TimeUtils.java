package com.appsinnova.admin.business.common.utils;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.TimeZone;

/**
 * 中国时区（GMT+8 / Asia/Shanghai）业务日工具。
 */
public final class TimeUtils {

    public static final ZoneId ZONE_CN = ZoneId.of("Asia/Shanghai");
    private static final TimeZone TIME_ZONE_CN = TimeZone.getTimeZone("GMT+8");
    private static final DateTimeFormatter DATE_YYYY_MM_DD = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private TimeUtils() {
    }

    /**
     * 中国时区业务日，格式 yyyyMMdd（如 20260907）。
     * 方法名历史为 YYMMdd，实际统一为 yyyyMMdd。
     */
    public static String getDateYYMMdd(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        simpleDateFormat.setTimeZone(TIME_ZONE_CN);
        return simpleDateFormat.format(new Date(timestamp));
    }

    /**
     * 解析 {@code yyyy-MM-dd}；空或非法返回 null。
     */
    public static LocalDate parseDateYyyyMmDd(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim(), DATE_YYYY_MM_DD);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    /**
     * 中国时区该日 00:00:00.000 的毫秒时间戳。
     */
    public static long startOfDayMs(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("date 不能为空");
        }
        return date.atStartOfDay(ZONE_CN).toInstant().toEpochMilli();
    }

    /**
     * 中国时区该日 23:59:59.999 的毫秒时间戳。
     */
    public static long endOfDayMs(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("date 不能为空");
        }
        return date.atTime(LocalTime.of(23, 59, 59, 999_000_000))
                .atZone(ZONE_CN)
                .toInstant()
                .toEpochMilli();
    }

    /**
     * {@code yyyy-MM-dd} → 当日开始毫秒；空或非法返回 null。
     */
    public static Long startOfDayMs(String dateYyyyMmDd) {
        LocalDate date = parseDateYyyyMmDd(dateYyyyMmDd);
        return date == null ? null : startOfDayMs(date);
    }

    /**
     * {@code yyyy-MM-dd} → 当日结束毫秒；空或非法返回 null。
     */
    public static Long endOfDayMs(String dateYyyyMmDd) {
        LocalDate date = parseDateYyyyMmDd(dateYyyyMmDd);
        return date == null ? null : endOfDayMs(date);
    }
}
