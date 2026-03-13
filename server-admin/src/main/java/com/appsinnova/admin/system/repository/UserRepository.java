package com.appsinnova.admin.system.repository;

import com.appsinnova.admin.system.domain.Dept;
import com.appsinnova.admin.system.domain.User;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * @author 小懒虫
 * @date 2018/8/14
 */
public interface UserRepository extends BaseRepository<User, Long>, JpaSpecificationExecutor<User> {

    /**
     * 根据用户名查询用户数据
     * @param username 用户名
     * @return 用户数据
     */
    User findByUsername(String username);

    /**
     * 根据用户名查询用户数据,且排查指定ID的用户
     * @param username 用户名
     * @param id 排除的用户ID
     * @return 用户数据
     */
    User findByUsernameAndIdNot(String username, Long id);

    /**
     * 查找多个相应部门的用户列表
     */
    List<User> findByDept(Dept dept);

    /**
     * 删除多条数据
     * @param ids     ID列表
     */
    Integer deleteByIdIn(List<Long> ids);
}
