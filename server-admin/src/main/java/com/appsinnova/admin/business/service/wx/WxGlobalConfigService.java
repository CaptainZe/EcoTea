package com.appsinnova.admin.business.service.wx;

import com.appsinnova.admin.business.common.constant.RedisConstant;
import com.appsinnova.admin.business.common.utils.RedisUtils;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
        entity = wxGlobalConfigRepository.save(entity);
        if (entity.getType() != null) {
            evictByType(entity.getType());
        }
        return entity;
    }

    @Transactional
    public void deleteByIdIn(List<Long> idList) {
        if (idList == null || idList.isEmpty()) {
            return;
        }
        Set<Integer> types = new LinkedHashSet<>();
        for (Long id : idList) {
            if (id == null) {
                continue;
            }
            WxGlobalConfig row = wxGlobalConfigRepository.findById(id).orElse(null);
            if (row != null && row.getType() != null) {
                types.add(row.getType());
            }
        }
        wxGlobalConfigRepository.deleteByIdIn(idList);
        for (Integer type : types) {
            evictByType(type);
        }
    }

    /** 失效 api 共用配置缓存（按 type） */
    private void evictByType(Integer type) {
        if (type == null) {
            return;
        }
        String key = String.format(RedisConstant.WX_GLOBAL_CONFIG_KEY, type);
        RedisUtils.delete(RedisUtils.defaultRedis(), key);
    }
}
