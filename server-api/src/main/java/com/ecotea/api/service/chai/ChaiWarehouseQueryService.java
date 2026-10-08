package com.ecotea.api.service.chai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecotea.api.common.constant.RedisConstant;
import com.ecotea.api.common.enums.chai.ChaiStatus;
import com.ecotea.api.common.utils.JsonUtils;
import com.ecotea.api.common.utils.RedisUtils;
import com.ecotea.api.domain.chai.ChaiWarehouse;
import com.ecotea.api.mapper.chai.ChaiWarehouseMapper;
import com.ecotea.api.vo.chai.ChaiWarehouseVO;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 仓库只读查询。
 */
@Service
@RequiredArgsConstructor
public class ChaiWarehouseQueryService {

    private static final TypeReference<List<ChaiWarehouse>> LIST_TYPE = new TypeReference<List<ChaiWarehouse>>() {
    };

    private final ChaiWarehouseMapper chaiWarehouseMapper;

    /**
     * 上架仓库 VO（筛选下拉），走 {@link #listOnlineOrdered()} 缓存。
     */
    public List<ChaiWarehouseVO> listOnline() {
        List<ChaiWarehouse> rows = listOnlineOrdered();
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<ChaiWarehouseVO> list = new ArrayList<>(rows.size());
        for (ChaiWarehouse wh : rows) {
            list.add(toVo(wh));
        }
        return list;
    }

    /**
     * 上架仓库：排序号大到小，其次 id 降序（与 admin listOnlineOrdered 一致）。
     * Redis → miss → MySQL → SET（DAY_EXPIRE）。
     */
    public List<ChaiWarehouse> listOnlineOrdered() {
        String key = RedisConstant.CHAI_WAREHOUSE_ONLINE_KEY;
        String cached = RedisUtils.get(RedisUtils.defaultRedis(), key);
        if (cached != null) {
            List<ChaiWarehouse> fromCache = JsonUtils.readValueForCache(cached, LIST_TYPE);
            return fromCache != null ? fromCache : Collections.emptyList();
        }

        List<ChaiWarehouse> list = chaiWarehouseMapper.selectList(
                new LambdaQueryWrapper<ChaiWarehouse>()
                        .eq(ChaiWarehouse::getStatus, ChaiStatus.ONLINE.getCode())
                        .orderByDesc(ChaiWarehouse::getOrderNum)
                        .orderByDesc(ChaiWarehouse::getId));
        if (list == null) {
            list = Collections.emptyList();
        }
        String json = JsonUtils.writeValueAsString(list);
        if (json != null) {
            RedisUtils.set(RedisUtils.defaultRedis(), key, json, RedisConstant.DAY_EXPIRE);
        }
        return list;
    }

    /**
     * 按 id 取仓库：先上架列表缓存，未命中再查库（含已下架）。
     */
    public Map<Long, ChaiWarehouse> mapByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashMap<>();
        }
        Set<Long> need = new HashSet<>();
        for (Long id : ids) {
            if (id != null) {
                need.add(id);
            }
        }
        if (need.isEmpty()) {
            return new HashMap<>();
        }
        Map<Long, ChaiWarehouse> map = new HashMap<>();
        for (ChaiWarehouse wh : listOnlineOrdered()) {
            if (wh.getId() != null && need.contains(wh.getId())) {
                map.put(wh.getId(), wh);
            }
        }
        if (map.size() == need.size()) {
            return map;
        }
        List<Long> missing = new ArrayList<>();
        for (Long id : need) {
            if (!map.containsKey(id)) {
                missing.add(id);
            }
        }
        for (ChaiWarehouse wh : chaiWarehouseMapper.selectBatchIds(missing)) {
            map.put(wh.getId(), wh);
        }
        return map;
    }

    private static ChaiWarehouseVO toVo(ChaiWarehouse wh) {
        ChaiWarehouseVO vo = new ChaiWarehouseVO();
        vo.setId(wh.getId());
        vo.setName(wh.getName());
        vo.setShortName(wh.getShortName());
        return vo;
    }
}
