package com.appsinnova.admin.business.service.sys;

import com.appsinnova.admin.business.domain.sys.AppNotice;
import com.appsinnova.admin.business.repository.sys.AppNoticeRepository;
import com.appsinnova.admin.common.data.PageSort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppNoticeService {

    private final AppNoticeRepository appNoticeRepository;

    public AppNotice getById(Long id) {
        return appNoticeRepository.findById(id).orElse(null);
    }

    public AppNotice getByType(Integer type) {
        return appNoticeRepository.findFirstByType(type);
    }

    public Page<AppNotice> getPageList(AppNotice param) {
        List<Sort.Order> orders = new ArrayList<>();
        orders.add(new Sort.Order(Sort.Direction.DESC, "updateTime"));
        PageRequest page = PageSort.pageRequest(orders);
        return appNoticeRepository.findAll((Root<AppNotice> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> preList = new ArrayList<>();
            if (param != null && param.getType() != null) {
                preList.add(cb.equal(root.get("type").as(Integer.class), param.getType()));
            }
            Predicate[] pres = new Predicate[preList.size()];
            return query.where(preList.toArray(pres)).getRestriction();
        }, page);
    }

    public AppNotice save(AppNotice entity) {
        if (entity.getId() == null) {
            entity.setCreateTime(System.currentTimeMillis());
        }
        entity.setUpdateTime(System.currentTimeMillis());
        return appNoticeRepository.save(entity);
    }

    @Transactional
    public void deleteByIdIn(List<Long> idList) {
        appNoticeRepository.deleteByIdIn(idList);
    }
}
