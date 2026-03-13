package com.appsinnova.admin.business.common.utils;

import com.appsinnova.admin.common.utils.HttpServletUtil;
import com.appsinnova.admin.common.utils.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 通用方法工具类
 *
 * @author xie.zy
 * @date 2023/8/15
 */
public class ToolUtil {
    /**
     * 获取当前时间(单位秒)
     */
    public static int getCurrentTime() {
        return (int) (System.currentTimeMillis() / 1000);
    }

    public static Integer convertDateToInt(String s) {
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = simpleDateFormat.parse(s);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            String formattedDate = sdf.format(date);
            return Integer.parseInt(formattedDate);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将时间转换为时间戳
     */
    public static int dateToStamp(String s) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date date = simpleDateFormat.parse(s);
            return (int) (date.getTime() / 1000);
        } catch (ParseException e) {
            return 0;
        }
    }

    /**
     * 将时间转换为时间戳
     */
    public static Long dateToStamp2(String s) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date date = simpleDateFormat.parse(s);
            return date.getTime();
        } catch (ParseException e) {
            return 0L;
        }
    }

    /**
     * 将时间转换为时间戳
     */
    public static int dateToStamp(String s, String timezone) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone(timezone));
        try {
            Date date = simpleDateFormat.parse(s);
            return (int) (date.getTime() / 1000);
        } catch (ParseException e) {
            return 0;
        }
    }

    /**
     * 将时间戳转换为时间
     */
    public static String stampToDate(Integer i) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(i.longValue() * 1000);
        return simpleDateFormat.format(date);
    }

    /**
     * 将时间戳转换为时间
     */
    public static String stampToDate(Long i){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(i);
        return simpleDateFormat.format(date);
    }

    /**
     * 格式转换：en=11;ru=22转为map
     */
    public static Map<String, String> analyseText(String text) {
        Map<String, String> map = new LinkedHashMap<>();
        if (StringUtils.isBlank(text)) {
            return map;
        }
        String[] keyValues = text.split(";");
        for (String value : keyValues) {
            if (StringUtils.isNotBlank(value) && value.trim().contains("=")) {
                String keyValue = value.trim();
                String[] entry = keyValue.split("=");
                if (entry.length == 2) {
                    map.put(entry[0], entry[1]);
                }
            }
        }
        return map;
    }

    public static String analyseText(String text, String key) {
        Map<String, String> map = analyseText(text);
        if (StringUtils.isBlank(key) || !map.containsKey(key)) {
            return "";
        }

        return map.get(key);
    }

    public static String analyseKeyForShow(String text, String key) {
        Map<String, String> map = analyseText(text);
        if (map.isEmpty()) {
            return "";
        }

        if (StringUtils.isBlank(key) || !map.containsKey(key)) {
            // 返回第一个
            return map.keySet().iterator().next();
        }

        return key;
    }

    /**
     * 获取默认名称展示
     */
    public static String findTextForShow(String text, String secKey) {
        String value = "";
        if (StringUtils.isNotEmpty(text)) {
            List<Map<String, String>> names = JsonUtils.readValue(text, new TypeReference<List<Map<String, String>>>() {});
            Map<String, String> nameMap = new HashMap<>();
            for (Map<String, String> item : names) {
                String name = item.get(secKey);
                String lang = item.get("lang");
                nameMap.put(lang, name);
            }

            String key = "zh_CN";
            if (nameMap.containsKey(key)) {
                return nameMap.get(key);
            }

            key = "default";
            if (nameMap.containsKey(key)) {
                return nameMap.get(key);
            }

            key = "en";
            if (nameMap.containsKey(key)) {
                return nameMap.get(key);
            }
        }
        return value;
    }

    // 字符串转List
    public static List<String> strToList(String valueStr) {
        if (StringUtils.isBlank(valueStr)) {
            return new ArrayList<>();
        }

        List<String> list = new ArrayList<>();
        String[] array = valueStr.split(",");
        for (String value : array) {
            if (StringUtils.isBlank(value)) {
                continue;
            }
            list.add(value.trim());
        }
        return list;
    }

    /**
     * 下载操作
     */
    public static void download(String data, String fileName) {
        try {
            fileName = URLEncoder.encode(fileName, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        HttpServletResponse response = HttpServletUtil.getResponse();
        response.setCharacterEncoding("utf-8");
        response.setContentType("multipart/form-data");
        response.setHeader("Content-Disposition", "attachment;fileName=" + fileName);
        OutputStream ros = null;
        try {
            ros = response.getOutputStream();
            ros.write(data.getBytes());
            ros.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (ros != null) {
                try {
                    ros.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}