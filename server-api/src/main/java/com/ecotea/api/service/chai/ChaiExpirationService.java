package com.ecotea.api.service.chai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecotea.api.common.constant.RedisConstant;
import com.ecotea.api.common.utils.JsonUtils;
import com.ecotea.api.common.utils.RedisUtils;
import com.ecotea.api.domain.chai.ChaiExpiration;
import com.ecotea.api.mapper.chai.ChaiExpirationMapper;
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

@Service
@RequiredArgsConstructor
public class ChaiExpirationService {

    private static final TypeReference<List<ChaiExpiration>> LIST_TYPE = new TypeReference<List<ChaiExpiration>>() {
    };

    private final ChaiExpirationMapper chaiExpirationMapper;

    /**
     * 上架保质期：排序号大到小，其次 id 降序（与 admin listOnlineOrdered 一致）。
     * Redis → miss → MySQL → SET（DAY_EXPIRE）。
     */
    public List<ChaiExpiration> listOnlineOrdered() {
        String key = RedisConstant.CHAI_EXPIRATION_ONLINE_KEY;
        String cached = RedisUtils.get(RedisUtils.defaultRedis(), key);
        if (cached != null) {
            List<ChaiExpiration> fromCache = JsonUtils.readValueForCache(cached, LIST_TYPE);
            return fromCache != null ? fromCache : Collections.emptyList();
        }

        List<ChaiExpiration> list = chaiExpirationMapper.selectList(new LambdaQueryWrapper<ChaiExpiration>()
                .eq(ChaiExpiration::getStatus, 1)
                .orderByDesc(ChaiExpiration::getOrderNum)
                .orderByDesc(ChaiExpiration::getId));
        String json = JsonUtils.writeValueAsString(list);
        if (json != null) {
            RedisUtils.set(RedisUtils.defaultRedis(), key, json, RedisConstant.DAY_EXPIRE);
        }
        return list;
    }

    /**
     * 按 id 补保质期名：先上架列表缓存，未命中再查库（含已下架）。
     */
    public Map<Long, String> mapNamesByIds(Collection<Long> ids) {
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
        Map<Long, String> map = new HashMap<>();
        for (ChaiExpiration item : listOnlineOrdered()) {
            if (item.getId() != null && need.contains(item.getId())) {
                map.put(item.getId(), item.getName());
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
        for (ChaiExpiration item : chaiExpirationMapper.selectBatchIds(missing)) {
            map.put(item.getId(), item.getName());
        }
        return map;
    }
}
