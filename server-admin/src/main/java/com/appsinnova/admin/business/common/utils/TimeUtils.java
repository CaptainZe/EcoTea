package com.appsinnova.admin.business.common.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class TimeUtils {

    public static String getDateYYMMdd(Long timestamp) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GTM+8"));
        Date date = new Date(timestamp);
        return simpleDateFormat.format(date);
    }
}