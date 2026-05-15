package com.appsinnova.admin.business.service.tea;

import com.appsinnova.admin.business.common.enums.tea.TeaPartnerStatus;
import com.appsinnova.admin.business.vo.tea.TeaPartnerDashboardVo;
import com.appsinnova.admin.common.utils.DictUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;

/**
 * 合作方仪表盘统计
 */
@Service
@RequiredArgsConstructor
public class TeaPartnerDashboardService {

    private static final int TREND_MONTH_RANGE_MONTHS = 13;
    private static final int LIAISON_RANK_LIMIT = 30;

    private final EntityManager entityManager;

    public TeaPartnerDashboardVo buildFullDashboard() {
        TeaPartnerDashboardVo vo = new TeaPartnerDashboardVo();
        fillCoreMetrics(vo);
        vo.setGrowthTrendByMonth(loadGrowthTrendByMonth());
        vo.setStatusDistribution(loadStatusDistribution());
        vo.setLiaisonPartnerRankList(loadLiaisonPartnerRank(LIAISON_RANK_LIMIT));
        return vo;
    }

    public void fillCoreMetrics(TeaPartnerDashboardVo target) {
        int signed = TeaPartnerStatus.SIGNED.getCode();
        int terminated = TeaPartnerStatus.TERMINATED.getCode();
        String sql = "SELECT COUNT(*), "
                + "COALESCE(SUM(CASE WHEN p.status = :signed THEN 1 ELSE 0 END), 0), "
                + "COALESCE(SUM(CASE WHEN p.status = :terminated THEN 1 ELSE 0 END), 0) "
                + "FROM tea_partner p";
        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("signed", signed);
        q.setParameter("terminated", terminated);
        Object[] row = (Object[]) q.getSingleResult();
        if (row != null && row.length >= 3) {
            target.setTotalCount(toLong(row[0]));
            target.setSignedCount(toLong(row[1]));
            target.setTerminatedCount(toLong(row[2]));
        }
    }

    public List<TeaPartnerDashboardVo.MonthTrendPoint> loadGrowthTrendByMonth() {
        long now = System.currentTimeMillis();
        long fromMs = now - (long) TREND_MONTH_RANGE_MONTHS * 31L * 24 * 3600 * 1000;
        String sql = "SELECT DATE_FORMAT(FROM_UNIXTIME(p.create_time/1000), '%Y-%m') AS bucket, COUNT(*) AS cnt "
                + "FROM tea_partner p WHERE p.create_time >= :fromMs "
                + "GROUP BY bucket ORDER BY bucket";
        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("fromMs", fromMs);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        List<TeaPartnerDashboardVo.MonthTrendPoint> list = new ArrayList<>();
        if (CollectionUtils.isEmpty(rows)) {
            return list;
        }
        for (Object[] r : rows) {
            if (r == null || r.length < 2 || r[0] == null) {
                continue;
            }
            list.add(new TeaPartnerDashboardVo.MonthTrendPoint(String.valueOf(r[0]), toLong(r[1])));
        }
        return list;
    }

    public List<TeaPartnerDashboardVo.StatusSlice> loadStatusDistribution() {
        String sql = "SELECT p.status, COUNT(*) FROM tea_partner p GROUP BY p.status ORDER BY p.status";
        Query q = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        List<TeaPartnerDashboardVo.StatusSlice> list = new ArrayList<>();
        for (Object[] r : rows) {
            if (r == null || r.length < 2) {
                continue;
            }
            Integer st = r[0] == null ? null : ((Number) r[0]).intValue();
            String label = "-";
            if (st != null) {
                String dv = DictUtils.keyValue("TEA_PARTNER_STATUS", String.valueOf(st));
                label = StringUtils.hasText(dv) ? dv : String.valueOf(st);
            }
            list.add(new TeaPartnerDashboardVo.StatusSlice(st, toLong(r[1]), label));
        }
        return list;
    }

    public List<TeaPartnerDashboardVo.LiaisonRankRow> loadLiaisonPartnerRank(int limit) {
        String sql = "SELECT p.liaison_user_id, MAX(u.nickname), MAX(u.username), COUNT(*) AS cnt "
                + "FROM tea_partner p "
                + "LEFT JOIN sys_user u ON u.id = p.liaison_user_id "
                + "WHERE p.liaison_user_id IS NOT NULL AND p.liaison_user_id > 0 "
                + "GROUP BY p.liaison_user_id "
                + "ORDER BY cnt DESC";
        Query q = entityManager.createNativeQuery(sql);
        q.setMaxResults(limit);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        List<TeaPartnerDashboardVo.LiaisonRankRow> list = new ArrayList<>();
        for (Object[] r : rows) {
            if (r == null || r.length < 4 || r[0] == null) {
                continue;
            }
            long uid = toLong(r[0]);
            String nick = r[1] == null ? "" : String.valueOf(r[1]).trim();
            String username = r[2] == null ? "" : String.valueOf(r[2]).trim();
            String display;
            if (StringUtils.hasText(nick) || StringUtils.hasText(username)) {
                display = (StringUtils.hasText(nick) ? nick : username)
                        + "（" + (StringUtils.hasText(username) ? username : String.valueOf(uid)) + "）";
            } else {
                display = "用户ID " + uid;
            }
            list.add(new TeaPartnerDashboardVo.LiaisonRankRow(uid, display, toLong(r[3])));
        }
        return list;
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
}
