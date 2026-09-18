package com.ecotea.api.service.chai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecotea.api.common.enums.chai.ChaiStatus;
import com.ecotea.api.domain.chai.ChaiWarehouse;
import com.ecotea.api.mapper.chai.ChaiWarehouseMapper;
import com.ecotea.api.vo.chai.ChaiWarehouseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 仓库只读查询。
 */
@Service
@RequiredArgsConstructor
public class ChaiWarehouseQueryService {

    private final ChaiWarehouseMapper chaiWarehouseMapper;

    /**
     * 上架仓库，按排序号大到小。
     */
    public List<ChaiWarehouseVO> listOnline() {
        List<ChaiWarehouse> rows = chaiWarehouseMapper.selectList(
                new LambdaQueryWrapper<ChaiWarehouse>()
                        .eq(ChaiWarehouse::getStatus, ChaiStatus.ONLINE.getCode())
                        .orderByDesc(ChaiWarehouse::getOrderNum)
                        .orderByDesc(ChaiWarehouse::getId));
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<ChaiWarehouseVO> list = new ArrayList<>(rows.size());
        for (ChaiWarehouse wh : rows) {
            list.add(toVo(wh));
        }
        return list;
    }

    private static ChaiWarehouseVO toVo(ChaiWarehouse wh) {
        ChaiWarehouseVO vo = new ChaiWarehouseVO();
        vo.setId(wh.getId());
        vo.setName(wh.getName());
        vo.setShortName(wh.getShortName());
        return vo;
    }
}
