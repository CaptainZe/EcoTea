package com.appsinnova.admin.business.service.chai;

import com.appsinnova.admin.business.common.enums.base.YesOrNo;
import com.appsinnova.admin.business.common.enums.chai.ChaiStockQuality;
import com.appsinnova.admin.business.common.utils.chai.ChaiSpecUtil;
import com.appsinnova.admin.business.domain.chai.ChaiSku;
import com.appsinnova.admin.business.domain.chai.ChaiStock;
import com.appsinnova.admin.business.domain.chai.ChaiStockWh;
import com.appsinnova.admin.business.domain.chai.ChaiWarehouse;
import com.appsinnova.admin.business.repository.chai.ChaiSkuRepository;
import com.appsinnova.admin.business.repository.chai.ChaiStockRepository;
import com.appsinnova.admin.business.repository.chai.ChaiStockWhRepository;
import com.appsinnova.admin.common.data.PageSort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChaiStockService {

    private final ChaiStockRepository chaiStockRepository;
    private final ChaiStockWhRepository chaiStockWhRepository;
    private final ChaiSkuRepository chaiSkuRepository;
    private final ChaiWarehouseService chaiWarehouseService;

    public ChaiStock getById(Long id) {
        return chaiStockRepository.findById(id).orElse(null);
    }

    public ChaiStock getBySkuId(Long skuId) {
        if (skuId == null) {
            return null;
        }
        return chaiStockRepository.findFirstBySkuId(skuId).orElse(null);
    }

    /**
     * 某 SKU 在指定仓的结存：qty / qtyNoBag / qtyDamaged / qtyDamagedNoBag；无结存行返回 0。
     */
    public int[] getWhQtyBuckets(Long skuId, Long whId) {
        if (skuId == null || whId == null || whId <= 0) {
            return new int[]{0, 0, 0, 0};
        }
        ChaiStock stock = getBySkuId(skuId);
        if (stock == null) {
            return new int[]{0, 0, 0, 0};
        }
        return chaiStockWhRepository.findFirstByStockIdAndWhId(stock.getId(), whId)
                .map(row -> new int[]{
                        row.getQty() == null ? 0 : row.getQty(),
                        row.getQtyNoBag() == null ? 0 : row.getQtyNoBag(),
                        row.getQtyDamaged() == null ? 0 : row.getQtyDamaged(),
                        row.getQtyDamagedNoBag() == null ? 0 : row.getQtyDamagedNoBag()
                })
                .orElse(new int[]{0, 0, 0, 0});
    }

    /**
     * 某 SKU 在指定仓的结存件数；无结存行返回 0。
     */
    public int getWhQty(Long skuId, Long whId) {
        return getWhQtyBuckets(skuId, whId)[0];
    }

    /**
     * 批量查分仓件数；key=skuId，无仓或无行则为 0。
     */
    public Map<Long, Integer> mapWhQtyBySkuIds(List<Long> skuIds, Long whId) {
        Map<Long, Integer> map = new HashMap<>();
        if (skuIds == null || skuIds.isEmpty()) {
            return map;
        }
        for (Long skuId : skuIds) {
            if (skuId == null) {
                continue;
            }
            map.put(skuId, getWhQty(skuId, whId));
        }
        return map;
    }

    /**
     * 批量查分仓：总数 + 三例外；key=skuId。
     */
    public Map<Long, Map<String, Integer>> mapWhStockBySkuIds(List<Long> skuIds, Long whId) {
        Map<Long, Map<String, Integer>> map = new HashMap<>();
        if (skuIds == null || skuIds.isEmpty()) {
            return map;
        }
        for (Long skuId : skuIds) {
            if (skuId == null) {
                continue;
            }
            int[] buckets = getWhQtyBuckets(skuId, whId);
            Map<String, Integer> one = new HashMap<>();
            one.put("qty", buckets[0]);
            one.put("qtyNoBag", buckets[1]);
            one.put("qtyDamaged", buckets[2]);
            one.put("qtyDamagedNoBag", buckets[3]);
            map.put(skuId, one);
        }
        return map;
    }

    public boolean hasPositiveQtyBySkuId(Long skuId) {
        if (skuId == null) {
            return false;
        }
        return chaiStockRepository.existsBySkuIdAndTotalQtyGreaterThan(skuId, 0);
    }

    /**
     * 某 SPU 下是否存在 total_qty &gt; 0 的结存
     */
    public boolean hasPositiveQtyBySpuId(Long spuId) {
        if (spuId == null) {
            return false;
        }
        List<ChaiSku> skus = chaiSkuRepository.findBySpuIdOrderByYearDescProdBatchDesc(spuId);
        for (ChaiSku sku : skus) {
            if (hasPositiveQtyBySkuId(sku.getId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 过账加减分仓；delta 可正可负。无行且 delta&gt;0 时建行；不允许结果 &lt; 0。
     * 按品相只动总数，或同步增减对应例外列。
     */
    @Transactional(rollbackFor = Exception.class)
    public void applyWhDelta(Long skuId, Long whId, int delta, ChaiStockQuality quality, String operator) {
        if (skuId == null || whId == null) {
            throw new IllegalArgumentException("SKU与仓库不能为空");
        }
        if (quality == null) {
            throw new IllegalArgumentException("品相不能为空");
        }
        if (delta == 0) {
            return;
        }
        ChaiStock stock = ensureStock(skuId, operator);
        ChaiStockWh row = chaiStockWhRepository.findFirstByStockIdAndWhId(stock.getId(), whId).orElse(null);
        if (row == null) {
            if (delta < 0) {
                throw new IllegalArgumentException(quality.insufficientMessage());
            }
            row = newEmptyWh(stock.getId(), whId);
            row = chaiStockWhRepository.save(row);
        }
        normalizeWhBuckets(row);
        row = chaiStockWhRepository.save(row);
        int updated = applyQualityDelta(row.getId(), delta, row.getVersion(), quality);
        if (updated == 0) {
            throw new IllegalArgumentException(quality.conflictMessage());
        }
        recalcTotalQty(stock.getId(), operator);
    }

    private int applyQualityDelta(Long rowId, int delta, int version, ChaiStockQuality quality) {
        switch (quality) {
            case NO_BAG:
                return chaiStockWhRepository.applyNoBagQtyDelta(rowId, delta, version);
            case DAMAGED:
                return chaiStockWhRepository.applyDamagedQtyDelta(rowId, delta, version);
            case DAMAGED_NO_BAG:
                return chaiStockWhRepository.applyDamagedNoBagQtyDelta(rowId, delta, version);
            case INTACT:
            default:
                return chaiStockWhRepository.applyIntactQtyDelta(rowId, delta, version);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ChaiStock ensureStock(Long skuId, String operator) {
        ChaiStock stock = getBySkuId(skuId);
        if (stock != null) {
            if (stock.getQtyNoBag() == null || stock.getQtyDamaged() == null || stock.getQtyDamagedNoBag() == null) {
                normalizeStockBuckets(stock);
                return chaiStockRepository.save(stock);
            }
            return stock;
        }
        long now = System.currentTimeMillis();
        stock = new ChaiStock();
        stock.setSkuId(skuId);
        stock.setTotalQty(0);
        stock.setQtyNoBag(0);
        stock.setQtyDamaged(0);
        stock.setQtyDamagedNoBag(0);
        stock.setOperator(operator != null ? operator : "");
        stock.setCreateTime(now);
        stock.setUpdateTime(now);
        return chaiStockRepository.save(stock);
    }

    private ChaiStockWh newEmptyWh(Long stockId, Long whId) {
        ChaiStockWh row = new ChaiStockWh();
        row.setStockId(stockId);
        row.setWhId(whId);
        row.setQty(0);
        row.setQtyNoBag(0);
        row.setQtyDamaged(0);
        row.setQtyDamagedNoBag(0);
        row.setVersion(0);
        return row;
    }

    private void normalizeWhBuckets(ChaiStockWh row) {
        if (row.getQtyNoBag() == null) {
            row.setQtyNoBag(0);
        }
        if (row.getQtyDamaged() == null) {
            row.setQtyDamaged(0);
        }
        if (row.getQtyDamagedNoBag() == null) {
            row.setQtyDamagedNoBag(0);
        }
    }

    private void normalizeStockBuckets(ChaiStock stock) {
        if (stock.getQtyNoBag() == null) {
            stock.setQtyNoBag(0);
        }
        if (stock.getQtyDamaged() == null) {
            stock.setQtyDamaged(0);
        }
        if (stock.getQtyDamagedNoBag() == null) {
            stock.setQtyDamagedNoBag(0);
        }
    }

    private void recalcTotalQty(Long stockId, String operator) {
        List<ChaiStockWh> rows = chaiStockWhRepository.findByStockIdOrderByQtyDescIdAsc(stockId);
        int sum = 0;
        int noBagSum = 0;
        int damagedSum = 0;
        int damagedNoBagSum = 0;
        for (ChaiStockWh row : rows) {
            sum += row.getQty() == null ? 0 : row.getQty();
            noBagSum += row.getQtyNoBag() == null ? 0 : row.getQtyNoBag();
            damagedSum += row.getQtyDamaged() == null ? 0 : row.getQtyDamaged();
            damagedNoBagSum += row.getQtyDamagedNoBag() == null ? 0 : row.getQtyDamagedNoBag();
        }
        ChaiStock stock = getById(stockId);
        if (stock == null) {
            return;
        }
        stock.setTotalQty(sum);
        stock.setQtyNoBag(noBagSum);
        stock.setQtyDamaged(damagedSum);
        stock.setQtyDamagedNoBag(damagedNoBagSum);
        stock.setOperator(operator != null ? operator : "");
        stock.setUpdateTime(System.currentTimeMillis());
        chaiStockRepository.save(stock);
    }

    public Page<ChaiStock> getPageList(ChaiStock param) {
        List<Sort.Order> orders = new ArrayList<>();
        orders.add(new Sort.Order(Sort.Direction.DESC, "updateTime"));
        PageRequest page = PageSort.pageRequest(orders);
        Page<ChaiStock> result = chaiStockRepository.findAll(
                (Root<ChaiStock> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
                    List<Predicate> preList = genCondition(root, query, cb, param);
                    Predicate[] pres = new Predicate[preList.size()];
                    return query.where(preList.toArray(pres)).getRestriction();
                }, page);
        attachSkus(result.getContent());
        fillListQty(result.getContent(), param != null ? param.getQueryWhId() : null);
        return result;
    }

    /**
     * 列表件数：未选仓用全仓合计；选仓用该仓分仓结存。
     */
    private void fillListQty(List<ChaiStock> stocks, Long whId) {
        if (stocks == null || stocks.isEmpty()) {
            return;
        }
        if (whId == null || whId <= 0) {
            for (ChaiStock stock : stocks) {
                stock.setListQty(stock.getTotalQty() != null ? stock.getTotalQty() : 0);
                stock.setListQtyNoBag(stock.getQtyNoBag() != null ? stock.getQtyNoBag() : 0);
                stock.setListQtyDamaged(stock.getQtyDamaged() != null ? stock.getQtyDamaged() : 0);
                stock.setListQtyDamagedNoBag(stock.getQtyDamagedNoBag() != null ? stock.getQtyDamagedNoBag() : 0);
            }
            return;
        }
        List<Long> skuIds = new ArrayList<>();
        for (ChaiStock stock : stocks) {
            if (stock.getSkuId() != null) {
                skuIds.add(stock.getSkuId());
            }
        }
        Map<Long, Map<String, Integer>> whMap = mapWhStockBySkuIds(skuIds, whId);
        for (ChaiStock stock : stocks) {
            Map<String, Integer> one = whMap.get(stock.getSkuId());
            if (one == null) {
                stock.setListQty(0);
                stock.setListQtyNoBag(0);
                stock.setListQtyDamaged(0);
                stock.setListQtyDamagedNoBag(0);
            } else {
                stock.setListQty(one.get("qty") != null ? one.get("qty") : 0);
                stock.setListQtyNoBag(one.get("qtyNoBag") != null ? one.get("qtyNoBag") : 0);
                stock.setListQtyDamaged(one.get("qtyDamaged") != null ? one.get("qtyDamaged") : 0);
                stock.setListQtyDamagedNoBag(one.get("qtyDamagedNoBag") != null ? one.get("qtyDamagedNoBag") : 0);
            }
        }
    }

    /**
     * 列表展示：批量挂载 SKU（非 JPA 关联）
     */
    private void attachSkus(List<ChaiStock> stocks) {
        if (stocks == null || stocks.isEmpty()) {
            return;
        }
        Set<Long> skuIds = new HashSet<>();
        for (ChaiStock stock : stocks) {
            if (stock.getSkuId() != null) {
                skuIds.add(stock.getSkuId());
            }
        }
        if (skuIds.isEmpty()) {
            return;
        }
        Map<Long, ChaiSku> skuMap = new HashMap<>();
        for (ChaiSku sku : chaiSkuRepository.findAllById(skuIds)) {
            skuMap.put(sku.getId(), sku);
        }
        for (ChaiStock stock : stocks) {
            stock.setSku(skuMap.get(stock.getSkuId()));
        }
    }

    public List<ChaiStockWh> listWhByStockId(Long stockId) {
        if (stockId == null) {
            return new ArrayList<>();
        }
        List<ChaiStockWh> list = chaiStockWhRepository.findByStockIdOrderByQtyDescIdAsc(stockId);
        for (ChaiStockWh row : list) {
            ChaiWarehouse wh = chaiWarehouseService.getById(row.getWhId());
            row.setWhName(wh != null ? wh.getName() : String.valueOf(row.getWhId()));
        }
        return list;
    }

    /**
     * 从已挂载的 sku 填充列表展示字段
     */
    public void fillSkuShow(ChaiStock stock, Map<Long, String> brandNameMap, Map<Long, String> spuCodeMap) {
        if (stock == null) {
            return;
        }
        ChaiSku sku = stock.getSku();
        if (sku == null && stock.getSkuId() != null) {
            return;
        }
        if (sku == null) {
            return;
        }
        stock.setSkuCode(sku.getSkuCode());
        stock.setName(sku.getName());
        stock.setBrand(sku.getBrand());
        stock.setStarLevel(sku.getStarLevel());
        stock.setYear(sku.getYear());
        stock.setProdBatch(sku.getProdBatch());
        stock.setType(sku.getType());
        stock.setStatus(sku.getStatus());
        stock.setDeleted(sku.getDeleted());
        stock.setSpuId(sku.getSpuId());
        if (sku.getBrand() != null && brandNameMap != null) {
            stock.setBrandName(brandNameMap.getOrDefault(sku.getBrand(), String.valueOf(sku.getBrand())));
        } else {
            stock.setBrandName("-");
        }
        if (sku.getSpuId() != null && spuCodeMap != null) {
            stock.setSpuCode(spuCodeMap.get(sku.getSpuId()));
        }
        stock.setSpecShow(ChaiSpecUtil.toShow(sku.getSpec()));
    }

    private List<Predicate> genCondition(Root<ChaiStock> root, CriteriaQuery<?> query,
                                         CriteriaBuilder cb, ChaiStock param) {
        List<Predicate> preList = new ArrayList<>();
        // 无 @ManyToOne：用第二 Root 等值连接，避免 sku_id 重复映射
        Root<ChaiSku> skuRoot = query.from(ChaiSku.class);
        preList.add(cb.equal(root.get("skuId"), skuRoot.get("id")));
        Class<?> resultType = query.getResultType();
        boolean isCount = (Long.class.equals(resultType) || long.class.equals(resultType));
        if (!isCount) {
            query.distinct(true);
        }

        if (param == null) {
            return preList;
        }
        if (StringUtils.hasText(param.getName())) {
            preList.add(cb.like(skuRoot.get("name").as(String.class), "%" + param.getName().trim() + "%"));
        }
        if (StringUtils.hasText(param.getSkuCode())) {
            preList.add(cb.equal(skuRoot.get("skuCode").as(String.class), param.getSkuCode().trim()));
        }
        if (param.getBrand() != null) {
            preList.add(cb.equal(skuRoot.get("brand").as(Long.class), param.getBrand()));
        }
        if (param.getType() != null) {
            preList.add(cb.equal(skuRoot.get("type").as(Integer.class), param.getType()));
        }
        if (param.getStarLevel() != null) {
            preList.add(cb.equal(skuRoot.get("starLevel").as(Integer.class), param.getStarLevel()));
        }
        if (param.getYear() != null) {
            preList.add(cb.equal(skuRoot.get("year").as(Integer.class), param.getYear()));
        }
        if (param.getProdBatch() != null) {
            preList.add(cb.equal(skuRoot.get("prodBatch").as(Integer.class), param.getProdBatch()));
        }
        if (param.getStatus() != null) {
            preList.add(cb.equal(skuRoot.get("status").as(Integer.class), param.getStatus()));
        }
        if (param.getNonSale() != null) {
            preList.add(cb.equal(skuRoot.get("nonSale").as(Integer.class), param.getNonSale()));
        }
        if (param.getDeleted() != null) {
            preList.add(cb.equal(skuRoot.get("deleted").as(Integer.class), param.getDeleted()));
        }
        if (param.getSpuId() != null) {
            preList.add(cb.equal(skuRoot.get("spuId").as(Long.class), param.getSpuId()));
        }
        Long whId = param.getQueryWhId();
        boolean filterByWh = whId != null && whId > 0;
        if (filterByWh) {
            if (YesOrNo.isYes(param.getQueryHasQty())) {
                // 该仓 qty > 0
                Subquery<Long> sq = query.subquery(Long.class);
                Root<ChaiStockWh> whRoot = sq.from(ChaiStockWh.class);
                sq.select(whRoot.get("stockId"));
                sq.where(
                        cb.equal(whRoot.get("stockId"), root.get("id")),
                        cb.equal(whRoot.get("whId"), whId),
                        cb.greaterThan(whRoot.get("qty").as(Integer.class), 0)
                );
                preList.add(cb.exists(sq));
            } else if (YesOrNo.isNo(param.getQueryHasQty())) {
                // 该仓无货：无分仓行或 qty = 0
                Subquery<Long> sq = query.subquery(Long.class);
                Root<ChaiStockWh> whRoot = sq.from(ChaiStockWh.class);
                sq.select(whRoot.get("stockId"));
                sq.where(
                        cb.equal(whRoot.get("stockId"), root.get("id")),
                        cb.equal(whRoot.get("whId"), whId),
                        cb.greaterThan(whRoot.get("qty").as(Integer.class), 0)
                );
                preList.add(cb.not(cb.exists(sq)));
            } else {
                // 仅选仓：该仓有过分仓记录（含清零）
                Subquery<Long> sq = query.subquery(Long.class);
                Root<ChaiStockWh> whRoot = sq.from(ChaiStockWh.class);
                sq.select(whRoot.get("stockId"));
                sq.where(
                        cb.equal(whRoot.get("stockId"), root.get("id")),
                        cb.equal(whRoot.get("whId"), whId)
                );
                preList.add(cb.exists(sq));
            }
        } else if (param.getQueryHasQty() != null) {
            if (YesOrNo.isYes(param.getQueryHasQty())) {
                preList.add(cb.greaterThan(root.get("totalQty").as(Integer.class), 0));
            } else if (YesOrNo.isNo(param.getQueryHasQty())) {
                preList.add(cb.equal(root.get("totalQty").as(Integer.class), 0));
            }
        }
        return preList;
    }
}
