package com.appsinnova.admin.business.service.chai.dashboard;

import com.appsinnova.admin.business.common.enums.chai.ChaiStatus;
import com.appsinnova.admin.business.common.enums.chai.ChaiStockBillStatus;
import com.appsinnova.admin.business.common.enums.chai.ChaiStockBillType;
import com.appsinnova.admin.business.domain.chai.ChaiStockBill;
import com.appsinnova.admin.business.repository.chai.ChaiSkuRepository;
import com.appsinnova.admin.business.repository.chai.ChaiSpuRepository;
import com.appsinnova.admin.business.repository.chai.ChaiStockBillRepository;
import com.appsinnova.admin.business.repository.chai.ChaiStockRepository;
import com.appsinnova.admin.business.repository.chai.ChaiWarehouseRepository;
import com.appsinnova.admin.business.vo.chai.dashboard.ChaiDashboardStackedBarVo;
import com.appsinnova.admin.business.vo.chai.dashboard.ChaiDashboardStackedSeriesVo;
import com.appsinnova.admin.business.vo.chai.dashboard.ChaiDashboardVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChaiDashboardService {

    private static final int DEFAULT_STAT_DAYS = 7;
    private static final List<Integer> ALLOWED_STAT_DAYS = Arrays.asList(7, 15, 30);
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("MM-dd");

    private final ChaiSpuRepository chaiSpuRepository;
    private final ChaiSkuRepository chaiSkuRepository;
    private final ChaiStockRepository chaiStockRepository;
    private final ChaiWarehouseRepository chaiWarehouseRepository;
    private final ChaiStockBillRepository chaiStockBillRepository;

    public ChaiDashboardVo getDashboard(Integer statDays) {
        int days = resolveStatDays(statDays);
        long startMs = startMsForDays(days);
        long endMs = System.currentTimeMillis();
        Integer posted = ChaiStockBillStatus.POSTED.getCode();

        ChaiDashboardVo dashboard = new ChaiDashboardVo();
        dashboard.setStatDays(days);
        dashboard.setScopeTip("茶叶库存运营概览：近 " + days + " 天库存单据统计（仅已过账）");

        long spuCount = chaiSpuRepository.countByDeleted(0);
        long skuCount = chaiSkuRepository.countByDeleted(0);
        long onlineSkuCount = chaiSkuRepository.countByDeletedAndStatus(0, ChaiStatus.ONLINE.getCode());
        dashboard.setSpuCount(spuCount);
        dashboard.setSkuCount(skuCount);
        dashboard.setOnlineSkuCount(onlineSkuCount);
        dashboard.setOfflineSkuCount(Math.max(0L, skuCount - onlineSkuCount));

        Long totalQty = chaiStockRepository.sumTotalQty();
        dashboard.setStockTotalQty(totalQty != null ? totalQty : 0L);
        dashboard.setStockSkuCount(chaiStockRepository.count());
        dashboard.setPositiveStockSkuCount(chaiStockRepository.countByTotalQtyGreaterThan(0));
        dashboard.setWarehouseCount(chaiWarehouseRepository.countByStatus(ChaiStatus.ONLINE.getCode()));

        long inCount = chaiStockBillRepository.countByStatusAndBillTypeAndCreateTimeBetween(
                posted, ChaiStockBillType.IN.getCode(), startMs, endMs);
        long outCount = chaiStockBillRepository.countByStatusAndBillTypeAndCreateTimeBetween(
                posted, ChaiStockBillType.OUT.getCode(), startMs, endMs);
        long transferCount = chaiStockBillRepository.countByStatusAndBillTypeAndCreateTimeBetween(
                posted, ChaiStockBillType.TRANSFER.getCode(), startMs, endMs);
        dashboard.setBillInCount(inCount);
        dashboard.setBillOutCount(outCount);
        dashboard.setBillTransferCount(transferCount);
        dashboard.setBillTotalCount(inCount + outCount + transferCount);
        dashboard.setBillTrendChart(buildBillTrend(days, posted));
        return dashboard;
    }

    private ChaiDashboardStackedBarVo buildBillTrend(int days, Integer postedStatus) {
        LocalDate endDate = LocalDate.now(ZONE);
        LocalDate startDate = endDate.minusDays(days - 1L);
        List<LocalDate> dateList = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            dateList.add(date);
        }

        Map<LocalDate, long[]> counterMap = new HashMap<>();
        for (LocalDate date : dateList) {
            counterMap.put(date, new long[3]);
        }

        long queryStart = startOfDayMs(startDate);
        List<ChaiStockBill> bills = chaiStockBillRepository.findByStatusAndCreateTimeGreaterThanEqual(postedStatus, queryStart);
        for (ChaiStockBill bill : bills) {
            if (bill.getCreateTime() == null || bill.getBillType() == null) {
                continue;
            }
            LocalDate billDate = Instant.ofEpochMilli(bill.getCreateTime()).atZone(ZONE).toLocalDate();
            long[] counters = counterMap.get(billDate);
            if (counters == null) {
                continue;
            }
            if (ChaiStockBillType.IN.getCode().equals(bill.getBillType())) {
                counters[0]++;
            } else if (ChaiStockBillType.OUT.getCode().equals(bill.getBillType())) {
                counters[1]++;
            } else if (ChaiStockBillType.TRANSFER.getCode().equals(bill.getBillType())) {
                counters[2]++;
            }
        }

        ChaiDashboardStackedBarVo chart = new ChaiDashboardStackedBarVo();
        List<String> categories = new ArrayList<>();
        List<Long> inData = new ArrayList<>();
        List<Long> outData = new ArrayList<>();
        List<Long> transferData = new ArrayList<>();
        for (LocalDate date : dateList) {
            categories.add(date.format(DAY_LABEL));
            long[] counters = counterMap.get(date);
            inData.add(counters[0]);
            outData.add(counters[1]);
            transferData.add(counters[2]);
        }
        chart.setCategories(categories);

        ChaiDashboardStackedSeriesVo inSeries = new ChaiDashboardStackedSeriesVo();
        inSeries.setName(ChaiStockBillType.IN.getMessage());
        inSeries.setData(inData);

        ChaiDashboardStackedSeriesVo outSeries = new ChaiDashboardStackedSeriesVo();
        outSeries.setName(ChaiStockBillType.OUT.getMessage());
        outSeries.setData(outData);

        ChaiDashboardStackedSeriesVo transferSeries = new ChaiDashboardStackedSeriesVo();
        transferSeries.setName(ChaiStockBillType.TRANSFER.getMessage());
        transferSeries.setData(transferData);

        chart.getSeries().add(inSeries);
        chart.getSeries().add(outSeries);
        chart.getSeries().add(transferSeries);
        return chart;
    }

    private int resolveStatDays(Integer statDays) {
        if (statDays != null && ALLOWED_STAT_DAYS.contains(statDays)) {
            return statDays;
        }
        return DEFAULT_STAT_DAYS;
    }

    private long startMsForDays(int days) {
        LocalDate startDate = LocalDate.now(ZONE).minusDays(days - 1L);
        return startOfDayMs(startDate);
    }

    private long startOfDayMs(LocalDate date) {
        return date.atStartOfDay(ZONE).toInstant().toEpochMilli();
    }
}
