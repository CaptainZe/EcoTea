package com.appsinnova.admin.business.service.chai;

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
import org.springframework.util.CollectionUtils;
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
        if (spuId == null) {
            return new ArrayList<>();
        }
        return chaiSkuRepository.findBySpuIdOrderByYearDescProdBatchDesc(spuId);
    }

    public long countBySpuId(Long spuId) {
        if (spuId == null) {
            return 0;
        }
        return chaiSkuRepository.countBySpuId(spuId);
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
        sku.setOfficialPrice(BigDecimal.ONE);
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
     * 批量保存某 SPU 下 SKU：提交列表全量覆盖（保留仍存在的 id/编码）。
     * brand / expiration / type 强制继承 SPU，不允许前端篡改。
     */
    @Transactional
    public void saveBatchForSpu(ChaiSpu spu, List<ChaiSku> itemList, String operator) {
        if (spu == null || spu.getId() == null) {
            throw new IllegalArgumentException("SPU不能为空");
        }
        Long spuId = spu.getId();
        if (CollectionUtils.isEmpty(itemList)) {
            chaiSkuRepository.deleteBySpuId(spuId);
            return;
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
        }

        List<ChaiSku> oldList = listBySpuId(spuId);
        Set<Long> keepIds = itemList.stream()
                .map(ChaiSku::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<Long> deleteIds = oldList.stream()
                .map(ChaiSku::getId)
                .filter(id -> !keepIds.contains(id))
                .collect(Collectors.toList());
        if (!deleteIds.isEmpty()) {
            chaiSkuRepository.deleteByIdIn(deleteIds);
        }

        for (ChaiSku item : itemList) {
            item.setSpuId(spuId);
            item.setBrand(spu.getBrand());
            item.setExpiration(spu.getExpiration());
            item.setType(spu.getType());
            item.setOperator(operator);
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
            // SKU 状态跟随 SPU，不允许单独设置
            item.setStatus(spu.getStatus() != null ? spu.getStatus() : 0);
            save(item);
        }
        // 再刷一遍，防止遗漏
        syncStatusFromSpu(spuId, spu.getStatus() != null ? spu.getStatus() : 0, operator);
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

    @Transactional
    public void deleteBySpuId(Long spuId) {
        if (spuId == null) {
            return;
        }
        chaiSkuRepository.deleteBySpuId(spuId);
    }

    @Transactional
    public void deleteByIdIn(List<Long> idList) {
        if (CollectionUtils.isEmpty(idList)) {
            return;
        }
        chaiSkuRepository.deleteByIdIn(idList);
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
        return preList;
    }
}
