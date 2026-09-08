package com.appsinnova.admin.business.common.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class TimeUtils {

    private static final TimeZone ZONE_CN = TimeZone.getTimeZone("GMT+8");

    /**
     * 中国时区业务日，格式 yyyyMMdd（如 20260907）。
     * 方法名历史为 YYMMdd，实际统一为 yyyyMMdd。
     */
    public static String getDateYYMMdd(Long timestamp) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        simpleDateFormat.setTimeZone(ZONE_CN);
        Date date = new Date(timestamp);
        return simpleDateFormat.format(date);
    }
}
