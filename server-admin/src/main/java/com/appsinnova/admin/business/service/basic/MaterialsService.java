package com.appsinnova.admin.business.service.basic;

import com.appsinnova.admin.business.domain.basic.MaterialsModel;
import com.appsinnova.admin.business.repository.basic.MaterialsRepository;
import com.appsinnova.admin.common.data.PageSort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaterialsService {

    private final MaterialsRepository materialsRepository;

    @Transactional
    public MaterialsModel getById(Long id) {
        return materialsRepository.findById(id).orElse(null);
    }

    public Page<MaterialsModel> getPageList(MaterialsModel param) {
        List<Sort.Order> orders = new ArrayList<>();
        orders.add(new Sort.Order(Sort.Direction.DESC, "updateTime"));
        PageRequest page = PageSort.pageRequest(orders);
        return materialsRepository.findAll((Root<MaterialsModel> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> preList = new ArrayList<>();

            if (param.getName() != null) {
                preList.add(cb.like(root.get("name").as(String.class), "%" + param.getName() + "%"));
            }
            if (param.getBrand() != null) {
                preList.add(cb.equal(root.get("brand").as(String.class), param.getBrand()));
            }
            if (param.getYear() != null) {
                preList.add(cb.equal(root.get("year").as(String.class), param.getYear()));
            }

            Predicate[] pres = new Predicate[preList.size()];
            return query.where(preList.toArray(pres)).getRestriction();
        }, page);
    }

    // 保存数据
    public MaterialsModel save(MaterialsModel model) {
        return materialsRepository.save(model);
    }

    @Transactional
    public void deleteByIdIn(List<Long> idList) {
        if (CollectionUtils.isEmpty(idList)) {
            return;
        }

        for (Long id : idList) {
            materialsRepository.deleteById(id);
        }
    }
}