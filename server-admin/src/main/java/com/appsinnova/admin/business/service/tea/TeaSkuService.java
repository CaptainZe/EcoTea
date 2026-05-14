package com.appsinnova.admin.business.service.tea;

import com.appsinnova.admin.business.common.utils.SkuUtil;
import com.appsinnova.admin.business.domain.tea.TeaSku;
import com.appsinnova.admin.business.repository.tea.TeaSkuRepository;
import com.appsinnova.admin.common.data.PageSort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TeaSkuService {

    private final TeaSkuRepository teaSkuRepository;

    public TeaSku getById(Long id) {
        return teaSkuRepository.findById(id).orElse(null);
    }

    public List<TeaSku> getByIdIn(List<Long> idList) {
        if (CollectionUtils.isEmpty(idList)) {
            return new ArrayList<>();
        }
        return teaSkuRepository.findByIdIn(idList);
    }

    public Page<TeaSku> getPageList(TeaSku param) {
        List<Sort.Order> orders = new ArrayList<>();
        orders.add(new Sort.Order(Sort.Direction.DESC, "updateTime"));
        PageRequest page = PageSort.pageRequest(orders);
        return teaSkuRepository.findAll((Root<TeaSku> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> preList = this.genCondition(root, cb, param);
            Predicate[] pres = new Predicate[preList.size()];
            return query.where(preList.toArray(pres)).getRestriction();
        }, page);
    }

    public TeaSku save(TeaSku entity) {
        boolean isCreate = false;
        if (entity.getId() == null) {
            entity.setSkuCode("");
            entity.setCreateTime(System.currentTimeMillis());
            isCreate = true;
        }
        entity.setUpdateTime(System.currentTimeMillis());
        entity = teaSkuRepository.save(entity);

        // 创建时，生成SKU编码
        if (isCreate) {
            entity.setSkuCode(SkuUtil.genTeaSkuCode(entity.getBrand(), entity.getId()));
        }
        entity = teaSkuRepository.save(entity);
        return entity;
    }

    @Transactional
    public void deleteByIdIn(List<Long> idList) {
        if (CollectionUtils.isEmpty(idList)) {
            return;
        }
        teaSkuRepository.deleteByIdIn(idList);
    }

    private List<Predicate> genCondition(Root<TeaSku> root, CriteriaBuilder cb, TeaSku param) {
        List<Predicate> preList = new ArrayList<>();
        if (param == null) {
            return preList;
        }

        if (StringUtils.hasText(param.getSkuCode())) {
            preList.add(cb.equal(root.get("skuCode").as(String.class), param.getSkuCode()));
        }
        if (param.getBrand() != null) {
            preList.add(cb.equal(root.get("brand").as(Integer.class), param.getBrand()));
        }
        if (param.getType() != null) {
            preList.add(cb.equal(root.get("type").as(Integer.class), param.getType()));
        }
        if (param.getGrade() != null) {
            preList.add(cb.equal(root.get("grade").as(Integer.class), param.getGrade()));
        }
        if (param.getYear() != null) {
            preList.add(cb.equal(root.get("year").as(Integer.class), param.getYear()));
        }
        if (param.getExpiration() != null) {
            preList.add(cb.equal(root.get("expiration").as(Integer.class), param.getExpiration()));
        }
        if (param.getStatus() != null) {
            preList.add(cb.equal(root.get("status").as(Integer.class), param.getStatus()));
        }
        if (param.getStarLevel() != null) {
            preList.add(cb.equal(root.get("starLevel").as(Integer.class), param.getStarLevel()));
        }
        if (StringUtils.hasText(param.getBarcode())) {
            preList.add(cb.equal(root.get("barcode").as(String.class), param.getBarcode()));
        }
        if (StringUtils.hasText(param.getName())) {
            preList.add(cb.like(root.get("name").as(String.class), "%" + param.getName() + "%"));
        }
        if (param.getImageConfigured() != null) {
            Predicate noImage = cb.or(
                    cb.isNull(root.get("imageUrls")),
                    cb.equal(root.get("imageUrls").as(String.class), ""),
                    cb.equal(root.get("imageUrls").as(String.class), "[]"));
            if (param.getImageConfigured() == 0) {
                preList.add(noImage);
            } else if (param.getImageConfigured() == 1) {
                preList.add(cb.not(noImage));
            }
        }
        if (param.getRealImageConfigured() != null) {
            Predicate noRealImage = cb.or(
                    cb.isNull(root.get("realImageUrls")),
                    cb.equal(root.get("realImageUrls").as(String.class), ""),
                    cb.equal(root.get("realImageUrls").as(String.class), "[]"));
            if (param.getRealImageConfigured() == 0) {
                preList.add(noRealImage);
            } else if (param.getRealImageConfigured() == 1) {
                preList.add(cb.not(noRealImage));
            }
        }

        return preList;
    }
}