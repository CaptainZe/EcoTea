package com.appsinnova.admin.common.utils;

import com.appsinnova.admin.common.data.PageSort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.*;

/**
 * 页面数据构造工具
 */
public class PageUtil {
    /**
     * 构造一个空的 Page 对象, 用于返回给前端
     */
    public static <T> Page<T> emptyPage() {
        List<T> list = new ArrayList<>();
        PageRequest pageInfo = PageSort.pageRequest(100);
        return new PageImpl<>(list, pageInfo, 0);
    }

    public static <T> Page<T> genPage(T obj) {
        if (obj == null) {
            return emptyPage();
        }
        List<T> list = new ArrayList<>();
        list.add(obj);
        PageRequest pageInfo = PageSort.pageRequest(100);
        return new PageImpl<>(list, pageInfo, 1);
    }

    public static <T> Page<T> genPage(List<T> objList) {
        if (objList == null) {
            return emptyPage();
        }
        PageRequest pageInfo = PageSort.pageRequest(100);
        return new PageImpl<>(objList, pageInfo, objList.size());
    }

    public static <T> Page<T> genPage(List<T> objList, int total) {
        if (objList == null) {
            return emptyPage();
        }
        PageRequest pageInfo = PageSort.pageRequest(100);
        return new PageImpl<>(objList, pageInfo, total);
    }
}
