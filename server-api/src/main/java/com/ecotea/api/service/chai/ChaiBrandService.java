package com.ecotea.api.service.chai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecotea.api.domain.chai.ChaiBrand;
import com.ecotea.api.mapper.chai.ChaiBrandMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ChaiBrandService {

    private final ChaiBrandMapper chaiBrandMapper;

    /**
     * 上架品牌，按名称拼音排序（与 EcoTea listOnlineOrdered 一致）
     */
    public List<ChaiBrand> listOnlineOrdered() {
        List<ChaiBrand> list = chaiBrandMapper.selectList(new LambdaQueryWrapper<ChaiBrand>()
                .eq(ChaiBrand::getStatus, 1));
        Collator collator = Collator.getInstance(Locale.CHINA);
        list.sort(Comparator.comparing(ChaiBrand::getName, Comparator.nullsLast(collator)));
        return list;
    }

    public List<ChaiBrand> listAll() {
        return chaiBrandMapper.selectList(new LambdaQueryWrapper<ChaiBrand>()
                .orderByDesc(ChaiBrand::getOrderNum)
                .orderByDesc(ChaiBrand::getUpdateTime));
    }
}
