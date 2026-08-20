package com.appsinnova.admin.business.service.chai;

import com.appsinnova.admin.business.common.enums.chai.ChaiStatus;
import com.appsinnova.admin.business.common.utils.chai.ChaiCodeUtil;
import com.appsinnova.admin.business.common.utils.chai.ChaiHalfYearUtil;
import com.appsinnova.admin.business.common.utils.chai.ChaiSpecUtil;
import com.appsinnova.admin.business.domain.chai.ChaiSku;
import com.appsinnova.admin.business.domain.chai.ChaiSpu;
import com.appsinnova.admin.business.repository.chai.ChaiSkuRepository;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChaiSkuService {

    private final ChaiSkuRepository chaiSkuRepository;

    public ChaiSku getById(Long id) {
        return chaiSkuRepository.findById(id).orElse(null);
    }

    public List<ChaiSku> listBySpuId(Long spuId) {
        return listBySpuId(spuId, 0);
    }

    /**
     * @param deleted 0未删除 1已删除 null全部
     */
    public List<ChaiSku> listBySpuId(Long spuId, Integer deleted) {
        if (spuId == null) {
            return new ArrayList<>();
        }
        if (deleted == null) {
            return chaiSkuRepository.findBySpuIdOrderByYearDescProdBatchDesc(spuId);
        }
        return chaiSkuRepository.findBySpuIdAndDeletedOrderByYearDescProdBatchDesc(spuId, deleted);
    }

    public long countBySpuId(Long spuId) {
        if (spuId == null) {
            return 0;
        }
        return chaiSkuRepository.countBySpuIdAndDeleted(spuId, 0);
    }

    /**
     * 是否存在未删除的锚点半年 SKU
     */
    public boolean existsActiveBySpuAndHalfYear(Long spuId, Integer year, Integer prodBatch) {
        if (spuId == null || year == null || prodBatch == null) {
            return false;
        }
        return chaiSkuRepository.existsBySpuIdAndYearAndProdBatchAndDeleted(spuId, year, prodBatch, 0);
    }

    public Page<ChaiSku> getPageList(ChaiSku param) {
        List<Sort.Order> orders = new ArrayList<>();
        orders.add(new Sort.Order(Sort.Direction.DESC, "updateTime"));
        PageRequest page = PageSort.pageRequest(orders);
        return chaiSkuRepository.findAll((Root<ChaiSku> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> preList = genCondition(root, cb, param);
            Predicate[] pres = new Predicate[preList.size()];
            return query.where(preList.toArray(pres)).getRestriction();
        }, page);
    }

    /**
     * 按 SPU 锚点生成默认 6 条（未落库）
     */
    public List<ChaiSku> buildDefaultSkuList(ChaiSpu spu) {
        List<ChaiSku> list = new ArrayList<>();
        if (spu == null || spu.getId() == null) {
            return list;
        }
        List<ChaiHalfYearUtil.HalfYear> halfYears =
                ChaiHalfYearUtil.recentFromAnchor(spu.getYear(), spu.getProdBatch(), 6);
        for (ChaiHalfYearUtil.HalfYear halfYear : halfYears) {
            list.add(copyFromSpu(spu, halfYear.getYear(), halfYear.getProdBatch()));
        }
        return list;
    }

    public ChaiSku copyFromSpu(ChaiSpu spu, Integer year, Integer prodBatch) {
        ChaiSku sku = new ChaiSku();
        sku.setSpuId(spu.getId());
        sku.setStarLevel(spu.getStarLevel());
        sku.setName(spu.getName());
        sku.setBrand(spu.getBrand());
        sku.setExpiration(spu.getExpiration());
        sku.setType(spu.getType());
        sku.setGrade(spu.getGrade());
        sku.setYear(year);
        sku.setProdBatch(prodBatch);
        sku.setSpec(spu.getSpec());
        sku.setShowImageUrls(spu.getShowImageUrls());
        sku.setRealImageUrls(spu.getRealImageUrls());
        sku.setStatus(spu.getStatus() != null ? spu.getStatus() : 0);
        sku.setDeleted(0);
        sku.setOfficialPrice(spu.getOfficialPrice() != null ? spu.getOfficialPrice() : BigDecimal.ONE);
        sku.setSalePrice(BigDecimal.ONE);
        sku.setRecyclePrice(BigDecimal.ONE);
        sku.setRecyclePriceReducePer(5);
        sku.setRecyclePriceReduceNoBag(new BigDecimal("10"));
        ChaiSpecUtil.fillSpecFields(sku);
        return sku;
    }

    public ChaiSku save(ChaiSku entity) {
        boolean isCreate = false;
        if (entity.getId() == null) {
            // 不可用空串：sku_code 有唯一约束，批量创建时多个 '' 会撞 unique_key1
            entity.setSkuCode("TMP-" + UUID.randomUUID().toString().replace("-", ""));
            entity.setCreateTime(System.currentTimeMillis());
            if (entity.getDeleted() == null) {
                entity.setDeleted(0);
            }
            isCreate = true;
        }
        entity.setUpdateTime(System.currentTimeMillis());
        entity = chaiSkuRepository.save(entity);
        if (isCreate) {
            entity.setSkuCode(ChaiCodeUtil.skuCode(entity.getSpuId(), entity.getId()));
            entity = chaiSkuRepository.save(entity);
        }
        return entity;
    }

    /**
     * 批量保存某 SPU 下 SKU：提交列表全量覆盖。
     * 未出现的有效 SKU 软删；无 id 但命中已删 (year, prodBatch) 则恢复原 id/编码。
     * 空列表视为全部有效 SKU 软删。brand / expiration / type 强制继承 SPU。
     */
    @Transactional
    public void saveBatchForSpu(ChaiSpu spu, List<ChaiSku> itemList, String operator) {
        if (spu == null || spu.getId() == null) {
            throw new IllegalArgumentException("SPU不能为空");
        }
        Long spuId = spu.getId();
        if (itemList == null) {
            itemList = new ArrayList<>();
        }

        Set<String> batchKeys = new HashSet<>();
        for (ChaiSku item : itemList) {
            if (item.getYear() == null || item.getProdBatch() == null) {
                throw new IllegalArgumentException("年份与生产批次必填");
            }
            String key = item.getYear() + "-" + item.getProdBatch();
            if (!batchKeys.add(key)) {
                throw new IllegalArgumentException("存在重复的年份+生产批次：" + item.getYear()
                        + " / " + item.getProdBatch());
            }
            if (!StringUtils.hasText(item.getName())) {
                throw new IllegalArgumentException("商品名称必填");
            }
            if (item.getStarLevel() == null) {
                throw new IllegalArgumentException("星级必选");
            }
            if (item.getGrade() == null) {
                throw new IllegalArgumentException("等级必选");
            }
            if (item.getTotalNetWeight() == null || item.getTotalNetWeight().compareTo(BigDecimal.ZERO) <= 0
                    || item.getUnitWeight() == null || item.getUnitWeight().compareTo(BigDecimal.ZERO) <= 0
                    || item.getUnitCount() == null || item.getUnitLabel() == null) {
                throw new IllegalArgumentException("规格信息不完整");
            }
            validatePrice(item);
            resolveExistingSku(item, spuId);
        }

        List<ChaiSku> oldList = listBySpuId(spuId);
        Set<Long> keepIds = itemList.stream()
                .map(ChaiSku::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        for (ChaiSku old : oldList) {
            if (keepIds.contains(old.getId())) {
                continue;
            }
            old.setDeleted(1);
            old.setStatus(ChaiStatus.OFFLINE.getCode());
            old.setOperator(operator);
            save(old);
        }

        for (ChaiSku item : itemList) {
            item.setSpuId(spuId);
            item.setBrand(spu.getBrand());
            item.setExpiration(spu.getExpiration());
            item.setType(spu.getType());
            item.setOperator(operator);
            item.setDeleted(0);
            if (item.getId() != null) {
                ChaiSku old = getById(item.getId());
                if (old == null || !spuId.equals(old.getSpuId())) {
                    throw new IllegalArgumentException("SKU记录不存在或不属于当前SPU");
                }
                item.setSkuCode(old.getSkuCode());
                item.setCreateTime(old.getCreateTime());
            }
            item.setSpec(ChaiSpecUtil.buildSpecJson(
                    item.getTotalNetWeight(), item.getUnitWeight(),
                    item.getUnitCount(), item.getUnitLabel()));
            if (!StringUtils.hasText(item.getShowImageUrls())) {
                item.setShowImageUrls("[]");
            }
            if (!StringUtils.hasText(item.getRealImageUrls())) {
                item.setRealImageUrls("[]");
            }
            item.setStatus(spu.getStatus() != null ? spu.getStatus() : 0);
            save(item);
        }
        syncStatusFromSpu(spuId, spu.getStatus() != null ? spu.getStatus() : 0, operator);
    }

    /**
     * 无 id 时按 (spuId, year, prodBatch) 找回已有行（含已删），沿用原 id/编码。
     * 有效行同键且提交了另一个 id 则拒绝。
     */
    private void resolveExistingSku(ChaiSku item, Long spuId) {
        ChaiSku byKey = chaiSkuRepository
                .findFirstBySpuIdAndYearAndProdBatch(spuId, item.getYear(), item.getProdBatch())
                .orElse(null);
        if (item.getId() == null) {
            if (byKey != null) {
                item.setId(byKey.getId());
            }
            return;
        }
        if (byKey != null && !byKey.getId().equals(item.getId())) {
            throw new IllegalArgumentException("存在重复的年份+生产批次：" + item.getYear()
                    + " / " + item.getProdBatch());
        }
    }

    /**
     * 将某 SPU 下全部 SKU 状态与 SPU 对齐
     */
    @Transactional
    public void syncStatusFromSpu(Long spuId, Integer status, String operator) {
        if (spuId == null || status == null) {
            return;
        }
        chaiSkuRepository.updateStatusBySpuId(spuId, status, operator, System.currentTimeMillis());
    }

    @Transactional
    public void markDeletedBySpuId(Long spuId, String operator) {
        if (spuId == null) {
            return;
        }
        chaiSkuRepository.softDeleteBySpuId(spuId, ChaiStatus.OFFLINE.getCode(),
                operator, System.currentTimeMillis());
    }

    @Transactional
    public void restoreBySpuId(Long spuId, String operator) {
        if (spuId == null) {
            return;
        }
        chaiSkuRepository.restoreBySpuId(spuId, ChaiStatus.OFFLINE.getCode(),
                operator, System.currentTimeMillis());
    }

    private void validatePrice(ChaiSku item) {
        if (item.getSalePrice() == null) {
            throw new IllegalArgumentException("销售价必填");
        }
        if (item.getRecyclePrice() == null) {
            throw new IllegalArgumentException("回收价必填");
        }
        if (item.getRecyclePriceReducePer() == null) {
            throw new IllegalArgumentException("回收价压价百分比必填");
        }
        if (item.getRecyclePriceReduceNoBag() == null) {
            throw new IllegalArgumentException("无提袋扣减必填");
        }
    }

    private List<Predicate> genCondition(Root<ChaiSku> root, CriteriaBuilder cb, ChaiSku param) {
        List<Predicate> preList = new ArrayList<>();
        if (param == null) {
            return preList;
        }
        if (StringUtils.hasText(param.getSkuCode())) {
            preList.add(cb.equal(root.get("skuCode").as(String.class), param.getSkuCode().trim()));
        }
        if (StringUtils.hasText(param.getName())) {
            preList.add(cb.like(root.get("name").as(String.class), "%" + param.getName().trim() + "%"));
        }
        if (param.getSpuId() != null) {
            preList.add(cb.equal(root.get("spuId").as(Long.class), param.getSpuId()));
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
