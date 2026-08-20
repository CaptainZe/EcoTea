package com.appsinnova.admin.business.service.chai;

import com.appsinnova.admin.business.common.enums.chai.ChaiStatus;
import com.appsinnova.admin.business.common.utils.chai.ChaiCodeUtil;
import com.appsinnova.admin.business.domain.chai.ChaiSpu;
import com.appsinnova.admin.business.repository.chai.ChaiSpuRepository;
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChaiSpuService {

    private final ChaiSpuRepository chaiSpuRepository;
    private final ChaiSkuService chaiSkuService;

    public ChaiSpu getById(Long id) {
        return chaiSpuRepository.findById(id).orElse(null);
    }

    public ChaiSpu getBySpuCode(String spuCode) {
        if (!StringUtils.hasText(spuCode)) {
            return null;
        }
        return chaiSpuRepository.findFirstBySpuCodeAndDeleted(spuCode.trim(), 0).orElse(null);
    }

    /** 列表按编码找父 SPU 时包含已删除 */
    public ChaiSpu getBySpuCodeIncludeDeleted(String spuCode) {
        if (!StringUtils.hasText(spuCode)) {
            return null;
        }
        return chaiSpuRepository.findFirstBySpuCode(spuCode.trim()).orElse(null);
    }

    public List<ChaiSpu> getByIdIn(List<Long> idList) {
        if (idList == null || idList.isEmpty()) {
            return new ArrayList<>();
        }
        return chaiSpuRepository.findByIdIn(idList);
    }

    /** 任意 SPU（含已软删）是否引用该品牌 */
    public boolean isBrandInUse(Long brand) {
        return brand != null && chaiSpuRepository.existsByBrand(brand);
    }

    /** 任意 SPU（含已软删）是否引用该保质期 */
    public boolean isExpirationInUse(Long expiration) {
        return expiration != null && chaiSpuRepository.existsByExpiration(expiration);
    }

    /**
     * 同品牌下是否存在同名商品（可排除当前编辑记录）
     */
    public boolean isNameTakenByOtherInBrand(Long brand, String name, Long excludeId) {
        if (brand == null || !StringUtils.hasText(name)) {
            return false;
        }
        String trimmed = name.trim();
        if (excludeId == null) {
            return chaiSpuRepository.existsByBrandAndNameAndDeleted(brand, trimmed, 0);
        }
        return chaiSpuRepository.existsByBrandAndNameAndIdNotAndDeleted(brand, trimmed, excludeId, 0);
    }

    public Page<ChaiSpu> getPageList(ChaiSpu param) {
        List<Sort.Order> orders = new ArrayList<>();
        orders.add(new Sort.Order(Sort.Direction.DESC, "updateTime"));
        PageRequest page = PageSort.pageRequest(orders);
        return chaiSpuRepository.findAll((Root<ChaiSpu> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> preList = genCondition(root, cb, param);
            Predicate[] pres = new Predicate[preList.size()];
            return query.where(preList.toArray(pres)).getRestriction();
        }, page);
    }

    public ChaiSpu save(ChaiSpu entity) {
        boolean isCreate = false;
        if (entity.getId() == null) {
            entity.setSpuCode("");
            entity.setCreateTime(System.currentTimeMillis());
            if (entity.getDeleted() == null) {
                entity.setDeleted(0);
            }
            if (entity.getOfficialPrice() == null) {
                entity.setOfficialPrice(BigDecimal.ONE);
            }
            isCreate = true;
        }
        entity.setUpdateTime(System.currentTimeMillis());
        entity = chaiSpuRepository.save(entity);
        if (isCreate) {
            entity.setSpuCode(ChaiCodeUtil.spuCode(entity.getId()));
            entity = chaiSpuRepository.save(entity);
        }
        return entity;
    }

    @Transactional
    public void softDeleteByIdIn(List<Long> idList, String operator) {
        if (idList == null || idList.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (Long id : idList) {
            int updated = chaiSpuRepository.softDeleteById(
                    id, ChaiStatus.OFFLINE.getCode(), operator, now);
            if (updated > 0) {
                chaiSkuService.markDeletedBySpuId(id, operator);
            }
        }
    }

    @Transactional
    public void restoreByIdIn(List<Long> idList, String operator) {
        if (idList == null || idList.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (Long id : idList) {
            int updated = chaiSpuRepository.restoreById(
                    id, ChaiStatus.OFFLINE.getCode(), operator, now);
            if (updated > 0) {
                chaiSkuService.restoreBySpuId(id, operator);
            }
        }
    }

    private List<Predicate> genCondition(Root<ChaiSpu> root, CriteriaBuilder cb, ChaiSpu param) {
        List<Predicate> preList = new ArrayList<>();
        if (param == null) {
            return preList;
        }
        if (StringUtils.hasText(param.getSpuCode())) {
            preList.add(cb.equal(root.get("spuCode").as(String.class), param.getSpuCode().trim()));
        }
        if (StringUtils.hasText(param.getName())) {
            preList.add(cb.like(root.get("name").as(String.class), "%" + param.getName().trim() + "%"));
        }
        if (param.getBrand() != null) {
            preList.add(cb.equal(root.get("brand").as(Long.class), param.getBrand()));
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
        if (param.getProdBatch() != null) {
            preList.add(cb.equal(root.get("prodBatch").as(Integer.class), param.getProdBatch()));
        }
        if (param.getStarLevel() != null) {
            preList.add(cb.equal(root.get("starLevel").as(Integer.class), param.getStarLevel()));
        }
        if (param.getStatus() != null) {
            preList.add(cb.equal(root.get("status").as(Integer.class), param.getStatus()));
        }
        if (param.getDeleted() != null) {
            preList.add(cb.equal(root.get("deleted").as(Integer.class), param.getDeleted()));
        }
        return preList;
    }
}
