package com.appsinnova.admin.system.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.appsinnova.admin.common.data.PageSort;
import com.appsinnova.admin.common.enums.StatusEnum;
import com.appsinnova.admin.system.domain.Role;
import com.appsinnova.admin.system.domain.User;
import com.appsinnova.admin.system.service.RoleService;
import com.appsinnova.admin.system.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author 小懒虫
 * @date 2018/8/14
 */
@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleRepository roleRepository;

    /**
     * 获取用户角色列表
     * @param id 用户ID
     */
    @Override
    @Transactional
    public Set<Role> getUserOkRoleList(Long id) {
        Byte status = StatusEnum.OK.getCode();
        return roleRepository.findByUsers_IdAndStatus(id, status);
    }

    /**
     * 获取用户一个角色id
     * @param id 用户ID
     */
    @Override
    @Transactional
    public Role getUserRoleId(Long id) {
        Byte status = StatusEnum.OK.getCode();
        Set<Role> roleList = roleRepository.findByUsers_IdAndStatus(id, status);
        return roleList.iterator().next();
    }

    /**
     * 判断指定的用户是否存在角色
     * @param id 用户ID
     */
    @Override
    public Boolean existsUserOk(Long id) {
        Byte status = StatusEnum.OK.getCode();
        return roleRepository.existsByUsers_IdAndStatus(id, status);
    }

    /**
     * 根据角色ID查询角色数据
     * @param id 角色ID
     */
    @Override
    @Transactional
    public Role getById(Long id) {
        return roleRepository.findById(id).orElse(null);
    }

    /**
     * 获取分页列表数据
     * @param example 查询实例
     * @return 返回分页数据
     */
    @Override
    public Page<Role> getPageList(Example<Role> example) {
        // 创建分页对象
        PageRequest page = PageSort.pageRequest(Sort.Direction.ASC);
        return roleRepository.findAll(example, page);
    }

    /**
     * 获取角色列表数据
     * @param sort 排序对象
     */
    @Override
    public List<Role> getListBySortOk(Sort sort) {
        return roleRepository.findAllByStatus(sort, StatusEnum.OK.getCode());
    }

    /**
     * 角色标识是否重复
     * @param role 角色实体类
     */
    @Override
    public boolean repeatByName(Role role) {
        Long id = role.getId() != null ? role.getId() : Long.MIN_VALUE;
        return roleRepository.findByNameAndIdNot(role.getName(), id) != null;
    }

    /**
     * 保存角色
     * @param role 角色实体类
     */
    @Override
    public Role save(Role role){
        return roleRepository.save(role);
    }

    /**
     * 状态(启用，冻结，删除)/批量状态处理
     */
    @Override
    @Transactional
    public Boolean updateStatus(StatusEnum statusEnum, List<Long> ids){
        // 删除角色时取消与角色和菜单的关联
        if(statusEnum == StatusEnum.DELETE){
            /*List<Role> roles = roleRepository.findByIdIn(ids);
            roles.forEach(role -> {
                role.setMenus(null);
                role.getUsers().forEach(user -> user.getRoles().remove(role));
            });*/

            // 非规范的Jpa操作，直接采用SQL语句
            roleRepository.cancelUserJoin(ids);
            roleRepository.cancelMenuJoin(ids);
        }
        return roleRepository.updateStatus(statusEnum.getCode(), ids) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> listActiveUsersByRoleName(String roleName) {
        if (roleName == null || roleName.isEmpty()) {
            return new ArrayList<>();
        }
        Role role = roleRepository.findByName(roleName);
        if (role == null || role.getUsers() == null) {
            return new ArrayList<>();
        }
        Byte ok = StatusEnum.OK.getCode();
        return role.getUsers().stream()
                .filter(u -> u.getStatus() != null && u.getStatus().equals(ok))
                .sorted(Comparator.comparing(User::getId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean userHasActiveRoleName(Long userId, String roleName) {
        if (userId == null || roleName == null || roleName.isEmpty()) {
            return false;
        }
        Role role = roleRepository.findByName(roleName);
        if (role == null || role.getUsers() == null) {
            return false;
        }
        Byte ok = StatusEnum.OK.getCode();
        return role.getUsers().stream()
                .anyMatch(u -> userId.equals(u.getId()) && u.getStatus() != null && u.getStatus().equals(ok));
    }
}
