package com.appsinnova.admin.business.service.chai;

import com.appsinnova.admin.business.common.enums.chai.ChaiSkuViewType;
import com.appsinnova.admin.business.common.enums.chai.ChaiStatus;
import com.appsinnova.admin.business.common.utils.chai.ChaiFormHelper;
import com.appsinnova.admin.business.common.utils.chai.ChaiHalfYearUtil;
import com.appsinnova.admin.business.common.utils.chai.ChaiPriceUtil;
import com.appsinnova.admin.business.common.utils.chai.ChaiSpecUtil;
import com.appsinnova.admin.business.domain.chai.ChaiBrand;
import com.appsinnova.admin.business.domain.chai.ChaiSku;
import com.appsinnova.admin.business.domain.chai.ChaiStock;
import com.appsinnova.admin.business.domain.chai.ChaiSpu;
import com.appsinnova.admin.business.repository.chai.ChaiSkuRepository;
import com.appsinnova.admin.business.repository.chai.ChaiStockRepository;
import com.appsinnova.admin.common.data.PageSort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 茶叶价目只读视图：SKU 为主表，关联库存合计。
 */
@Service
@RequiredArgsConstructor
public class ChaiSkuViewService {

    private static final int RECYCLE_HALF_YEAR_COUNT = 6;

    private final ChaiSkuRepository chaiSkuRepository;
    private final ChaiStockRepository chaiStockRepository;
    private final ChaiBrandService chaiBrandService;
    private final ChaiSpuService chaiSpuService;

    public Page<ChaiSku> getPageList(ChaiSku param, ChaiSkuViewType viewType) {
        if (param == null) {
            param = new ChaiSku();
        }
        resolveSpuCodeFilter(param);

        List<Sort.Order> orders = new ArrayList<>();
        orders.add(new Sort.Order(Sort.Direction.DESC, "year"));
        orders.add(new Sort.Order(Sort.Direction.DESC, "prodBatch"));
        orders.add(new Sort.Order(Sort.Direction.DESC, "id"));
        PageRequest pageRequest = PageSort.pageRequest(orders);

        final ChaiSku queryParam = param;
        final boolean recentHalfYearOnly = viewType != null && viewType.isRecentHalfYearOnly();
        Page<ChaiSku> page = chaiSkuRepository.findAll(
                (Root<ChaiSku> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
                    List<Predicate> preList = genCondition(root, cb, queryParam, recentHalfYearOnly);
                    return query.where(preList.toArray(new Predicate[0])).getRestriction();
                }, pageRequest);

        Map<Long, String> brandNameMap = buildBrandNameMap();
        page.forEach(item -> fillShowFields(item, brandNameMap));
        fillStockTotalQty(page.getContent());
        return page;
    }

    /**
     * 同款弹窗：同 SPU 下未删除且上架；不套回收端「最近 6 个半年」，便于看全半年。
     */
    public List<ChaiSku> listSameSpu(Long spuId) {
        if (spuId == null) {
            return new ArrayList<>();
        }
        Sort sort = Sort.by(
                new Sort.Order(Sort.Direction.DESC, "year"),
                new Sort.Order(Sort.Direction.DESC, "prodBatch"),
                new Sort.Order(Sort.Direction.DESC, "id"));
        List<ChaiSku> list = chaiSkuRepository.findAll(
                (Root<ChaiSku> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
                    List<Predicate> preList = new ArrayList<>();
                    preList.add(cb.equal(root.get("deleted").as(Integer.class), 0));
                    preList.add(cb.equal(root.get("status").as(Integer.class), ChaiStatus.ONLINE.getCode()));
                    preList.add(cb.equal(root.get("spuId").as(Long.class), spuId));
                    return query.where(preList.toArray(new Predicate[0])).getRestriction();
                }, sort);
        Map<Long, String> brandNameMap = buildBrandNameMap();
        list.forEach(item -> fillShowFields(item, brandNameMap));
        fillStockTotalQty(list);
        return list;
    }

    private void resolveSpuCodeFilter(ChaiSku param) {
        if (!StringUtils.hasText(param.getQuerySpuCode())) {
            return;
        }
        ChaiSpu parent = chaiSpuService.getBySpuCodeIncludeDeleted(param.getQuerySpuCode().trim());
        if (parent == null) {
            param.setSpuId(-1L);
        } else if (param.getSpuId() == null) {
            param.setSpuId(parent.getId());
        }
    }

    private List<Predicate> genCondition(Root<ChaiSku> root, CriteriaBuilder cb,
                                         ChaiSku param, boolean recentHalfYearOnly) {
        List<Predicate> preList = new ArrayList<>();
        // 价目视图硬性：未删除 + 上架
        preList.add(cb.equal(root.get("deleted").as(Integer.class), 0));
        preList.add(cb.equal(root.get("status").as(Integer.class), ChaiStatus.ONLINE.getCode()));

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
        if (param.getYear() != null) {
            preList.add(cb.equal(root.get("year").as(Integer.class), param.getYear()));
        }
        if (param.getProdBatch() != null) {
            preList.add(cb.equal(root.get("prodBatch").as(Integer.class), param.getProdBatch()));
        }
        if (param.getStarLevel() != null) {
            preList.add(cb.equal(root.get("starLevel").as(Integer.class), param.getStarLevel()));
        }

        if (recentHalfYearOnly) {
            List<ChaiHalfYearUtil.HalfYear> halfYears = ChaiHalfYearUtil.recentFromNow(RECYCLE_HALF_YEAR_COUNT);
            List<Predicate> halfOr = new ArrayList<>();
            for (ChaiHalfYearUtil.HalfYear halfYear : halfYears) {
                halfOr.add(cb.and(
                        cb.equal(root.get("year").as(Integer.class), halfYear.getYear()),
                        cb.equal(root.get("prodBatch").as(Integer.class), halfYear.getProdBatch())
                ));
            }
            if (halfOr.isEmpty()) {
                preList.add(cb.disjunction());
            } else {
                preList.add(cb.or(halfOr.toArray(new Predicate[0])));
            }
        }
        return preList;
    }

    private void fillStockTotalQty(List<ChaiSku> list) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        List<Long> skuIds = list.stream().map(ChaiSku::getId).collect(Collectors.toList());
        Map<Long, Integer> qtyMap = new HashMap<>();
        for (ChaiStock stock : chaiStockRepository.findBySkuIdIn(skuIds)) {
            if (stock.getSkuId() != null) {
                qtyMap.put(stock.getSkuId(), stock.getTotalQty() != null ? stock.getTotalQty() : 0);
            }
        }
        for (ChaiSku sku : list) {
            sku.setStockTotalQty(qtyMap.getOrDefault(sku.getId(), 0));
        }
    }

    private void fillShowFields(ChaiSku item, Map<Long, String> brandNameMap) {
        if (item.getBrand() != null) {
            String brandName = brandNameMap.get(item.getBrand());
            if (brandName == null) {
                ChaiBrand brand = chaiBrandService.getById(item.getBrand());
                brandName = brand != null ? brand.getName() : String.valueOf(item.getBrand());
                brandNameMap.put(item.getBrand(), brandName);
            }
            item.setBrandName(brandName);
        } else {
            item.setBrandName("-");
        }
        item.setSpecShow(ChaiSpecUtil.toShow(item.getSpec()));
        item.setShowImageList(ChaiFormHelper.parseUrlList(item.getShowImageUrls()));
        item.setRealImageList(ChaiFormHelper.parseUrlList(item.getRealImageUrls()));
        ChaiPriceUtil.fillListShow(item);
    }

    private Map<Long, String> buildBrandNameMap() {
        Map<Long, String> map = new HashMap<>();
        for (ChaiBrand brand : chaiBrandService.listOnlineOrdered()) {
            map.put(brand.getId(), brand.getName());
        }
        return map;
    }
}
