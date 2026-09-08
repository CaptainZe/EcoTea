package com.appsinnova.admin.business.common.utils.chai;

import com.appsinnova.admin.business.common.enums.base.DailySeqType;
import com.appsinnova.admin.business.common.enums.chai.ChaiStockBillType;
import com.appsinnova.admin.business.common.utils.TimeUtils;
import com.appsinnova.admin.business.service.sys.DailySequenceService;

/**
 * 库存单据号：CHAI-SBRK/SBCK/SBDB-{yyyyMMdd}-{4位日序}
 */
public final class ChaiStockBillNoUtil {

    private ChaiStockBillNoUtil() {
    }

    public static String next(ChaiStockBillType billType, DailySequenceService dailySequenceService) {
        if (billType == null || dailySequenceService == null) {
            throw new IllegalArgumentException("单据类型不能为空");
        }
        DailySeqType seqType;
        switch (billType) {
            case IN:
                seqType = DailySeqType.CHAI_STOCK_IN;
                break;
            case OUT:
                seqType = DailySeqType.CHAI_STOCK_OUT;
                break;
            case TRANSFER:
                seqType = DailySeqType.CHAI_STOCK_TRANSFER;
                break;
            default:
                throw new IllegalArgumentException("不支持的单据类型");
        }
        long now = System.currentTimeMillis();
        String date = TimeUtils.getDateYYMMdd(now);
        Integer seq = dailySequenceService.getCurrentAndIncrement(seqType, now);
        return String.format("%s-%s-%04d", billType.getBillNoPrefix(), date, seq);
    }
}
