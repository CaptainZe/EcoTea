package com.appsinnova.admin.business.service.wx;

import com.appsinnova.admin.business.domain.wx.WxMpMenu;
import com.appsinnova.admin.business.repository.wx.WxMpMenuRepository;
import com.appsinnova.admin.common.data.PageSort;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
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
public class WxMpMenuService {

    private final WxMpMenuRepository wxMpMenuRepository;

    public WxMpMenu getById(Long id) {
        return wxMpMenuRepository.findById(id).orElse(null);
    }

    public WxMpMenu getByAppId(String appId) {
        if (StringUtils.isBlank(appId)) {
            return null;
        }
        return wxMpMenuRepository.findFirstByAppId(appId.trim());
    }

    public Page<WxMpMenu> getPageList(WxMpMenu param) {
        List<Sort.Order> orders = new ArrayList<>();
        orders.add(new Sort.Order(Sort.Direction.DESC, "updateTime"));
        orders.add(new Sort.Order(Sort.Direction.DESC, "id"));
        PageRequest page = PageSort.pageRequest(orders);
        return wxMpMenuRepository.findAll((Root<WxMpMenu> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> preList = new ArrayList<>();
            if (param != null && StringUtils.isNotBlank(param.getAppId())) {
                preList.add(cb.like(root.get("appId").as(String.class), "%" + param.getAppId().trim() + "%"));
            }
            if (param != null && StringUtils.isNotBlank(param.getAppName())) {
                preList.add(cb.like(root.get("appName").as(String.class), "%" + param.getAppName().trim() + "%"));
            }
            Predicate[] pres = new Predicate[preList.size()];
            return query.where(preList.toArray(pres)).getRestriction();
        }, page);
    }

    public WxMpMenu save(WxMpMenu entity) {
        if (entity.getId() == null) {
            entity.setCreateTime(System.currentTimeMillis());
        }
        entity.setUpdateTime(System.currentTimeMillis());
        return wxMpMenuRepository.save(entity);
    }

    @Transactional
    public void deleteByIdIn(List<Long> idList) {
        wxMpMenuRepository.deleteByIdIn(idList);
    }
}
