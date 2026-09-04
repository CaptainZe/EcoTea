package com.appsinnova.admin.business.service.wx;

import com.appsinnova.admin.business.domain.wx.WxGlobalConfig;
import com.appsinnova.admin.business.repository.wx.WxGlobalConfigRepository;
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
public class WxGlobalConfigService {

    private final WxGlobalConfigRepository wxGlobalConfigRepository;

    public WxGlobalConfig getById(Long id) {
        return wxGlobalConfigRepository.findById(id).orElse(null);
    }

    public WxGlobalConfig getByType(Integer type) {
        return wxGlobalConfigRepository.findFirstByType(type);
    }

    public Page<WxGlobalConfig> getPageList(WxGlobalConfig param) {
        List<Sort.Order> orders = new ArrayList<>();
        orders.add(new Sort.Order(Sort.Direction.ASC, "type"));
        orders.add(new Sort.Order(Sort.Direction.DESC, "updateTime"));
        PageRequest page = PageSort.pageRequest(orders);
        return wxGlobalConfigRepository.findAll((Root<WxGlobalConfig> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> preList = new ArrayList<>();
            if (param != null && param.getType() != null) {
                preList.add(cb.equal(root.get("type").as(Integer.class), param.getType()));
            }
            Predicate[] pres = new Predicate[preList.size()];
            return query.where(preList.toArray(pres)).getRestriction();
        }, page);
    }

    public WxGlobalConfig save(WxGlobalConfig entity) {
        if (entity.getId() == null) {
            entity.setCreateTime(System.currentTimeMillis());
        }
        entity.setUpdateTime(System.currentTimeMillis());
        return wxGlobalConfigRepository.save(entity);
    }

    @Transactional
    public void deleteByIdIn(List<Long> idList) {
        wxGlobalConfigRepository.deleteByIdIn(idList);
    }
}
