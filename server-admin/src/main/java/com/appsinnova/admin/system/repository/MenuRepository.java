package com.appsinnova.admin.system.repository;

import com.appsinnova.admin.common.constant.StatusConst;
import com.appsinnova.admin.system.domain.Menu;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author 小懒虫
 * @date 2018/8/14
 */
public interface MenuRepository extends BaseRepository<Menu, Long> {

    /**
     * 查找多个菜单
     * @param ids id列表
     */
    List<Menu> findByIdIn(List<Long> ids);

    /**
     * 查找响应状态的菜单
     * @param sort 排序对象
     */
    List<Menu> findAllByStatus(Sort sort, Byte status);

    /**
     * 查询菜单URL
     * @param url id列表
     */
    Menu findByUrl(String url);

    /**
     * 根据父ID查找子菜单
     * @param pids pid列表
     */
    List<Menu> findByPidsLikeAndStatus(String pids, Byte status);

    /**
     * 获取排序最大值
     * @param pid 父菜单ID
     */
    @Query("select max(sort) from Menu m where m.pid = ?1 and m.status <> " + StatusConst.DELETE)
    Integer findSortMax(long pid);

    /**
     * 根据父级菜单ID获取本级全部菜单
     * @param sort 排序对象
     * @param pid 父菜单ID
     * @param notId 需要排除的菜单ID
     */
    List<Menu> findByPidAndIdNot(Sort sort, long pid, long notId);

    /**
     * 取消菜单与角色之间的关系
     * @param id 菜单ID
     */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM sys_role_menu WHERE menu_id = ?1", nativeQuery = true)
    Integer cancelRoleJoin(Long id);
}
