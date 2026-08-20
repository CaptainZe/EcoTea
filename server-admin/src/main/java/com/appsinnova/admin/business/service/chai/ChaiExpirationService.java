package com.appsinnova.admin.business.service.chai;

import com.appsinnova.admin.business.domain.chai.ChaiExpiration;
import com.appsinnova.admin.business.repository.chai.ChaiExpirationRepository;
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
public class ChaiExpirationService {

    private final ChaiExpirationRepository chaiExpirationRepository;
    private final ChaiSpuService chaiSpuService;

    public ChaiExpiration getById(Long id) {
        return chaiExpirationRepository.findById(id).orElse(null);
    }

    public Page<ChaiExpiration> getPageList(ChaiExpiration param) {
        List<Sort.Order> orders = new ArrayList<>();
        orders.add(new Sort.Order(Sort.Direction.DESC, "orderNum"));
        orders.add(new Sort.Order(Sort.Direction.DESC, "updateTime"));
        PageRequest page = PageSort.pageRequest(orders);
        return chaiExpirationRepository.findAll((Root<ChaiExpiration> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> preList = genCondition(root, cb, param);
            Predicate[] pres = new Predicate[preList.size()];
            return query.where(preList.toArray(pres)).getRestriction();
        }, page);
    }

    public ChaiExpiration getByName(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        return chaiExpirationRepository.findFirstByName(name.trim()).orElse(null);
    }

    /**
     * 显示名是否已被其他记录占用
     */
    public boolean isNameTakenByOther(String name, Long excludeId) {
        if (!StringUtils.hasText(name)) {
            return false;
        }
        String trimmed = name.trim();
        if (excludeId == null) {
            return chaiExpirationRepository.findFirstByName(trimmed).isPresent();
        }
        return chaiExpirationRepository.existsByNameAndIdNot(trimmed, excludeId);
    }

    public ChaiExpiration save(ChaiExpiration entity) {
        if (entity.getId() == null) {
            entity.setCreateTime(System.currentTimeMillis());
        }
        entity.setUpdateTime(System.currentTimeMillis());
        return chaiExpirationRepository.save(entity);
    }

    /**
     * 上架保质期，按排序号大到小
     */
    public List<ChaiExpiration> listOnlineOrdered() {
        List<Sort.Order> orders = new ArrayList<>();
        orders.add(new Sort.Order(Sort.Direction.DESC, "orderNum"));
        orders.add(new Sort.Order(Sort.Direction.DESC, "id"));
        return chaiExpirationRepository.findAll((Root<ChaiExpiration> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
                cb.equal(root.get("status").as(Integer.class), 1), Sort.by(orders));
    }

    /**
     * 未被商品引用的才物理删。
     *
     * @return 被引用而未删除的名称；空表示全部已删
     * @throws IllegalArgumentException 全部被引用，一条都未删
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
            ChaiExpiration expiration = chaiExpirationRepository.findById(id).orElse(null);
            if (expiration == null) {
                continue;
            }
            if (chaiSpuService.isExpirationInUse(id)) {
                blockedNames.add(expiration.getName());
            } else {
                toDelete.add(id);
            }
        }
        if (toDelete.isEmpty() && !blockedNames.isEmpty()) {
            throw new IllegalArgumentException(formatAllBlocked(blockedNames));
        }
        if (!toDelete.isEmpty()) {
            chaiExpirationRepository.deleteByIdIn(toDelete);
        }
        return blockedNames;
    }

    private static String formatAllBlocked(List<String> names) {
        if (names.size() == 1) {
            return "「" + names.get(0) + "」已被商品引用，无法删除，请先下架";
        }
        return "以下已被商品引用，无法删除，请先下架：" + String.join("、", names);
    }

    private List<Predicate> genCondition(Root<ChaiExpiration> root, CriteriaBuilder cb, ChaiExpiration param) {
        List<Predicate> preList = new ArrayList<>();
        if (param == null) {
            return preList;
        }
        if (StringUtils.hasText(param.getName())) {
            preList.add(cb.like(root.get("name").as(String.class), "%" + param.getName().trim() + "%"));
        }
        if (param.getMonths() != null) {
            preList.add(cb.equal(root.get("months").as(Integer.class), param.getMonths()));
        }
        if (param.getStatus() != null) {
            preList.add(cb.equal(root.get("status").as(Integer.class), param.getStatus()));
        }
        return preList;
    }
}
