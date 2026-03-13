package com.appsinnova.admin.component.thymeleaf.utility;

import com.appsinnova.admin.common.utils.EhCacheUtil;
import com.appsinnova.admin.common.utils.SpringContextUtil;
import com.appsinnova.admin.system.domain.Dict;
import com.appsinnova.admin.system.service.DictService;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 字典提取工具对象
 * @author 小懒虫
 * @date 2018/8/14
 */
public class DictUtil {

    private static Cache dictCache = EhCacheUtil.getDictCache();

    /**
     * 获取字典值集合
     * @param label 字典标识
     */
    public static Map<String, String> value(String label){
        Map<String, String> value = null;
        Element dictEle = dictCache.get(label);
        if(dictEle != null){
            value = (Map<String, String>) dictEle.getObjectValue();
        }else {
            DictService dictService = SpringContextUtil.getBean(DictService.class);
            Dict dict = dictService.getByNameOk(label);
            if(dict != null){
                if (dict.getType() == 2) {
                    String dictValue = dict.getValue();
                    String[] outerSplit = dictValue.split(",");
                    value = new LinkedHashMap<>();
                    for (String osp : outerSplit) {
                        String[] split = osp.split(":");
                        if (split.length > 1) {
                            value.put(split[0], split[1]);
                        }
                    }
                    dictCache.put(new Element(dict.getName(), value));
                } else {
                    String sqlValue = dict.getValue();
                    JdbcTemplate jdbcTemplate = SpringContextUtil.getBean(JdbcTemplate.class);
                    List<Map<String, String>> valueList = jdbcTemplate.query(sqlValue, (rs, i) -> {
                        Map<String, String> map = new HashMap<>();
                        map.put("value", rs.getString("value"));
                        map.put("text", rs.getString("text"));
                        return map;
                    });
                    final Map<String, String> finalValue = new LinkedHashMap<>();
                    valueList.forEach(map -> finalValue.put(map.get("value"), map.get("text")));
                    value = finalValue;
                    dictCache.put(new Element(dict.getName(), value, 5, 5));
                }
            }
        }
        return value;
    }

    /**
     * 根据选项编码获取选项值
     * @param label 字典标识
     * @param code 选项编码
     */
    public static String keyValue(String label, String code){
        Map<String, String> list = DictUtil.value(label);
        if(list != null){
            return list.get(code);
        }else{
            return "";
        }
    }

    /**
     * 封装数据状态字典
     * @param status 状态
     */
    public static String dataStatus(Byte status){
        String label = "DATA_STATUS";
        return DictUtil.keyValue(label, String.valueOf(status));
    }

    public static String dateFormat(String mills, String pattern) {
        long nowMills = 0;
        if (NumberUtils.isDigits(mills) && !StringUtils.equals(mills, "0")) {
            nowMills = NumberUtils.toLong(mills);
            if (nowMills < Integer.MAX_VALUE) {
                return DateFormatUtils.format(nowMills * 1000, pattern);
            } else {
                return DateFormatUtils.format(nowMills, pattern);
            }
        } else {
            return "";
        }
    }

    /**
     * 清除缓存中指定的数据
     * @param label 字典标识
     */
    public static void clearCache(String label){
        Element dictEle = dictCache.get(label);
        if (dictEle != null){
            dictCache.remove(label);
        }
    }
}
