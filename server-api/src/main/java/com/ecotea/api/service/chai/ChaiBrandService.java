package com.ecotea.api.service.chai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecotea.api.domain.chai.ChaiBrand;
import com.ecotea.api.mapper.chai.ChaiBrandMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChaiBrandService {

    private final ChaiBrandMapper chaiBrandMapper;

    /**
     * 上架品牌，按排序号大到小（与 EcoTea listOnlineOrdered 一致）
     */
    public List<ChaiBrand> listOnlineOrdered() {
        return chaiBrandMapper.selectList(new LambdaQueryWrapper<ChaiBrand>()
                .eq(ChaiBrand::getStatus, 1)
                .orderByDesc(ChaiBrand::getOrderNum)
                .orderByDesc(ChaiBrand::getId));
    }

    public List<ChaiBrand> listAll() {
        return chaiBrandMapper.selectList(new LambdaQueryWrapper<ChaiBrand>()
                .orderByDesc(ChaiBrand::getOrderNum)
                .orderByDesc(ChaiBrand::getUpdateTime));
    }
}
