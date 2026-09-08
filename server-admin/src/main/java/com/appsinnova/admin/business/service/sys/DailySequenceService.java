package com.appsinnova.admin.business.service.sys;

import com.appsinnova.admin.business.common.enums.base.DailySeqType;
import com.appsinnova.admin.business.common.utils.TimeUtils;
import com.appsinnova.admin.business.domain.sys.DailySequence;
import com.appsinnova.admin.business.repository.sys.DailySequenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DailySequenceService {

    private final DailySequenceRepository dailySequenceRepository;

    /**
     * 获取当天当前序号，并把序号 +1 后落库（业务日按当前时刻中国时区 yyyyMMdd）。
     */
    @Transactional(rollbackFor = Exception.class)
    public synchronized Integer getCurrentAndIncrement(DailySeqType dailySeqType) {
        return getCurrentAndIncrement(dailySeqType, System.currentTimeMillis());
    }

    /**
     * 按指定时间戳对应的中国业务日取号，保证与单号日期一致。
     */
    @Transactional(rollbackFor = Exception.class)
    public synchronized Integer getCurrentAndIncrement(DailySeqType dailySeqType, long timestamp) {
        String bizDate = TimeUtils.getDateYYMMdd(timestamp);
        long now = System.currentTimeMillis();
        DailySequence model = dailySequenceRepository.findFirstByBizTypeAndBizDate(dailySeqType.getCode(), bizDate);
        if (model == null) {
            model = new DailySequence();
            model.setBizType(dailySeqType.getCode());
            model.setBizDate(bizDate);
            model.setCurrentSeq(2);
            model.setCreateTime(now);
            model.setUpdateTime(now);
            dailySequenceRepository.save(model);
            return 1;
        }

        int current = model.getCurrentSeq() == null ? 1 : model.getCurrentSeq();
        model.setCurrentSeq(current + 1);
        model.setUpdateTime(now);
        dailySequenceRepository.save(model);
        return current;
    }
}
