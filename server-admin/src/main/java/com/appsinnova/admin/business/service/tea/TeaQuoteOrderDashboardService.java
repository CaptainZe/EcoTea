package com.appsinnova.admin.business.service.tea;

import com.appsinnova.admin.business.common.enums.tea.TeaQuoteTrendGranularity;
import com.appsinnova.admin.business.vo.tea.TeaQuoteDashboardVo;
import com.appsinnova.admin.common.utils.DictUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 报价单仪表盘统计（SQL 聚合，供 TeaOverviewController 使用）
 */
@Service
@RequiredArgsConstructor
public class TeaQuoteOrderDashboardService {

    private static final int TREND_DAY_RANGE_DAYS = 31;
    private static final int TREND_WEEK_RANGE_WEEKS = 13;
    private static final int TREND_MONTH_RANGE_MONTHS = 13;
    private static final int SKU_TOP_LIMIT = 20;
    private static final int BRAND_BAR_TOP_LIMIT = 20;
    private static final int USER_RANK_LIMIT = 20;

    private final EntityManager entityManager;

    /**
     * 组装完整仪表盘数据（含默认「按天」趋势）
     */
    public TeaQuoteDashboardVo buildFullDashboard() {
        TeaQuoteDashboardVo vo = new TeaQuoteDashboardVo();
        fillCoreMetrics(vo);
        vo.setStatusDistribution(loadStatusDistribution());
        vo.setTrendSeries(loadTrendSeries(TeaQuoteTrendGranularity.DAY));
        vo.setBrandQuoteQtyTopList(loadBrandQuoteQtyTop(BRAND_BAR_TOP_LIMIT));
        vo.setBrandQuoteManualAmtTopList(loadBrandQuoteManualAmtTop(BRAND_BAR_TOP_LIMIT));
        vo.setSkuTopList(loadSkuTop(SKU_TOP_LIMIT));
        vo.setUserRankList(loadUserRank(USER_RANK_LIMIT));
        return vo;
    }

    public List<TeaQuoteDashboardVo.TrendPoint> loadTrendSeries(TeaQuoteTrendGranularity granularity) {
        long now = System.currentTimeMillis();
        long fromMs;
        String sql;
        switch (granularity) {
            case WEEK:
                fromMs = now - (long) TREND_WEEK_RANGE_WEEKS * 7L * 24 * 3600 * 1000;
                sql = "SELECT DATE_FORMAT(FROM_UNIXTIME(o.create_time/1000), '%x-%v') AS bucket, "
                        + "COUNT(*) AS cnt, "
                        + "COALESCE(SUM(o.total_amount),0) AS amt, "
                        + "COALESCE(SUM(o.total_manual_amount),0) AS man, "
                        + "COALESCE(SUM(o.total_quantity),0) AS qty "
                        + "FROM tea_quote_order o WHERE o.create_time >= :fromMs "
                        + "GROUP BY bucket ORDER BY bucket";
                break;
            case MONTH:
                fromMs = now - (long) TREND_MONTH_RANGE_MONTHS * 31L * 24 * 3600 * 1000;
                sql = "SELECT DATE_FORMAT(FROM_UNIXTIME(o.create_time/1000), '%Y-%m') AS bucket, "
                        + "COUNT(*) AS cnt, "
                        + "COALESCE(SUM(o.total_amount),0) AS amt, "
                        + "COALESCE(SUM(o.total_manual_amount),0) AS man, "
                        + "COALESCE(SUM(o.total_quantity),0) AS qty "
                        + "FROM tea_quote_order o WHERE o.create_time >= :fromMs "
                        + "GROUP BY bucket ORDER BY bucket";
                break;
            case DAY:
            default:
                fromMs = now - (long) TREND_DAY_RANGE_DAYS * 24 * 3600 * 1000;
                sql = "SELECT DATE(FROM_UNIXTIME(o.create_time/1000)) AS bucket, "
                        + "COUNT(*) AS cnt, "
                        + "COALESCE(SUM(o.total_amount),0) AS amt, "
                        + "COALESCE(SUM(o.total_manual_amount),0) AS man, "
                        + "COALESCE(SUM(o.total_quantity),0) AS qty "
                        + "FROM tea_quote_order o WHERE o.create_time >= :fromMs "
                        + "GROUP BY bucket ORDER BY bucket";
                break;
        }
        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("fromMs", fromMs);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        List<TeaQuoteDashboardVo.TrendPoint> list = new ArrayList<>();
        for (Object[] r : rows) {
            if (r == null || r.length < 5 || r[0] == null) {
                continue;
            }
            list.add(new TeaQuoteDashboardVo.TrendPoint(
                    String.valueOf(r[0]),
                    toLong(r[1]),
                    toDecimal(r[2]).setScale(2, RoundingMode.HALF_UP),
                    toDecimal(r[3]).setScale(2, RoundingMode.HALF_UP),
                    toLong(r[4])
            ));
        }
        return list;
    }

    public void fillCoreMetrics(TeaQuoteDashboardVo target) {
        String sql = "SELECT COUNT(*), "
                + "COALESCE(SUM(o.total_amount),0), "
                + "COALESCE(SUM(o.total_manual_amount),0), "
                + "COALESCE(SUM(o.total_quantity),0) "
                + "FROM tea_quote_order o";
        Query q = entityManager.createNativeQuery(sql);
        Object[] row = (Object[]) q.getSingleResult();
        if (row != null && row.length >= 4) {
            target.setOrderCount(toLong(row[0]));
            target.setTotalAmountSum(toDecimal(row[1]).setScale(2, RoundingMode.HALF_UP));
            target.setTotalManualAmountSum(toDecimal(row[2]).setScale(2, RoundingMode.HALF_UP));
            target.setTotalQuantitySum(toLong(row[3]));
        }
    }

    public List<TeaQuoteDashboardVo.StatusSlice> loadStatusDistribution() {
        String sql = "SELECT o.status, COUNT(*) FROM tea_quote_order o GROUP BY o.status ORDER BY o.status";
        Query q = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        List<TeaQuoteDashboardVo.StatusSlice> list = new ArrayList<>();
        for (Object[] r : rows) {
            if (r == null || r.length < 2) {
                continue;
            }
            Integer st = r[0] == null ? null : ((Number) r[0]).intValue();
            String label = "-";
            if (st != null) {
                String dv = DictUtils.keyValue("TEA_QUOTE_ORDER_STATUS", String.valueOf(st));
                label = StringUtils.hasText(dv) ? dv : String.valueOf(st);
            }
            list.add(new TeaQuoteDashboardVo.StatusSlice(st, toLong(r[1]), label));
        }
        return list;
    }

    public List<TeaQuoteDashboardVo.SkuTopRow> loadSkuTop(int limit) {
        String sql = "SELECT i.sku_id, MAX(i.sku_code), MAX(i.sku_brand), MAX(i.sku_name), MAX(i.sku_spec), MAX(i.sku_production_batch), "
                + "SUM(i.quantity), COALESCE(SUM(i.amount),0), COUNT(DISTINCT i.order_id) "
                + "FROM tea_quote_order_item i "
                + "GROUP BY i.sku_id "
                + "ORDER BY SUM(i.quantity) DESC";
        Query q = entityManager.createNativeQuery(sql);
        q.setMaxResults(limit);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        List<TeaQuoteDashboardVo.SkuTopRow> list = new ArrayList<>();
        for (Object[] r : rows) {
            if (r == null || r.length < 9 || r[0] == null) {
                continue;
            }
            String skuCode = str(r[1]);
            String skuBrand = str(r[2]);
            String skuName = str(r[3]);
            String skuSpec = str(r[4]);
            String skuBatch = str(r[5]);
            String summary = buildSkuProductSummary(skuCode, skuBrand, skuName, skuSpec, skuBatch);
            list.add(new TeaQuoteDashboardVo.SkuTopRow(
                    toLong(r[0]),
                    skuCode,
                    skuBrand,
                    skuName,
                    skuSpec,
                    skuBatch,
                    toLong(r[6]),
                    toDecimal(r[7]).setScale(2, RoundingMode.HALF_UP),
                    toLong(r[8]),
                    summary
            ));
        }
        return list;
    }

    public List<TeaQuoteDashboardVo.BrandQuoteQtyRow> loadBrandQuoteQtyTop(int limit) {
        String sql = "SELECT TRIM(i.sku_brand) AS b, SUM(i.quantity) AS q "
                + "FROM tea_quote_order_item i "
                + "WHERE i.sku_brand IS NOT NULL AND LENGTH(TRIM(i.sku_brand)) > 0 "
                + "GROUP BY TRIM(i.sku_brand) "
                + "ORDER BY q DESC";
        Query q = entityManager.createNativeQuery(sql);
        q.setMaxResults(limit);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        List<TeaQuoteDashboardVo.BrandQuoteQtyRow> list = new ArrayList<>();
        for (Object[] r : rows) {
            if (r == null || r.length < 2 || r[0] == null) {
                continue;
            }
            String key = String.valueOf(r[0]).trim();
            list.add(new TeaQuoteDashboardVo.BrandQuoteQtyRow(
                    resolveTeaBrandLabel(key),
                    toLong(r[1])
            ));
        }
        return list;
    }

    public List<TeaQuoteDashboardVo.BrandQuoteManualAmtRow> loadBrandQuoteManualAmtTop(int limit) {
        String sql = "SELECT TRIM(i.sku_brand) AS b, COALESCE(SUM(COALESCE(i.manual_amount, i.amount)),0) AS amt "
                + "FROM tea_quote_order_item i "
                + "WHERE i.sku_brand IS NOT NULL AND LENGTH(TRIM(i.sku_brand)) > 0 "
                + "GROUP BY TRIM(i.sku_brand) "
                + "ORDER BY amt DESC";
        Query q = entityManager.createNativeQuery(sql);
        q.setMaxResults(limit);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        List<TeaQuoteDashboardVo.BrandQuoteManualAmtRow> list = new ArrayList<>();
        for (Object[] r : rows) {
            if (r == null || r.length < 2 || r[0] == null) {
                continue;
            }
            String key = String.valueOf(r[0]).trim();
            list.add(new TeaQuoteDashboardVo.BrandQuoteManualAmtRow(
                    resolveTeaBrandLabel(key),
                    toDecimal(r[1]).setScale(2, RoundingMode.HALF_UP)
            ));
        }
        return list;
    }

    public List<TeaQuoteDashboardVo.UserRankRow> loadUserRank(int limit) {
        String sql = "SELECT o.user_id, MAX(o.user_name), COUNT(*), COALESCE(SUM(o.total_amount),0) "
                + "FROM tea_quote_order o "
                + "WHERE o.user_id IS NOT NULL "
                + "GROUP BY o.user_id "
                + "ORDER BY COUNT(*) DESC";
        Query q = entityManager.createNativeQuery(sql);
        q.setMaxResults(limit);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        List<TeaQuoteDashboardVo.UserRankRow> list = new ArrayList<>();
        for (Object[] r : rows) {
            if (r == null || r.length < 4 || r[0] == null) {
                continue;
            }
            list.add(new TeaQuoteDashboardVo.UserRankRow(
                    toLong(r[0]),
                    r[1] == null ? "" : String.valueOf(r[1]),
                    toLong(r[2]),
                    toDecimal(r[3]).setScale(2, RoundingMode.HALF_UP)
            ));
        }
        return list;
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private static String resolveTeaBrandLabel(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "-";
        }
        String t = raw.trim();
        String dv = DictUtils.keyValue("TEA_BRAND", t);
        return StringUtils.hasText(dv) ? dv : t;
    }

    private static String nvlDisplay(String s) {
        return StringUtils.hasText(s) ? s.trim() : "-";
    }

    private static String buildSkuProductSummary(String skuCode, String skuBrandRaw, String skuName, String skuSpec, String skuBatch) {
        return "编码：" + nvlDisplay(skuCode)
                + " | 品牌：" + resolveTeaBrandLabel(skuBrandRaw)
                + " | 名称：" + nvlDisplay(skuName)
                + " | 规格：" + nvlDisplay(skuSpec)
                + " | 生产批次：" + nvlDisplay(skuBatch);
    }

    private static long toLong(Object o) {
        if (o == null) {
            return 0L;
        }
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        return Long.parseLong(o.toString());
    }

    private static BigDecimal toDecimal(Object o) {
        if (o == null) {
            return BigDecimal.ZERO;
        }
        if (o instanceof BigDecimal) {
            return (BigDecimal) o;
        }
        if (o instanceof Number) {
            return BigDecimal.valueOf(((Number) o).doubleValue());
        }
        return new BigDecimal(o.toString());
    }
}
