package com.appsinnova.admin.business.common.utils.chai;

import com.appsinnova.admin.business.common.enums.chai.ChaiProdBatch;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 茶叶半年批次推算（CHAI_PROD_BATCH：上半年 / 下半年）
 */
public final class ChaiHalfYearUtil {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private ChaiHalfYearUtil() {
    }

    /**
     * 以「今天」所在半年为锚点，向前推算 count 个半年（含当期）。
     * 1–6 月=上半年(100)，7–12 月=下半年(200)。
     */
    public static List<HalfYear> recentFromNow(int count) {
        LocalDate today = LocalDate.now(ZONE);
        int year = today.getYear();
        int prodBatch = today.getMonthValue() <= 6
                ? ChaiProdBatch.FIRST_HALF.getCode()
                : ChaiProdBatch.SECOND_HALF.getCode();
        return recentFromAnchor(year, prodBatch, count);
    }

    /**
     * 以锚点为第 1 期，向前推算 count 个半年（含锚点）
     */
    public static List<HalfYear> recentFromAnchor(Integer year, Integer prodBatch, int count) {
        List<HalfYear> list = new ArrayList<>();
        if (year == null || prodBatch == null || count <= 0) {
            return list;
        }
        int y = year;
        ChaiProdBatch batch = ChaiProdBatch.fromCode(prodBatch);
        for (int i = 0; i < count; i++) {
            list.add(new HalfYear(y, batch.getCode()));
            if (batch == ChaiProdBatch.FIRST_HALF) {
                y = y - 1;
                batch = ChaiProdBatch.SECOND_HALF;
            } else {
                batch = ChaiProdBatch.FIRST_HALF;
            }
        }
        return list;
    }

    public static int normalizeBatch(int prodBatch) {
        return ChaiProdBatch.fromCode(prodBatch).getCode();
    }

    public static final class HalfYear {
        private final int year;
        private final int prodBatch;

        public HalfYear(int year, int prodBatch) {
            this.year = year;
            this.prodBatch = prodBatch;
        }

        public int getYear() {
            return year;
        }

        public int getProdBatch() {
            return prodBatch;
        }
    }
}
