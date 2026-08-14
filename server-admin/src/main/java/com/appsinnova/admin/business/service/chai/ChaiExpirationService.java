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
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChaiExpirationService {

    private final ChaiExpirationRepository chaiExpirationRepository;

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

    @Transactional
    public void deleteByIdIn(List<Long> idList) {
        if (idList == null || idList.isEmpty()) {
            return;
        }
        chaiExpirationRepository.deleteByIdIn(idList);
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
