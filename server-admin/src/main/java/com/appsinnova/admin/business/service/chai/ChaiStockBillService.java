package com.appsinnova.admin.business.service.chai;

import com.appsinnova.admin.business.common.enums.base.YesOrNo;
import com.appsinnova.admin.business.common.enums.chai.ChaiStatus;
import com.appsinnova.admin.business.common.enums.chai.ChaiStockBillStatus;
import com.appsinnova.admin.business.common.enums.chai.ChaiStockBillType;
import com.appsinnova.admin.business.common.enums.chai.ChaiStockReason;
import com.appsinnova.admin.business.common.utils.RedisSeqUtils;
import com.appsinnova.admin.business.common.utils.chai.ChaiStockBillNoUtil;
import com.appsinnova.admin.business.domain.chai.*;
import com.appsinnova.admin.business.repository.chai.ChaiStockBillItemRepository;
import com.appsinnova.admin.business.repository.chai.ChaiStockBillRepository;
import com.appsinnova.admin.business.vo.chai.ChaiStockBillSaveVo;
import com.appsinnova.admin.common.data.PageSort;
import com.appsinnova.admin.business.common.utils.JsonUtils;
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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChaiStockBillService {

    private final ChaiStockBillRepository chaiStockBillRepository;
    private final ChaiStockBillItemRepository chaiStockBillItemRepository;
    private final ChaiStockService chaiStockService;
    private final ChaiSkuService chaiSkuService;
    private final ChaiStaffService chaiStaffService;
    private final ChaiWarehouseService chaiWarehouseService;

    public ChaiStockBill getById(Long id) {
        return chaiStockBillRepository.findById(id).orElse(null);
    }

    public List<ChaiStockBillItem> listItems(Long billId) {
        if (billId == null) {
            return new ArrayList<>();
        }
        return chaiStockBillItemRepository.findByBillIdOrderByIdAsc(billId);
    }

    public Page<ChaiStockBill> getPageList(ChaiStockBill param) {
        List<Sort.Order> orders = new ArrayList<>();
        orders.add(new Sort.Order(Sort.Direction.DESC, "createTime"));
        orders.add(new Sort.Order(Sort.Direction.DESC, "id"));
        PageRequest page = PageSort.pageRequest(orders);
        return chaiStockBillRepository.findAll((Root<ChaiStockBill> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> preList = genCondition(root, cb, param);
            return query.where(preList.toArray(new Predicate[0])).getRestriction();
        }, page);
    }

    /**
     * 保存即过账
     */
    @Transactional(rollbackFor = Exception.class)
    public ChaiStockBill post(ChaiStockBillSaveVo saveVo, String operator) {
        validateSaveVo(saveVo);
        ChaiStockBillType billType = ChaiStockBillType.fromCode(saveVo.getBillType());

        Long handlerId = saveVo.getHandlerId() == null ? 0L : saveVo.getHandlerId();
        if (handlerId <= 0) {
            throw new IllegalArgumentException("请选择经手人");
        }
        ChaiStaff handler = chaiStaffService.getById(handlerId);
        if (handler == null) {
            throw new IllegalArgumentException("经手人不存在");
        }
        if (!ChaiStatus.isOnline(handler.getStatus())) {
            throw new IllegalArgumentException("经手人已下架，请重新选择");
        }
        String handlerName = ChaiStaffService.formatDisplayName(handler);

        ChaiWarehouse fromWh = chaiWarehouseService.getById(saveVo.getFromWhId());
        if (fromWh == null || !ChaiStatus.isOnline(fromWh.getStatus())) {
            throw new IllegalArgumentException("仓库不存在或已下架");
        }
        Long toWhId = saveVo.getToWhId() == null ? 0L : saveVo.getToWhId();
        if (billType == ChaiStockBillType.TRANSFER) {
            ChaiWarehouse toWh = chaiWarehouseService.getById(toWhId);
            if (toWh == null || !ChaiStatus.isOnline(toWh.getStatus())) {
                throw new IllegalArgumentException("调入仓库不存在或已下架");
            }
            if (saveVo.getFromWhId().equals(toWhId)) {
                throw new IllegalArgumentException("调出仓与调入仓不能相同");
            }
        }

        List<ChaiStockBillItem> preparedItems = new ArrayList<>();
        int totalQty = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (ChaiStockBillItem line : saveVo.getItemList()) {
            ChaiSku sku = chaiSkuService.getById(line.getSkuId());
            if (sku == null) {
                throw new IllegalArgumentException("SKU不存在：" + line.getSkuId());
            }
            if (billType == ChaiStockBillType.IN && YesOrNo.isYes(sku.getDeleted())) {
                throw new IllegalArgumentException("已删除的SKU不能入库，请先恢复：" + sku.getSkuCode());
            }
            int qty = line.getQty();
            int appearanceDamaged = YesOrNo.isYes(line.getAppearanceDamaged())
                    ? YesOrNo.YES.getCode() : YesOrNo.NO.getCode();
            BigDecimal price = line.getPrice() == null ? BigDecimal.ZERO : line.getPrice();
            if (price.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("单价不能为负数");
            }
            BigDecimal amount = price.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
            ChaiStockBillItem item = new ChaiStockBillItem();
            item.setSkuId(sku.getId());
            item.setSkuCode(sku.getSkuCode() != null ? sku.getSkuCode() : "");
            item.setName(sku.getName() != null ? sku.getName() : "");
            item.setQty(qty);
            item.setAppearanceDamaged(appearanceDamaged);
            item.setPrice(price);
            item.setAmount(amount);
            item.setRemark(StringUtils.hasText(line.getRemark()) ? line.getRemark().trim() : "");
            item.setSkuSnap(buildSkuSnap(sku));
            preparedItems.add(item);
            totalQty += qty;
            if (billType != ChaiStockBillType.TRANSFER) {
                totalAmount = totalAmount.add(amount);
            }
        }

        long now = System.currentTimeMillis();
        ChaiStockBill bill = new ChaiStockBill();
        bill.setBillNo(nextBillNo(billType));
        bill.setBillType(billType.getCode());
        bill.setStatus(ChaiStockBillStatus.POSTED.getCode());
        bill.setReason(saveVo.getReason());
        bill.setHandlerId(handlerId);
        bill.setHandlerName(handlerName);
        bill.setFromWhId(saveVo.getFromWhId());
        bill.setToWhId(billType == ChaiStockBillType.TRANSFER ? toWhId : 0L);
        bill.setTotalQty(totalQty);
        bill.setTotalAmount(billType == ChaiStockBillType.TRANSFER ? BigDecimal.ZERO : totalAmount);
        bill.setRemark(StringUtils.hasText(saveVo.getRemark()) ? saveVo.getRemark().trim() : "");
        bill.setOperator(operator != null ? operator : "");
        bill.setCreateTime(now);
        bill.setUpdateTime(now);
        bill = chaiStockBillRepository.save(bill);

        for (ChaiStockBillItem item : preparedItems) {
            item.setBillId(bill.getId());
            chaiStockBillItemRepository.save(item);
            applyStockChange(billType, saveVo.getFromWhId(), toWhId, item.getSkuId(), item.getQty(),
                    YesOrNo.isYes(item.getAppearanceDamaged()), operator, false);
        }
        return bill;
    }

    @Transactional(rollbackFor = Exception.class)
    public void voidBill(Long billId, String operator) {
        ChaiStockBill bill = requireBill(billId);
        if (!ChaiStockBillStatus.POSTED.getCode().equals(bill.getStatus())) {
            throw new IllegalArgumentException("仅已过账单据可作废");
        }
        ChaiStockBillType billType = ChaiStockBillType.fromCode(bill.getBillType());
        List<ChaiStockBillItem> items = listItems(billId);
        for (ChaiStockBillItem item : items) {
            applyStockChange(billType, bill.getFromWhId(), bill.getToWhId(),
                    item.getSkuId(), item.getQty(),
                    YesOrNo.isYes(item.getAppearanceDamaged()), operator, true);
        }
        bill.setStatus(ChaiStockBillStatus.VOIDED.getCode());
        bill.setOperator(operator != null ? operator : "");
        bill.setUpdateTime(System.currentTimeMillis());
        chaiStockBillRepository.save(bill);
    }

    @Transactional(rollbackFor = Exception.class)
    public void archiveBill(Long billId, String operator) {
        ChaiStockBill bill = requireBill(billId);
        if (!ChaiStockBillStatus.POSTED.getCode().equals(bill.getStatus())) {
            throw new IllegalArgumentException("仅已过账单据可归档");
        }
        bill.setStatus(ChaiStockBillStatus.ARCHIVED.getCode());
        bill.setOperator(operator != null ? operator : "");
        bill.setUpdateTime(System.currentTimeMillis());
        chaiStockBillRepository.save(bill);
    }

    private String nextBillNo(ChaiStockBillType billType) {
        RedisSeqUtils.MaxSeqLoader loader = date -> {
            ChaiStockBill latest = chaiStockBillRepository.findFirstByBillNoStartingWithOrderByBillNoDesc(
                    ChaiStockBillNoUtil.noPrefix(billType, date));
            return latest == null ? 0L : ChaiStockBillNoUtil.parseSeq(latest.getBillNo());
        };
        for (int i = 0; i < 5; i++) {
            String billNo = ChaiStockBillNoUtil.next(billType, loader);
            if (!chaiStockBillRepository.existsByBillNo(billNo)) {
                return billNo;
            }
        }
        throw new IllegalStateException("生成单据号失败，请重试");
    }

    private void applyStockChange(ChaiStockBillType billType, Long fromWhId, Long toWhId,
                                  Long skuId, int qty, boolean appearanceDamaged,
                                  String operator, boolean reverse) {
        int sign = reverse ? -1 : 1;
        switch (billType) {
            case IN:
                // 入库：from_wh 为入到哪
                chaiStockService.applyWhDelta(skuId, fromWhId, sign * qty, appearanceDamaged, operator);
                break;
            case OUT:
                chaiStockService.applyWhDelta(skuId, fromWhId, -sign * qty, appearanceDamaged, operator);
                break;
            case TRANSFER:
                chaiStockService.applyWhDelta(skuId, fromWhId, -sign * qty, appearanceDamaged, operator);
                chaiStockService.applyWhDelta(skuId, toWhId, sign * qty, appearanceDamaged, operator);
                break;
            default:
                throw new IllegalArgumentException("不支持的单据类型");
        }
    }

    private void validateSaveVo(ChaiStockBillSaveVo saveVo) {
        if (saveVo == null) {
            throw new IllegalArgumentException("单据不能为空");
        }
        ChaiStockBillType billType = ChaiStockBillType.fromCode(saveVo.getBillType());
        if (billType == null) {
            throw new IllegalArgumentException("单据类型无效");
        }
        if (!ChaiStockReason.matchesBillType(saveVo.getReason(), saveVo.getBillType())) {
            throw new IllegalArgumentException("事由与单据类型不匹配");
        }
        if (saveVo.getFromWhId() == null || saveVo.getFromWhId() <= 0) {
            throw new IllegalArgumentException("仓库必选");
        }
        if (billType == ChaiStockBillType.TRANSFER) {
            if (saveVo.getToWhId() == null || saveVo.getToWhId() <= 0) {
                throw new IllegalArgumentException("调入仓库必选");
            }
        } else {
            saveVo.setToWhId(0L);
        }
        if (CollectionUtils.isEmpty(saveVo.getItemList())) {
            throw new IllegalArgumentException("请至少添加一行明细");
        }
        for (ChaiStockBillItem line : saveVo.getItemList()) {
            if (line == null || line.getSkuId() == null) {
                throw new IllegalArgumentException("明细SKU必选");
            }
            if (line.getQty() == null || line.getQty() <= 0) {
                throw new IllegalArgumentException("明细件数必须大于0");
            }
        }
    }

    private ChaiStockBill requireBill(Long billId) {
        if (billId == null) {
            throw new IllegalArgumentException("单据不存在");
        }
        ChaiStockBill bill = getById(billId);
        if (bill == null) {
            throw new IllegalArgumentException("单据不存在");
        }
        return bill;
    }

    private String buildSkuSnap(ChaiSku sku) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", sku.getId());
        map.put("spu_id", sku.getSpuId());
        map.put("sku_code", sku.getSkuCode());
        map.put("star_level", sku.getStarLevel());
        map.put("name", sku.getName());
        map.put("brand", sku.getBrand());
        map.put("expiration", sku.getExpiration());
        map.put("type", sku.getType());
        map.put("grade", sku.getGrade());
        map.put("year", sku.getYear());
        map.put("prod_batch", sku.getProdBatch());
        map.put("spec", sku.getSpec());
        map.put("show_image_urls", sku.getShowImageUrls());
        map.put("real_image_urls", sku.getRealImageUrls());
        map.put("official_price", sku.getOfficialPrice());
        map.put("sale_price", sku.getSalePrice());
        map.put("recycle_price", sku.getRecyclePrice());
        map.put("recycle_price_reduce_per", sku.getRecyclePriceReducePer());
        map.put("recycle_price_reduce_no_bag", sku.getRecyclePriceReduceNoBag());
        map.put("status", sku.getStatus());
        map.put("deleted", sku.getDeleted());
        String json = JsonUtils.writeValueAsString(map);
        return json != null ? json : "{}";
    }

    private List<Predicate> genCondition(Root<ChaiStockBill> root, CriteriaBuilder cb, ChaiStockBill param) {
        List<Predicate> preList = new ArrayList<>();
        if (param == null) {
            return preList;
        }
        if (StringUtils.hasText(param.getBillNo())) {
            preList.add(cb.like(root.get("billNo").as(String.class), "%" + param.getBillNo().trim() + "%"));
        }
        if (param.getBillType() != null) {
            preList.add(cb.equal(root.get("billType").as(Integer.class), param.getBillType()));
        }
        if (param.getStatus() != null) {
            preList.add(cb.equal(root.get("status").as(Integer.class), param.getStatus()));
        }
        if (param.getReason() != null) {
            preList.add(cb.equal(root.get("reason").as(Integer.class), param.getReason()));
        }
        if (param.getFromWhId() != null) {
            preList.add(cb.equal(root.get("fromWhId").as(Long.class), param.getFromWhId()));
        }
        if (param.getHandlerId() != null) {
            preList.add(cb.equal(root.get("handlerId").as(Long.class), param.getHandlerId()));
        }
        if (StringUtils.hasText(param.getHandlerName())) {
            preList.add(cb.like(root.get("handlerName").as(String.class),
                    "%" + param.getHandlerName().trim() + "%"));
        }
        return preList;
    }
}
