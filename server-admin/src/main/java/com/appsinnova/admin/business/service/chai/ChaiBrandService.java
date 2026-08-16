package com.appsinnova.admin.business.service.chai;

import com.appsinnova.admin.business.domain.chai.ChaiBrand;
import com.appsinnova.admin.business.repository.chai.ChaiBrandRepository;
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
public class ChaiBrandService {

    private final ChaiBrandRepository chaiBrandRepository;

    public ChaiBrand getById(Long id) {
        return chaiBrandRepository.findById(id).orElse(null);
    }

    public Page<ChaiBrand> getPageList(ChaiBrand param) {
        List<Sort.Order> orders = new ArrayList<>();
        // 排序号大到小，其次按更新时间
        orders.add(new Sort.Order(Sort.Direction.DESC, "orderNum"));
        orders.add(new Sort.Order(Sort.Direction.DESC, "updateTime"));
        PageRequest page = PageSort.pageRequest(orders);
        return chaiBrandRepository.findAll((Root<ChaiBrand> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> preList = genCondition(root, cb, param);
            Predicate[] pres = new Predicate[preList.size()];
            return query.where(preList.toArray(pres)).getRestriction();
        }, page);
    }

    public ChaiBrand getByName(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        return chaiBrandRepository.findFirstByName(name.trim()).orElse(null);
    }

    /**
     * 品牌名称是否已被其他记录占用
     */
    public boolean isNameTakenByOther(String name, Long excludeId) {
        if (!StringUtils.hasText(name)) {
            return false;
        }
        String trimmed = name.trim();
        if (excludeId == null) {
            return chaiBrandRepository.findFirstByName(trimmed).isPresent();
        }
        return chaiBrandRepository.existsByNameAndIdNot(trimmed, excludeId);
    }

    public ChaiBrand save(ChaiBrand entity) {
        if (entity.getId() == null) {
            entity.setCreateTime(System.currentTimeMillis());
        }
        entity.setUpdateTime(System.currentTimeMillis());
        return chaiBrandRepository.save(entity);
    }

    /**
     * 上架品牌，按排序号大到小
     */
    public List<ChaiBrand> listOnlineOrdered() {
        List<Sort.Order> orders = new ArrayList<>();
        orders.add(new Sort.Order(Sort.Direction.DESC, "orderNum"));
        orders.add(new Sort.Order(Sort.Direction.DESC, "id"));
        return chaiBrandRepository.findAll((Root<ChaiBrand> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
                cb.equal(root.get("status").as(Integer.class), 1), Sort.by(orders));
    }

    @Transactional
    public void deleteByIdIn(List<Long> idList) {
        if (idList == null || idList.isEmpty()) {
            return;
        }
        chaiBrandRepository.deleteByIdIn(idList);
    }

    private List<Predicate> genCondition(Root<ChaiBrand> root, CriteriaBuilder cb, ChaiBrand param) {
        List<Predicate> preList = new ArrayList<>();
        if (param == null) {
            return preList;
        }
        if (StringUtils.hasText(param.getName())) {
            preList.add(cb.like(root.get("name").as(String.class), "%" + param.getName().trim() + "%"));
        }
        if (param.getStatus() != null) {
            preList.add(cb.equal(root.get("status").as(Integer.class), param.getStatus()));
        }
        return preList;
    }
}
