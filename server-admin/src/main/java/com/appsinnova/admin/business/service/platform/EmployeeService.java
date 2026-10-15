package com.appsinnova.admin.business.service.platform;

import com.appsinnova.admin.business.common.enums.platform.EmployeePermCode;
import com.appsinnova.admin.business.common.enums.platform.EmployeeStatus;
import com.appsinnova.admin.business.domain.platform.Employee;
import com.appsinnova.admin.business.domain.platform.EmployeeTenant;
import com.appsinnova.admin.business.domain.platform.EmployeeTenantPerm;
import com.appsinnova.admin.business.domain.platform.Tenant;
import com.appsinnova.admin.business.repository.platform.EmployeeRepository;
import com.appsinnova.admin.business.repository.platform.EmployeeTenantPermRepository;
import com.appsinnova.admin.business.repository.platform.EmployeeTenantRepository;
import com.appsinnova.admin.business.repository.platform.TenantRepository;
import com.appsinnova.admin.common.data.PageSort;
import com.appsinnova.admin.component.shiro.ShiroUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeTenantRepository employeeTenantRepository;
    private final EmployeeTenantPermRepository employeeTenantPermRepository;
    private final TenantRepository tenantRepository;
    private final EmployeeSessionCacheService employeeSessionCacheService;

    public Employee getById(Long id) {
        return employeeRepository.findById(id).orElse(null);
    }

    public boolean isUsernameTakenByOther(String username, Long excludeId) {
        if (!StringUtils.hasText(username)) {
            return false;
        }
        if (excludeId == null) {
            return employeeRepository.findFirstByUsername(username).isPresent();
        }
        return employeeRepository.existsByUsernameAndIdNot(username, excludeId);
    }

    public Page<Employee> getPageList(Employee param) {
        List<Sort.Order> orders = new ArrayList<>();
        orders.add(new Sort.Order(Sort.Direction.DESC, "updateTime"));
        PageRequest page = PageSort.pageRequest(orders);
        Page<Employee> result = employeeRepository.findAll((root, query, cb) -> {
            List<Predicate> preList = new ArrayList<>();
            if (param != null) {
                if (StringUtils.hasText(param.getUsername())) {
                    preList.add(cb.like(root.get("username"), "%" + param.getUsername().trim() + "%"));
                }
                if (StringUtils.hasText(param.getRealName())) {
                    preList.add(cb.like(root.get("realName"), "%" + param.getRealName().trim() + "%"));
                }
                if (StringUtils.hasText(param.getMobile())) {
                    preList.add(cb.like(root.get("mobile"), "%" + param.getMobile().trim() + "%"));
                }
                if (param.getStatus() != null) {
                    preList.add(cb.equal(root.get("status"), param.getStatus()));
                }
                if (param.getFilterTenantId() != null) {
                    Subquery<Long> sq = query.subquery(Long.class);
                    Root<EmployeeTenant> et = sq.from(EmployeeTenant.class);
                    sq.select(et.get("employeeId")).where(
                            cb.equal(et.get("employeeId"), root.get("id")),
                            cb.equal(et.get("tenantId"), param.getFilterTenantId())
                    );
                    preList.add(cb.exists(sq));
                }
            }
            return query.where(preList.toArray(new Predicate[0])).getRestriction();
        }, page);
        fillTenantSummaries(result.getContent());
        return result;
    }

    public void fillTenantSummaries(List<Employee> employees) {
        if (CollectionUtils.isEmpty(employees)) {
            return;
        }
        List<Long> ids = employees.stream().map(Employee::getId).collect(Collectors.toList());
        List<EmployeeTenant> binds = employeeTenantRepository.findByEmployeeIdIn(ids);
        Map<Long, String> tenantNames = loadTenantNameMap();
        Map<Long, List<EmployeeTenant>> byEmp = binds.stream()
                .collect(Collectors.groupingBy(EmployeeTenant::getEmployeeId));
        for (Employee emp : employees) {
            List<EmployeeTenant> list = byEmp.getOrDefault(emp.getId(), Collections.emptyList());
            if (list.isEmpty()) {
                emp.setTenantSummary("未挂店");
                continue;
            }
            List<String> parts = new ArrayList<>();
            for (EmployeeTenant et : list) {
                String name = tenantNames.getOrDefault(et.getTenantId(), String.valueOf(et.getTenantId()));
                String flag = EmployeeStatus.isEnabled(et.getStatus()) ? "" : "(停)";
                parts.add(name + flag);
            }
            emp.setTenantSummary(String.join("、", parts));
        }
    }

    public List<EmployeeTenant> listBindsWithPerms(Long employeeId) {
        List<EmployeeTenant> binds = employeeTenantRepository.findByEmployeeIdOrderByTenantIdAsc(employeeId);
        Map<Long, String> tenantNames = loadTenantNameMap();
        Map<Long, List<String>> permsByTenant = employeeTenantPermRepository.findByEmployeeId(employeeId).stream()
                .collect(Collectors.groupingBy(
                        EmployeeTenantPerm::getTenantId,
                        Collectors.mapping(EmployeeTenantPerm::getPermCode, Collectors.toList())));
        for (EmployeeTenant et : binds) {
            et.setTenantName(tenantNames.get(et.getTenantId()));
            et.setPermCodes(new ArrayList<>(permsByTenant.getOrDefault(et.getTenantId(), Collections.emptyList())));
        }
        return binds;
    }

    public Map<Long, String> loadTenantNameMap() {
        Map<Long, String> map = new HashMap<>();
        for (Tenant t : tenantRepository.findAll()) {
            map.put(t.getTenantId(), t.getName());
        }
        return map;
    }

    public List<Tenant> listAllTenants() {
        return tenantRepository.findAll().stream()
                .sorted(Comparator.comparing(Tenant::getTenantId))
                .collect(Collectors.toList());
    }

    public long countOccupied(Long tenantId) {
        return employeeTenantRepository.countOccupied(tenantId);
    }

    /**
     * 保存账号。新建须带明文密码；编辑时空密码表示不改。
     * 新建时可带首店挂店（createBind），用于「建号+挂店」。
     */
    @Transactional
    public Employee saveAccount(Employee entity, String plainPassword, ShopBindItem createBind, String operator) {
        boolean isNew = entity.getId() == null;
        Employee db = null;
        if (!isNew) {
            db = getById(entity.getId());
            if (db == null) {
                throw new IllegalArgumentException("编辑的数据记录不存在");
            }
        }

        Integer newStatus = entity.getStatus();
        if (newStatus == null) {
            throw new IllegalArgumentException("状态必选");
        }

        if (isNew) {
            if (!StringUtils.hasText(plainPassword)) {
                throw new IllegalArgumentException("新建员工须设置密码");
            }
            if (createBind == null || createBind.getTenantId() == null) {
                throw new IllegalArgumentException("新建员工须挂至少一个店铺");
            }
            assertCanOccupy(createBind.getTenantId(), true, newStatus, createBind.getStatus(), false);
        } else {
            // 员工从停用→启用：校验其所有启用挂店的配额
            if (!EmployeeStatus.isEnabled(db.getStatus()) && EmployeeStatus.isEnabled(newStatus)) {
                List<EmployeeTenant> binds = employeeTenantRepository.findByEmployeeIdOrderByTenantIdAsc(db.getId());
                for (EmployeeTenant et : binds) {
                    if (EmployeeStatus.isEnabled(et.getStatus())) {
                        assertCanOccupy(et.getTenantId(), true, newStatus, et.getStatus(), false);
                    }
                }
            }
        }

        long now = System.currentTimeMillis();
        if (isNew) {
            entity.setCreateTime(now);
            entity.setWxOpenid(null);
            entity.setWxUnionid(null);
            entity.setWxBindTime(0L);
            applyPassword(entity, plainPassword);
        } else {
            entity.setUsername(db.getUsername()); // 登录名创建后不可改
            entity.setCreateTime(db.getCreateTime());
            entity.setWxOpenid(db.getWxOpenid());
            entity.setWxUnionid(db.getWxUnionid());
            entity.setWxBindTime(db.getWxBindTime() == null ? 0L : db.getWxBindTime());
            if (StringUtils.hasText(plainPassword)) {
                applyPassword(entity, plainPassword);
            } else {
                entity.setPassword(db.getPassword());
                entity.setSalt(db.getSalt());
            }
        }
        entity.setOperator(operator == null ? "" : operator);
        entity.setUpdateTime(now);
        if (entity.getMobile() == null) {
            entity.setMobile("");
        }
        if (entity.getRealName() == null) {
            entity.setRealName("");
        }
        Employee saved = employeeRepository.save(entity);

        if (isNew && createBind != null) {
            replaceSingleBind(saved.getId(), createBind, operator, now, saved.getStatus());
        }

        // 改密或改状态 → 踢小程序会话（新建无会话，invalidate 为空操作）
        boolean passwordChanged = isNew || StringUtils.hasText(plainPassword);
        boolean statusChanged = !isNew && db != null && !Objects.equals(db.getStatus(), newStatus);
        if (passwordChanged || statusChanged) {
            employeeSessionCacheService.invalidateByEmployeeId(saved.getId());
        }
        return saved;
    }

    @Transactional
    public void updateStatus(Long id, Integer status, String operator) {
        Employee emp = getById(id);
        if (emp == null) {
            return;
        }
        if (status.equals(emp.getStatus())) {
            return;
        }
        if (EmployeeStatus.isEnabled(status)) {
            List<EmployeeTenant> binds = employeeTenantRepository.findByEmployeeIdOrderByTenantIdAsc(id);
            for (EmployeeTenant et : binds) {
                if (EmployeeStatus.isEnabled(et.getStatus())) {
                    assertCanOccupy(et.getTenantId(), true, status, et.getStatus(), false);
                }
            }
        }
        emp.setStatus(status);
        emp.setOperator(operator == null ? "" : operator);
        emp.setUpdateTime(System.currentTimeMillis());
        employeeRepository.save(emp);
        employeeSessionCacheService.invalidateByEmployeeId(id);
    }

    /**
     * 整员工覆盖挂店与权限。空列表表示解绑全部。
     */
    @Transactional
    public void replaceBinds(Long employeeId, List<ShopBindItem> binds, String operator) {
        Employee emp = getById(employeeId);
        if (emp == null) {
            throw new IllegalArgumentException("员工不存在");
        }
        List<ShopBindItem> items = binds == null ? Collections.emptyList() : binds;
        LinkedHashMap<Long, ShopBindItem> unique = new LinkedHashMap<>();
        for (ShopBindItem item : items) {
            if (item == null || item.getTenantId() == null || item.getTenantId() <= 0) {
                throw new IllegalArgumentException("店铺租户ID无效");
            }
            if (item.getStatus() == null) {
                item.setStatus(EmployeeStatus.ENABLED.getCode());
            }
            unique.put(item.getTenantId(), item);
        }

        Map<Long, EmployeeTenant> oldByTenant = employeeTenantRepository
                .findByEmployeeIdOrderByTenantIdAsc(employeeId).stream()
                .collect(Collectors.toMap(EmployeeTenant::getTenantId, x -> x, (a, b) -> a));

        for (ShopBindItem item : unique.values()) {
            Tenant tenant = tenantRepository.findFirstByTenantId(item.getTenantId())
                    .orElseThrow(() -> new IllegalArgumentException("店铺不存在: " + item.getTenantId()));
            EmployeeTenant old = oldByTenant.get(item.getTenantId());
            boolean alreadyOccupying = old != null
                    && EmployeeStatus.isEnabled(emp.getStatus())
                    && EmployeeStatus.isEnabled(old.getStatus());
            assertCanOccupy(tenant.getTenantId(), true, emp.getStatus(), item.getStatus(), alreadyOccupying);
            validatePermCodes(item.getPermCodes());
        }

        long now = System.currentTimeMillis();
        employeeTenantPermRepository.deleteByEmployeeId(employeeId);
        employeeTenantRepository.deleteByEmployeeId(employeeId);

        for (ShopBindItem item : unique.values()) {
            persistBind(employeeId, item, operator, now);
        }
        // 挂店/perm 变更 → 踢会话，避免旧 permCodes 残留
        employeeSessionCacheService.invalidateByEmployeeId(employeeId);
    }

    private void replaceSingleBind(Long employeeId, ShopBindItem item, String operator, long now, Integer empStatus) {
        validatePermCodes(item.getPermCodes());
        if (item.getStatus() == null) {
            item.setStatus(EmployeeStatus.ENABLED.getCode());
        }
        Tenant tenant = tenantRepository.findFirstByTenantId(item.getTenantId())
                .orElseThrow(() -> new IllegalArgumentException("店铺不存在: " + item.getTenantId()));
        assertCanOccupy(tenant.getTenantId(), true, empStatus, item.getStatus(), false);
        persistBind(employeeId, item, operator, now);
    }

    private void persistBind(Long employeeId, ShopBindItem item, String operator, long now) {
        EmployeeTenant et = new EmployeeTenant();
        et.setEmployeeId(employeeId);
        et.setTenantId(item.getTenantId());
        et.setStatus(item.getStatus());
        et.setOperator(operator == null ? "" : operator);
        et.setCreateTime(now);
        et.setUpdateTime(now);
        employeeTenantRepository.save(et);

        if (!CollectionUtils.isEmpty(item.getPermCodes())) {
            for (String code : new LinkedHashSet<>(item.getPermCodes())) {
                if (!StringUtils.hasText(code)) {
                    continue;
                }
                EmployeeTenantPerm perm = new EmployeeTenantPerm();
                perm.setEmployeeId(employeeId);
                perm.setTenantId(item.getTenantId());
                perm.setPermCode(code.trim());
                perm.setOperator(operator == null ? "" : operator);
                perm.setCreateTime(now);
                perm.setUpdateTime(now);
                employeeTenantPermRepository.save(perm);
            }
        }
    }

    /**
     * @param alreadyOccupying 该员工是否已计入该店占用（启用员工+启用挂店）
     */
    public void assertCanOccupy(Long tenantId, boolean checkTenantExists,
                                Integer empStatus, Integer etStatus, boolean alreadyOccupying) {
        boolean willOccupy = EmployeeStatus.isEnabled(empStatus) && EmployeeStatus.isEnabled(etStatus);
        if (!willOccupy || alreadyOccupying) {
            return;
        }
        Tenant tenant = tenantRepository.findFirstByTenantId(tenantId).orElse(null);
        if (tenant == null) {
            if (checkTenantExists) {
                throw new IllegalArgumentException("店铺不存在: " + tenantId);
            }
            return;
        }
        int quota = tenant.getEmployeeQuota() == null ? 0 : tenant.getEmployeeQuota();
        if (quota <= 0) {
            throw new IllegalArgumentException("店铺「" + tenant.getName() + "」员工名额为 0，无法再挂启用员工");
        }
        long occupied = countOccupied(tenantId);
        if (occupied >= quota) {
            throw new IllegalArgumentException("店铺「" + tenant.getName() + "」员工名额已满（"
                    + occupied + "/" + quota + "）");
        }
    }

    private void validatePermCodes(List<String> permCodes) {
        if (CollectionUtils.isEmpty(permCodes)) {
            return;
        }
        for (String code : permCodes) {
            if (!StringUtils.hasText(code)) {
                continue;
            }
            if (!EmployeePermCode.isValid(code.trim())) {
                throw new IllegalArgumentException("无效权限码: " + code);
            }
        }
    }

    private void applyPassword(Employee entity, String plainPassword) {
        String salt = ShiroUtil.getRandomSalt();
        entity.setSalt(salt);
        entity.setPassword(ShiroUtil.encrypt(plainPassword.trim(), salt));
    }

    @Data
    public static class ShopBindItem {
        private Long tenantId;
        private Integer status;
        private List<String> permCodes;
    }
}
