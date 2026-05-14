package com.appsinnova.admin.business.service.tea;

import com.appsinnova.admin.business.common.enums.SkuStatus;
import com.appsinnova.admin.business.domain.tea.TeaSku;
import com.appsinnova.admin.business.repository.tea.TeaSkuRepository;
import com.appsinnova.admin.business.vo.tea.TeaSkuStatVo;
import com.appsinnova.admin.common.utils.DictUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 茶品 SKU 仪表盘统计（与 {@link TeaQuoteOrderDashboardService} 职责划分一致，供首页与茶品概览等使用）
 */
@Service
@RequiredArgsConstructor
public class TeaSkuDashboardService {

    private final TeaSkuRepository teaSkuRepository;

    /**
     * 汇总 SKU 总数、上下架、星级分布、品牌分布等仪表盘数据
     */
    public TeaSkuStatVo buildStatOverview() {
        List<TeaSku> skuList = teaSkuRepository.findAll();
        TeaSkuStatVo statVo = new TeaSkuStatVo();
        if (CollectionUtils.isEmpty(skuList)) {
            return statVo;
        }

        statVo.setTotalCount((long) skuList.size());

        long onlineCount = skuList.stream()
                .filter(item -> item.getStatus() != null && item.getStatus().equals(SkuStatus.ONLINE.getCode()))
                .count();
        statVo.setOnlineCount(onlineCount);
        statVo.setOfflineCount(statVo.getTotalCount() - onlineCount);

        Map<Integer, Long> starLevelCountMap = new HashMap<>();
        for (TeaSku sku : skuList) {
            int key = sku.getStarLevel() == null ? -1 : sku.getStarLevel();
            starLevelCountMap.merge(key, 1L, Long::sum);
        }
        List<TeaSkuStatVo.StarLevelStatItem> starLevelStatList = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : starLevelCountMap.entrySet()) {
            int key = entry.getKey();
            String label;
            if (key == -1) {
                label = "未设置";
            } else {
                label = DictUtils.keyValue("STAR_LEVEL", String.valueOf(key));
                if (!StringUtils.hasText(label)) {
                    label = String.valueOf(key);
                }
            }
            starLevelStatList.add(new TeaSkuStatVo.StarLevelStatItem(
                    key == -1 ? null : key,
                    entry.getValue(),
                    label
            ));
        }
        starLevelStatList.sort(Comparator.comparing(
                (TeaSkuStatVo.StarLevelStatItem i) -> i.getStarLevel() == null ? Integer.MAX_VALUE : i.getStarLevel(),
                Comparator.naturalOrder()
        ));
        statVo.setStarLevelStatList(starLevelStatList);

        Map<Integer, Long> brandCountMap = new LinkedHashMap<>();
        for (TeaSku sku : skuList) {
            if (sku.getBrand() == null) {
                continue;
            }
            brandCountMap.put(sku.getBrand(), brandCountMap.getOrDefault(sku.getBrand(), 0L) + 1L);
        }

        List<TeaSkuStatVo.BrandStatItem> brandStatList = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : brandCountMap.entrySet()) {
            brandStatList.add(new TeaSkuStatVo.BrandStatItem(entry.getKey(), entry.getValue()));
        }
        brandStatList.sort(Comparator.comparingLong(TeaSkuStatVo.BrandStatItem::getCount).reversed());
        statVo.setBrandStatList(brandStatList);

        return statVo;
    }
}
