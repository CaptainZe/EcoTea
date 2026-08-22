package com.appsinnova.admin.business.service.chai;

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
     * 某 SKU 在指定仓的结存件数；无结存行返回 0。
     */
    public int getWhQty(Long skuId, Long whId) {
        if (skuId == null || whId == null || whId <= 0) {
            return 0;
        }
        ChaiStock stock = getBySkuId(skuId);
        if (stock == null) {
            return 0;
        }
        return chaiStockWhRepository.findFirstByStockIdAndWhId(stock.getId(), whId)
                .map(row -> row.getQty() == null ? 0 : row.getQty())
                .orElse(0);
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
     */
    @Transactional(rollbackFor = Exception.class)
    public void applyWhDelta(Long skuId, Long whId, int delta, String operator) {
        if (skuId == null || whId == null) {
            throw new IllegalArgumentException("SKU与仓库不能为空");
        }
        if (delta == 0) {
            return;
        }
        ChaiStock stock = ensureStock(skuId, operator);
        ChaiStockWh row = chaiStockWhRepository.findFirstByStockIdAndWhId(stock.getId(), whId).orElse(null);
        if (row == null) {
            if (delta < 0) {
                throw new IllegalArgumentException("仓库库存不足");
            }
            row = new ChaiStockWh();
            row.setStockId(stock.getId());
            row.setWhId(whId);
            row.setQty(0);
            row.setVersion(0);
            row = chaiStockWhRepository.save(row);
        }
        int updated = chaiStockWhRepository.applyQtyDelta(row.getId(), delta, row.getVersion());
        if (updated == 0) {
            throw new IllegalArgumentException("库存不足或并发冲突，请重试");
        }
        recalcTotalQty(stock.getId(), operator);
    }

    @Transactional(rollbackFor = Exception.class)
    public ChaiStock ensureStock(Long skuId, String operator) {
        ChaiStock stock = getBySkuId(skuId);
        if (stock != null) {
            return stock;
        }
        long now = System.currentTimeMillis();
        stock = new ChaiStock();
        stock.setSkuId(skuId);
        stock.setTotalQty(0);
        stock.setOperator(operator != null ? operator : "");
        stock.setCreateTime(now);
        stock.setUpdateTime(now);
        return chaiStockRepository.save(stock);
    }

    private void recalcTotalQty(Long stockId, String operator) {
        List<ChaiStockWh> rows = chaiStockWhRepository.findByStockIdOrderByQtyDescIdAsc(stockId);
        int sum = 0;
        for (ChaiStockWh row : rows) {
            sum += row.getQty() == null ? 0 : row.getQty();
        }
        ChaiStock stock = getById(stockId);
        if (stock == null) {
            return;
        }
        stock.setTotalQty(sum);
        stock.setOperator(operator != null ? operator : "");
        stock.setUpdateTime(System.currentTimeMillis());
        chaiStockRepository.save(stock);
    }

    public Page<ChaiStock> getPageList(ChaiStock param) {
        List<Sort.Order> orders = new ArrayList<>();
        orders.add(new Sort.Order(Sort.Direction.DESC, "totalQty"));
        orders.add(new Sort.Order(Sort.Direction.DESC, "updateTime"));
        PageRequest page = PageSort.pageRequest(orders);
        Page<ChaiStock> result = chaiStockRepository.findAll(
                (Root<ChaiStock> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
                    List<Predicate> preList = genCondition(root, query, cb, param);
                    Predicate[] pres = new Predicate[preList.size()];
                    return query.where(preList.toArray(pres)).getRestriction();
                }, page);
        attachSkus(result.getContent());
        return result;
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
        boolean isCount = resultType != null
                && (Long.class.equals(resultType) || long.class.equals(resultType));
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
        if (param.getDeleted() != null) {
            preList.add(cb.equal(skuRoot.get("deleted").as(Integer.class), param.getDeleted()));
        }
        if (param.getSpuId() != null) {
            preList.add(cb.equal(skuRoot.get("spuId").as(Long.class), param.getSpuId()));
        }
        if (param.getQueryHasQty() != null) {
            if (Integer.valueOf(1).equals(param.getQueryHasQty())) {
                preList.add(cb.greaterThan(root.get("totalQty").as(Integer.class), 0));
            } else if (Integer.valueOf(0).equals(param.getQueryHasQty())) {
                preList.add(cb.equal(root.get("totalQty").as(Integer.class), 0));
            }
        }
        if (param.getQueryWhId() != null) {
            Subquery<Long> sq = query.subquery(Long.class);
            Root<ChaiStockWh> whRoot = sq.from(ChaiStockWh.class);
            sq.select(whRoot.get("stockId"));
            sq.where(
                    cb.equal(whRoot.get("stockId"), root.get("id")),
                    cb.equal(whRoot.get("whId"), param.getQueryWhId())
            );
            preList.add(cb.exists(sq));
        }
        return preList;
    }
}
