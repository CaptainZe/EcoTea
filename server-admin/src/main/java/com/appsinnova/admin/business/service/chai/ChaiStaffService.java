package com.appsinnova.admin.business.service.chai;

import com.appsinnova.admin.business.domain.chai.ChaiStaff;
import com.appsinnova.admin.business.repository.chai.ChaiStaffRepository;
import com.appsinnova.admin.common.data.PageSort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChaiStaffService {

    private final ChaiStaffRepository chaiStaffRepository;

    public ChaiStaff getById(Long id) {
        return chaiStaffRepository.findById(id).orElse(null);
    }

    public Page<ChaiStaff> getPageList(ChaiStaff param) {
        List<Sort.Order> orders = new ArrayList<>();
        orders.add(new Sort.Order(Sort.Direction.DESC, "orderNum"));
        orders.add(new Sort.Order(Sort.Direction.DESC, "updateTime"));
        PageRequest page = PageSort.pageRequest(orders);
        return chaiStaffRepository.findAll((Root<ChaiStaff> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> preList = genCondition(root, cb, param);
            Predicate[] pres = new Predicate[preList.size()];
            return query.where(preList.toArray(pres)).getRestriction();
        }, page);
    }

    /**
     * 上架经手人，按排序号大到小
     */
    public List<ChaiStaff> listOnlineOrdered() {
        List<Sort.Order> orders = new ArrayList<>();
        orders.add(new Sort.Order(Sort.Direction.DESC, "orderNum"));
        orders.add(new Sort.Order(Sort.Direction.DESC, "id"));
        List<ChaiStaff> list = chaiStaffRepository.findAll(
                (Root<ChaiStaff> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
                        cb.equal(root.get("status").as(Integer.class), 1),
                Sort.by(orders));
        list.forEach(this::fillDisplayName);
        return list;
    }

    public boolean isNickNameTakenByOther(String nickName, Long excludeId) {
        if (!StringUtils.hasText(nickName)) {
            return false;
        }
        String trimmed = nickName.trim();
        if (excludeId == null) {
            return chaiStaffRepository.findFirstByNickName(trimmed).isPresent();
        }
        return chaiStaffRepository.existsByNickNameAndIdNot(trimmed, excludeId);
    }

    public boolean isReferencedByBill(Long staffId) {
        if (staffId == null) {
            return false;
        }
        return chaiStaffRepository.countBillsByHandlerId(staffId) > 0;
    }

    public ChaiStaff save(ChaiStaff entity) {
        if (entity.getId() == null) {
            entity.setCreateTime(System.currentTimeMillis());
        }
        entity.setUpdateTime(System.currentTimeMillis());
        return chaiStaffRepository.save(entity);
    }

    /**
     * 未被库存单据引用的才物理删。
     *
     * @return 被引用而未删除的展示名；空表示全部已删
     */
    @Transactional
    public List<String> deleteByIdIn(List<Long> idList) {
        if (idList == null || idList.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> seen = new LinkedHashSet<>();
        List<Long> toDelete = new ArrayList<>();
        List<String> blockedNames = new ArrayList<>();
        for (Long id : idList) {
            if (id == null || !seen.add(id)) {
                continue;
            }
            ChaiStaff staff = chaiStaffRepository.findById(id).orElse(null);
            if (staff == null) {
                continue;
            }
            if (isReferencedByBill(id)) {
                blockedNames.add(formatDisplayName(staff));
            } else {
                toDelete.add(id);
            }
        }
        if (toDelete.isEmpty() && !blockedNames.isEmpty()) {
            throw new IllegalArgumentException(formatAllBlocked(blockedNames));
        }
        if (!toDelete.isEmpty()) {
            chaiStaffRepository.deleteByIdIn(toDelete);
        }
        return blockedNames;
    }

    public void fillDisplayName(ChaiStaff staff) {
        if (staff != null) {
            staff.setDisplayName(formatDisplayName(staff));
        }
    }

    /**
     * 单据快照 / 下拉统一格式：真实姓名(花名)
     */
    public static String formatDisplayName(ChaiStaff staff) {
        if (staff == null) {
            return "";
        }
        String real = staff.getRealName() != null ? staff.getRealName().trim() : "";
        String nick = staff.getNickName() != null ? staff.getNickName().trim() : "";
        if (!real.isEmpty() && !nick.isEmpty()) {
            return real + "(" + nick + ")";
        }
        if (!real.isEmpty()) {
            return real;
        }
        return nick;
    }

    private static String formatAllBlocked(List<String> names) {
        if (names.size() == 1) {
            return "「" + names.get(0) + "」已被库存单据引用，无法删除，请先下架";
        }
        return "以下已被库存单据引用，无法删除，请先下架：" + String.join("、", names);
    }

    private List<Predicate> genCondition(Root<ChaiStaff> root, CriteriaBuilder cb, ChaiStaff param) {
        List<Predicate> preList = new ArrayList<>();
        if (param == null) {
            return preList;
        }
        if (StringUtils.hasText(param.getNickName())) {
            preList.add(cb.like(root.get("nickName").as(String.class), "%" + param.getNickName().trim() + "%"));
        }
        if (StringUtils.hasText(param.getRealName())) {
            preList.add(cb.like(root.get("realName").as(String.class), "%" + param.getRealName().trim() + "%"));
        }
        if (param.getStatus() != null) {
            preList.add(cb.equal(root.get("status").as(Integer.class), param.getStatus()));
        }
        return preList;
    }
}
