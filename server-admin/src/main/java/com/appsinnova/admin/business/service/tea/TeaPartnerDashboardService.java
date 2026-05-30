package com.appsinnova.admin.business.service.tea;

import com.appsinnova.admin.business.common.enums.tea.TeaPartnerStatus;
import com.appsinnova.admin.business.common.pca.PcaCodeService;
import com.appsinnova.admin.business.vo.tea.TeaPartnerDashboardVo;
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

    private static final int LIAISON_RANK_LIMIT = 30;

    private final EntityManager entityManager;
    private final PcaCodeService pcaCodeService;

    public TeaPartnerDashboardVo buildFullDashboard() {
        TeaPartnerDashboardVo vo = new TeaPartnerDashboardVo();
        fillCoreMetrics(vo);
        vo.setProvinceStatList(loadProvinceStats());
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

    public List<TeaPartnerDashboardVo.RegionStatRow> loadProvinceStats() {
        String sql = "SELECT p.province, COUNT(*) AS cnt "
                + "FROM tea_partner p "
                + "WHERE p.province IS NOT NULL AND p.province <> '' "
                + "GROUP BY p.province ORDER BY cnt DESC, p.province";
        Query q = entityManager.createNativeQuery(sql);
        return mapRegionRows(q.getResultList());
    }

    public List<TeaPartnerDashboardVo.RegionStatRow> loadCityStatsByProvince(String provinceCode) {
        if (!StringUtils.hasText(provinceCode)) {
            return new ArrayList<>();
        }
        String sql = "SELECT p.city, COUNT(*) AS cnt "
                + "FROM tea_partner p "
                + "WHERE p.province = :provinceCode "
                + "AND p.city IS NOT NULL AND p.city <> '' "
                + "GROUP BY p.city ORDER BY cnt DESC, p.city";
        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("provinceCode", provinceCode.trim());
        return mapRegionRows(q.getResultList());
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

    private List<TeaPartnerDashboardVo.RegionStatRow> mapRegionRows(List<?> rows) {
        List<TeaPartnerDashboardVo.RegionStatRow> list = new ArrayList<>();
        if (CollectionUtils.isEmpty(rows)) {
            return list;
        }
        for (Object row : rows) {
            if (!(row instanceof Object[])) {
                continue;
            }
            Object[] r = (Object[]) row;
            if (r.length < 2 || r[0] == null) {
                continue;
            }
            String code = String.valueOf(r[0]).trim();
            if (!StringUtils.hasText(code)) {
                continue;
            }
            list.add(new TeaPartnerDashboardVo.RegionStatRow(
                    code, pcaCodeService.resolveName(code), toLong(r[1])));
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
