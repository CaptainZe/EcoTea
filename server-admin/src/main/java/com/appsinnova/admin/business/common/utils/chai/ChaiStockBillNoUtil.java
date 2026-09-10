package com.appsinnova.admin.business.common.utils.chai;

import com.appsinnova.admin.business.common.constant.SeqBizConstant;
import com.appsinnova.admin.business.common.enums.chai.ChaiStockBillType;
import com.appsinnova.admin.business.common.utils.RedisSeqUtils;
import com.appsinnova.admin.business.common.utils.TimeUtils;
import org.springframework.util.StringUtils;

/**
 * 库存单据号：CHAI-SBRK/SBCK/SBDB-{yyyyMMdd}-{4位日序}
 */
public final class ChaiStockBillNoUtil {

    private ChaiStockBillNoUtil() {
    }

    public static String next(ChaiStockBillType billType, RedisSeqUtils.MaxSeqLoader maxLoader) {
        if (billType == null) {
            throw new IllegalArgumentException("单据类型不能为空");
        }
        long now = System.currentTimeMillis();
        String date = TimeUtils.getDateYYMMdd(now);
        long seq = RedisSeqUtils.nextDaily(seqBiz(billType), now, maxLoader);
        return format(billType, date, seq);
    }

    public static String seqBiz(ChaiStockBillType billType) {
        if (billType == null) {
            throw new IllegalArgumentException("单据类型不能为空");
        }
        switch (billType) {
            case IN:
                return SeqBizConstant.CHAI_STOCK_IN;
            case OUT:
                return SeqBizConstant.CHAI_STOCK_OUT;
            case TRANSFER:
                return SeqBizConstant.CHAI_STOCK_TRANSFER;
            default:
                throw new IllegalArgumentException("不支持的单据类型");
        }
    }

    /** 当日单号前缀，如 {@code CHAI-SBRK-20260910-} */
    public static String noPrefix(ChaiStockBillType billType, String bizDateYyyyMmDd) {
        return billType.getBillNoPrefix() + "-" + bizDateYyyyMmDd + "-";
    }

    public static String format(ChaiStockBillType billType, String date, long seq) {
        return String.format("%s-%s-%04d", billType.getBillNoPrefix(), date, seq);
    }

    public static long parseSeq(String billNo) {
        if (!StringUtils.hasText(billNo)) {
            return 0L;
        }
        int i = billNo.lastIndexOf('-');
        if (i < 0 || i == billNo.length() - 1) {
            return 0L;
        }
        try {
            long seq = Long.parseLong(billNo.substring(i + 1).trim());
            return Math.max(seq, 0L);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
