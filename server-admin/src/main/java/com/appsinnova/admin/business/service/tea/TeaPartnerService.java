package com.appsinnova.admin.business.service.tea;

import com.appsinnova.admin.business.domain.tea.TeaPartner;
import com.appsinnova.admin.business.repository.tea.TeaPartnerRepository;
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
public class TeaPartnerService {

    private final TeaPartnerRepository teaPartnerRepository;

    public TeaPartner getById(Long id) {
        return teaPartnerRepository.findById(id).orElse(null);
    }

    public Page<TeaPartner> getPageList(TeaPartner param) {
        List<Sort.Order> orders = new ArrayList<>();
        orders.add(new Sort.Order(Sort.Direction.DESC, "updateTime"));
        PageRequest page = PageSort.pageRequest(orders);
        return teaPartnerRepository.findAll((Root<TeaPartner> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> preList = genCondition(root, cb, param);
            Predicate[] pres = new Predicate[preList.size()];
            return query.where(preList.toArray(pres)).getRestriction();
        }, page);
    }

    /**
     * user_id 非 0 时是否已被其他合作方占用
     */
    public boolean isUserIdTakenByOther(Long userId, Long excludeId) {
        if (userId == null || userId <= 0) {
            return false;
        }
        if (excludeId == null) {
            return teaPartnerRepository.findFirstByUserId(userId).isPresent();
        }
        return teaPartnerRepository.existsByUserIdAndIdNot(userId, excludeId);
    }

    public TeaPartner save(TeaPartner entity) {
        if (entity.getId() == null) {
            entity.setCreateTime(System.currentTimeMillis());
        }
        entity.setUpdateTime(System.currentTimeMillis());
        return teaPartnerRepository.save(entity);
    }

    @Transactional
    public void deleteByIdIn(List<Long> idList) {
        teaPartnerRepository.deleteByIdIn(idList);
    }

    private List<Predicate> genCondition(Root<TeaPartner> root, CriteriaBuilder cb, TeaPartner param) {
        List<Predicate> preList = new ArrayList<>();
        if (param == null) {
            return preList;
        }
        if (param.getPartnerType() != null) {
            preList.add(cb.equal(root.get("partnerType").as(Integer.class), param.getPartnerType()));
        }
        if (param.getStatus() != null) {
            preList.add(cb.equal(root.get("status").as(Integer.class), param.getStatus()));
        }
        if (StringUtils.hasText(param.getPartnerName())) {
            preList.add(cb.like(root.get("partnerName").as(String.class), "%" + param.getPartnerName().trim() + "%"));
        }
        if (StringUtils.hasText(param.getContactName())) {
            preList.add(cb.like(root.get("contactName").as(String.class), "%" + param.getContactName().trim() + "%"));
        }
        if (StringUtils.hasText(param.getContactPhone())) {
            preList.add(cb.like(root.get("contactPhone").as(String.class), "%" + param.getContactPhone().trim() + "%"));
        }
        if (StringUtils.hasText(param.getProvince())) {
            preList.add(cb.like(root.get("province").as(String.class), "%" + param.getProvince().trim() + "%"));
        }
        if (StringUtils.hasText(param.getCity())) {
            preList.add(cb.like(root.get("city").as(String.class), "%" + param.getCity().trim() + "%"));
        }
        if (param.getUserId() != null && param.getUserId() > 0) {
            preList.add(cb.equal(root.get("userId").as(Long.class), param.getUserId()));
        }
        if (param.getLiaisonUserId() != null && param.getLiaisonUserId() > 0) {
            preList.add(cb.equal(root.get("liaisonUserId").as(Long.class), param.getLiaisonUserId()));
        }
        if (param.getUserLinked() != null) {
            Predicate notLinked = cb.or(
                    cb.isNull(root.get("userId")),
                    cb.equal(root.get("userId").as(Long.class), 0L));
            if (param.getUserLinked() == 0) {
                preList.add(notLinked);
            } else if (param.getUserLinked() == 1) {
                preList.add(cb.not(notLinked));
            }
        }
        return preList;
    }
}
