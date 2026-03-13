package com.appsinnova.admin.business.common.utils;

public class DateUtil {

    /**
     * 秒转换为小时格式  11:00:00
     */
    public static String formatHour(int seconds) {
        int hour = seconds / 3600;
        int minute = seconds % 3600 / 60;
        int second = seconds % 60;
        return DateUtil.numToTime(hour) + ":" + DateUtil.numToTime(minute) + ":" + DateUtil.numToTime(second);
    }

    /**
     * 2位以内整数转成字符串
     */
    public static String numToTime(int num) {
        if (num == 0) {
            return "00";
        } else if (num < 10L) {
            return "0" + num;
        }

        return num + "";
    }

    /**
     * 转换时间  04:00 -> 14400
     */
    public static Integer convertTime(String timeStr) {
        try {
            int product = 1;
            if (timeStr.startsWith("-")) {
                timeStr = timeStr.substring(1);
                product = -1;
            }
            int second = 0;
            String[] outerSplit = timeStr.split(":");
            if (outerSplit.length >= 2) {
                if (outerSplit[0] != null && outerSplit[1] != null) {
                    String hourStr = outerSplit[0];
                    String minuteStr = outerSplit[1];
                    if (hourStr.startsWith("0")) {
                        hourStr = hourStr.substring(1);
                    }
                    if (minuteStr.startsWith("0")) {
                        minuteStr = minuteStr.substring(1);
                    }
                    second = Integer.parseInt(hourStr) * 3600;
                    second += Integer.parseInt(minuteStr) * 60;
                }
                if (outerSplit[2] != null) {
                    String secondStr = outerSplit[2];
                    if (secondStr.startsWith("0")) {
                        secondStr = secondStr.substring(1);
                    }
                    second += Integer.parseInt(secondStr);
                }
            }
            second = second * product;
            return second;
        } catch (Exception ignored) {
        }

        return 0;
    }
}