package com.appsinnova.admin.common.data;

import com.appsinnova.admin.common.utils.HttpServletUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

/**
 * 分页排序数据
 *
 * @author 小懒虫
 * @date 2018/12/8
 */
public class PageSort {

    private static final Integer pageSizeDef = 30;
    private static final String orderByColumnDef = "id";
    private static final Sort.Direction sortDirection = Sort.Direction.DESC;

    /**
     * 创建分页排序对象
     */
    public static PageRequest pageRequest() {
        return pageRequest(pageSizeDef, orderByColumnDef, sortDirection);
    }

    /**
     * 创建分页排序对象
     */
    public static PageRequest pageRequest(Integer pageSize) {
        return pageRequest(pageSize, orderByColumnDef, sortDirection);
    }

    /**
     * 创建分页排序对象
     *
     * @param sortDirection 排序方式默认值
     */
    public static PageRequest pageRequest(Sort.Direction sortDirection) {
        return pageRequest(pageSizeDef, orderByColumnDef, sortDirection);
    }

    /**
     * 创建分页排序对象
     *
     * @param orderByColumnDef 排序字段名称默认值
     * @param sortDirection    排序方式默认值
     */
    public static PageRequest pageRequest(String orderByColumnDef, Sort.Direction sortDirection) {
        return pageRequest(pageSizeDef, orderByColumnDef, sortDirection);
    }

    /**
     * 创建分页排序对象
     *
     * @param pageSizeDef      分页数据数量默认值
     * @param orderByColumnDef 排序字段名称默认值
     * @param sortDirection    排序方式默认值
     */
    public static PageRequest pageRequest(Integer pageSizeDef, String orderByColumnDef, Sort.Direction sortDirection) {
        Integer pageIndex = HttpServletUtil.getParameterInt("page", 1);
        Integer pageSize = HttpServletUtil.getParameterInt("size", pageSizeDef);
        String orderByColumn = HttpServletUtil.getParameter("orderByColumn", orderByColumnDef);
        String direction = HttpServletUtil.getParameter("isAsc", sortDirection.toString());
        Sort sort = new Sort(Sort.Direction.fromString(direction), orderByColumn);
        return PageRequest.of(pageIndex - 1, pageSize, sort);
    }

    /**
     * 创建分页排序对象
     *
     * @param orderByColumnDef 排序字段名称默认值
     * @param sortDirection    排序方式默认值
     */
    public static PageRequest pageRequest(String orderByColumnDef, String orderByColumnDef1, Sort.Direction sortDirection) {
        return pageRequest(pageSizeDef, orderByColumnDef, orderByColumnDef1, sortDirection);
    }

    /**
     * 创建分页排序对象
     *
     * @param pageSizeDef      分页数据数量默认值
     * @param orderByColumnDef 排序字段名称默认值
     * @param sortDirection    排序方式默认值
     */
    public static PageRequest pageRequest(Integer pageSizeDef, String orderByColumnDef, String orderByColumnDef1, Sort.Direction sortDirection) {
        Integer pageIndex = HttpServletUtil.getParameterInt("page", 1);
        Integer pageSize = HttpServletUtil.getParameterInt("size", pageSizeDef);
        String orderByColumn = HttpServletUtil.getParameter("orderByColumn", orderByColumnDef);
        String orderByColumn1 = HttpServletUtil.getParameter("orderByColumn1", orderByColumnDef1);
        String direction = HttpServletUtil.getParameter("isAsc", sortDirection.toString());
        Sort sort = new Sort(Sort.Direction.fromString(direction), orderByColumn, orderByColumn1);
        return PageRequest.of(pageIndex - 1, pageSize, sort);
    }

    /**
     * 多字段排序
     */
    public static PageRequest pageRequest(List<Sort.Order> orders) {
        return pageRequest(pageSizeDef, orders);
    }

    /**
     * 多字段排序
     */
    public static PageRequest pageRequest(Integer pageSizeDef, List<Sort.Order> orders) {
        Integer pageIndex = HttpServletUtil.getParameterInt("page", 1);
        Integer pageSize = HttpServletUtil.getParameterInt("size", pageSizeDef);
        String orderByColumn = HttpServletUtil.getParameter("orderByColumn", "");
        String direction = HttpServletUtil.getParameter("isAsc", sortDirection.toString());
        Sort sort = Sort.by(orders);
        if (StringUtils.isNotBlank(orderByColumn)) {
            sort = new Sort(Sort.Direction.fromString(direction), orderByColumn);
        }

        return PageRequest.of(pageIndex - 1, pageSize, sort);
    }
}
