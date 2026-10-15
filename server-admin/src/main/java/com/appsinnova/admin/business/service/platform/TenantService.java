package com.appsinnova.admin.business.service.platform;

import com.appsinnova.admin.business.common.constant.RedisConstant;
import com.appsinnova.admin.business.common.pca.PcaCodeService;
import com.appsinnova.admin.business.common.utils.RedisUtils;
import com.appsinnova.admin.business.domain.platform.Tenant;
import com.appsinnova.admin.business.repository.platform.TenantRepository;
import com.appsinnova.admin.common.data.PageSort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final PcaCodeService pcaCodeService;

    public Tenant getById(Long id) {
        return tenantRepository.findById(id).orElse(null);
    }

    public Page<Tenant> getPageList(Tenant param) {
        List<Sort.Order> orders = new ArrayList<>();
        orders.add(new Sort.Order(Sort.Direction.DESC, "updateTime"));
        PageRequest page = PageSort.pageRequest(orders);
        Page<Tenant> result = tenantRepository.findAll(
                (Root<Tenant> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
                    List<Predicate> preList = genCondition(root, cb, param);
                    Predicate[] pres = new Predicate[preList.size()];
                    return query.where(preList.toArray(pres)).getRestriction();
                }, page);
        result.getContent().forEach(this::fillRegionNames);
        return result;
    }

    public boolean isTenantIdTakenByOther(Long tenantId, Long excludeId) {
        if (tenantId == null) {
            return false;
        }
        if (excludeId == null) {
            return tenantRepository.findFirstByTenantId(tenantId).isPresent();
        }
        return tenantRepository.existsByTenantIdAndIdNot(tenantId, excludeId);
    }

    public Tenant save(Tenant entity) {
        if (entity.getId() == null) {
            entity.setCreateTime(System.currentTimeMillis());
        }
        entity.setUpdateTime(System.currentTimeMillis());
        Tenant saved = tenantRepository.save(entity);
        clearTenantCache(saved.getTenantId());
        return saved;
    }

    /** 写后 DEL：{@code ecotea_tenant_{tenantId}} */
    public void clearTenantCache(Long tenantId) {
        if (tenantId == null) {
            return;
        }
        try {
            RedisUtils.delete(RedisUtils.defaultRedis(),
                    String.format(RedisConstant.TENANT_KEY, tenantId));
        } catch (Exception e) {
            log.warn("clear tenant cache failed, tenantId={}: {}", tenantId, e.getMessage());
        }
    }

    public void fillRegionNames(Tenant item) {
        if (item == null) {
            return;
        }
        item.setProvinceName(pcaCodeService.resolveName(item.getProvince()));
        item.setCityName(pcaCodeService.resolveName(item.getCity()));
        item.setDistrictName(pcaCodeService.resolveName(item.getDistrict()));
    }

    private List<Predicate> genCondition(Root<Tenant> root, CriteriaBuilder cb, Tenant param) {
        List<Predicate> preList = new ArrayList<>();
        if (param == null) {
            return preList;
        }
        if (param.getTenantId() != null) {
            preList.add(cb.equal(root.get("tenantId").as(Long.class), param.getTenantId()));
        }
        if (StringUtils.hasText(param.getName())) {
            preList.add(cb.like(root.get("name").as(String.class), "%" + param.getName().trim() + "%"));
        }
        if (param.getStatus() != null) {
            preList.add(cb.equal(root.get("status").as(Integer.class), param.getStatus()));
        }
        if (StringUtils.hasText(param.getProvince())) {
            preList.add(cb.equal(root.get("province").as(String.class), param.getProvince().trim()));
        }
        if (StringUtils.hasText(param.getCity())) {
            preList.add(cb.equal(root.get("city").as(String.class), param.getCity().trim()));
        }
        if (StringUtils.hasText(param.getDistrict())) {
            preList.add(cb.equal(root.get("district").as(String.class), param.getDistrict().trim()));
        }
        return preList;
    }
}
