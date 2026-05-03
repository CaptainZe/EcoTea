package com.appsinnova.admin.business.service.tea;

import com.appsinnova.admin.business.common.enums.DailySeqType;
import com.appsinnova.admin.business.common.enums.SkuStatus;
import com.appsinnova.admin.business.common.enums.tea.AppearanceCondition;
import com.appsinnova.admin.business.common.enums.tea.HasBag;
import com.appsinnova.admin.business.common.enums.tea.TeaQuoteOrderStatus;
import com.appsinnova.admin.business.common.utils.TimeUtils;
import com.appsinnova.admin.business.domain.tea.TeaQuoteOrder;
import com.appsinnova.admin.business.domain.tea.TeaQuoteOrderItem;
import com.appsinnova.admin.business.domain.tea.TeaSku;
import com.appsinnova.admin.business.repository.tea.TeaQuoteOrderItemRepository;
import com.appsinnova.admin.business.repository.tea.TeaQuoteOrderRepository;
import com.appsinnova.admin.business.service.sys.DailySequenceService;
import com.appsinnova.admin.business.vo.base.PayInfoVo;
import com.appsinnova.admin.business.vo.tea.TeaQuoteOrderSubmitVo;
import com.appsinnova.admin.business.vo.tea.TeaQuoteOrderSupplementVo;
import com.appsinnova.admin.business.vo.tea.TeaQuoteSkuQuoteVo;
import com.appsinnova.admin.common.data.PageSort;
import com.appsinnova.admin.common.utils.DictUtils;
import com.appsinnova.admin.common.utils.JsonUtils;
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
public class TeaQuoteOrderService {

    private final TeaSkuService teaSkuService;
    private final TeaQuoteOrderRepository teaQuoteOrderRepository;
    private final TeaQuoteOrderItemRepository teaQuoteOrderItemRepository;
    private final DailySequenceService dailySequenceService;

    // 通用分页：按报单号（模糊）、用户、状态筛选；排序 createTime 倒序
    public Page<TeaQuoteOrder> getPageList(TeaQuoteOrder param) {
        List<Sort.Order> orders = new ArrayList<>();
        orders.add(new Sort.Order(Sort.Direction.DESC, "createTime"));
        PageRequest page = PageSort.pageRequest(orders);
        return teaQuoteOrderRepository.findAll((Root<TeaQuoteOrder> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> preList = this.genCondition(root, cb, param);
            Predicate[] pres = new Predicate[preList.size()];
            return query.where(preList.toArray(pres)).getRestriction();
        }, page);
    }

    // 组装查询条件：orderNo 非空时 like；userId、status 非空时精确匹配
    private List<Predicate> genCondition(Root<TeaQuoteOrder> root, CriteriaBuilder cb, TeaQuoteOrder param) {
        List<Predicate> preList = new ArrayList<>();
        if (param == null) {
            return preList;
        }
        if (param.getUserId() != null) {
            preList.add(cb.equal(root.get("userId").as(Long.class), param.getUserId()));
        }
        if (param.getStatus() != null) {
            preList.add(cb.equal(root.get("status").as(Integer.class), param.getStatus()));
        }
        if (StringUtils.hasText(param.getOrderNo())) {
            preList.add(cb.like(root.get("orderNo").as(String.class), "%" + param.getOrderNo().trim() + "%"));
        }
        return preList;
    }

    public TeaQuoteOrder getByIdAndUserId(Long orderId, Long userId) {
        return teaQuoteOrderRepository.findById(orderId)
                .filter(o -> userId != null && userId.equals(o.getUserId()))
                .orElse(null);
    }

    public List<TeaQuoteOrderItem> getItemListByOrderId(Long orderId) {
        if (orderId == null) {
            return new ArrayList<>();
        }
        return teaQuoteOrderItemRepository.findByOrderIdOrderByIdAsc(orderId);
    }

    /**
     * 取消：仅【已提交】状态（未到【已确认】）可操作。
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrderForUser(Long orderId, Long userId) {
        TeaQuoteOrder order = getByIdAndUserId(orderId, userId);
        if (order == null) {
            throw new IllegalArgumentException("报价单不存在或无权操作");
        }
        if (!TeaQuoteOrderStatus.SUBMITTED.getCode().equals(order.getStatus())) {
            throw new IllegalArgumentException("当前状态不可取消");
        }
        order.setStatus(TeaQuoteOrderStatus.CANCELLED.getCode());
        order.setUpdateTime(System.currentTimeMillis());
        teaQuoteOrderRepository.save(order);
    }

    /**
     * 补充快递与打款信息：状态须在【已验收】之前（不含已验收）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateExpressAndPayForUser(Long orderId, Long userId, TeaQuoteOrderSupplementVo vo) {
        TeaQuoteOrder order = getByIdAndUserId(orderId, userId);
        if (order == null) {
            throw new IllegalArgumentException("报价单不存在或无权操作");
        }
        if (order.getStatus() == null || order.getStatus() >= TeaQuoteOrderStatus.ACCEPTED.getCode()) {
            throw new IllegalArgumentException("当前状态不可补充快递或打款信息");
        }
        long now = System.currentTimeMillis();
        order.setExpressCompany(vo.getExpressCompany());
        order.setExpressNo(vo.getExpressNo() == null ? "" : vo.getExpressNo());
        if (vo.getPayMethod() != null) {
            order.setPayMethod(vo.getPayMethod());
        }
        if (vo.getPayInfo() != null) {
            order.setPayInfo(JsonUtils.writeValueAsString(vo.getPayInfo()));
        }
        order.setUpdateTime(now);
        teaQuoteOrderRepository.save(order);
    }

    public PayInfoVo parsePayInfo(String payInfoJson) {
        if (org.apache.commons.lang3.StringUtils.isBlank(payInfoJson)) {
            return new PayInfoVo();
        }
        PayInfoVo vo = JsonUtils.readValue(payInfoJson, PayInfoVo.class);
        return vo != null ? vo : new PayInfoVo();
    }

    @Transactional(rollbackFor = Exception.class)
    public String createQuoteOrder(TeaQuoteOrderSubmitVo submitVo, String operator) {
        List<TeaQuoteOrderItem> orderItemList = this.buildOrderItemList(submitVo);
        if (CollectionUtils.isEmpty(orderItemList)) {
            throw new IllegalArgumentException("请至少填写一个商品数量");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalQuantity = 0;
        for (TeaQuoteOrderItem item : orderItemList) {
            totalAmount = totalAmount.add(item.getAmount());
            totalQuantity += item.getQuantity();
        }

        long now = System.currentTimeMillis();
        TeaQuoteOrder order = new TeaQuoteOrder();
        order.setOrderNo(this.generateOrderNo(now));
        order.setUserId(submitVo.getUserId());
        order.setUserName(submitVo.getUserName());
        order.setExpressCompany(submitVo.getExpressCompany());
        order.setExpressNo(submitVo.getExpressNo());
        order.setPayMethod(submitVo.getPayMethod());
        order.setPayInfo(JsonUtils.writeValueAsString(submitVo.getPayInfo()));
        order.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
        order.setTotalQuantity(totalQuantity);
        order.setStatus(TeaQuoteOrderStatus.SUBMITTED.getCode());
        order.setOperator(operator);
        order.setCreateTime(now);
        order.setUpdateTime(now);
        order = teaQuoteOrderRepository.save(order);

        for (TeaQuoteOrderItem item : orderItemList) {
            item.setOrderId(order.getId());
            item.setCreateTime(now);
            item.setUpdateTime(now);
        }
        teaQuoteOrderItemRepository.saveAll(orderItemList);
        return order.getOrderNo();
    }

    private List<TeaQuoteOrderItem> buildOrderItemList(TeaQuoteOrderSubmitVo submitVo) {
        List<TeaQuoteOrderItem> orderItemList = new ArrayList<>();
        if (submitVo == null || CollectionUtils.isEmpty(submitVo.getItemList())) {
            return orderItemList;
        }

        List<Long> skuIdList = new ArrayList<>();
        for (TeaQuoteSkuQuoteVo item : submitVo.getItemList()) {
            if (item.getSkuId() != null) {
                skuIdList.add(item.getSkuId());
            }
        }
        List<TeaSku> skuList = teaSkuService.getByIdIn(skuIdList);
        Map<Long, TeaSku> skuMap = new LinkedHashMap<>();
        for (TeaSku sku : skuList) {
            if (sku.getId() != null) {
                skuMap.put(sku.getId(), sku);
            }
        }

        for (TeaQuoteSkuQuoteVo item : submitVo.getItemList()) {
            if (item.getSkuId() == null) {
                continue;
            }
            TeaSku sku = skuMap.get(item.getSkuId());
            if (sku == null || sku.getStatus() == null || !sku.getStatus().equals(SkuStatus.ONLINE.getCode())) {
                continue;
            }

            BigDecimal noBagReduce = sku.getRecyclePriceReduceNoBag() == null ? BigDecimal.ZERO : sku.getRecyclePriceReduceNoBag();
            BigDecimal completeBagUnitPrice = defaultPrice(sku.getRecyclePrice());
            BigDecimal completeNoBagUnitPrice = nonNegative(completeBagUnitPrice.subtract(noBagReduce));
            BigDecimal brokenBagUnitPrice = this.calculateBrokenUnitPrice(sku);
            BigDecimal brokenNoBagUnitPrice = nonNegative(brokenBagUnitPrice.subtract(noBagReduce));

            this.tryAddOrderItem(orderItemList, sku, AppearanceCondition.COMPLETE, HasBag.YES,
                    completeBagUnitPrice, item.getQtyCompleteBag());
            this.tryAddOrderItem(orderItemList, sku, AppearanceCondition.COMPLETE, HasBag.NO,
                    completeNoBagUnitPrice, item.getQtyCompleteNoBag());
            this.tryAddOrderItem(orderItemList, sku, AppearanceCondition.DAMAGED, HasBag.YES,
                    brokenBagUnitPrice, item.getQtyBrokenBag());
            this.tryAddOrderItem(orderItemList, sku, AppearanceCondition.DAMAGED, HasBag.NO,
                    brokenNoBagUnitPrice, item.getQtyBrokenNoBag());
        }

        return orderItemList;
    }

    private BigDecimal calculateBrokenUnitPrice(TeaSku sku) {
        BigDecimal basePrice = defaultPrice(sku.getRecyclePrice());
        Integer reducePer = sku.getRecyclePriceReducePer() == null ? 0 : sku.getRecyclePriceReducePer();
        BigDecimal reduceRate = BigDecimal.valueOf(reducePer).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return nonNegative(basePrice.multiply(BigDecimal.ONE.subtract(reduceRate))).setScale(2, RoundingMode.HALF_UP);
    }

    private void tryAddOrderItem(List<TeaQuoteOrderItem> orderItemList, TeaSku sku,
                                   AppearanceCondition appearanceCondition, HasBag hasBag,
                                   BigDecimal unitPrice, Integer quantity) {
        int qty = quantity == null ? 0 : quantity;
        if (qty <= 0) {
            return;
        }
        TeaQuoteOrderItem orderItem = new TeaQuoteOrderItem();
        orderItem.setSkuId(sku.getId());
        orderItem.setSkuCode(sku.getSkuCode());
        orderItem.setSkuName(sku.getName());
        orderItem.setSkuBrand(DictUtils.keyValue("TEA_BRAND", String.valueOf(sku.getBrand())));
        orderItem.setSkuSpec(sku.getSpec());
        orderItem.setSkuProductionBatch(sku.getProductionBatch());
        orderItem.setAppearanceCondition(appearanceCondition.getCode());
        orderItem.setHasBag(hasBag.getCode());
        orderItem.setBaseRecyclePrice(unitPrice.setScale(2, RoundingMode.HALF_UP));
        orderItem.setQuantity(qty);
        orderItem.setAmount(unitPrice.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP));
        orderItemList.add(orderItem);
    }

    private String generateOrderNo(long timestamp) {
        String date = TimeUtils.getDateYYMMdd(timestamp);
        Integer seq = dailySequenceService.getCurrentAndIncrement(DailySeqType.QUOTE_ORDER_NO);
        return String.format("TEA-QO-%s-%04d", date, seq);
    }

    private static BigDecimal defaultPrice(BigDecimal price) {
        return price == null ? BigDecimal.ZERO : price;
    }

    private static BigDecimal nonNegative(BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : value;
    }
}
