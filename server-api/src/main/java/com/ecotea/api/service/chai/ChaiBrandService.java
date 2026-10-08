package com.ecotea.api.service.chai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecotea.api.common.constant.RedisConstant;
import com.ecotea.api.common.utils.JsonUtils;
import com.ecotea.api.common.utils.RedisUtils;
import com.ecotea.api.domain.chai.ChaiBrand;
import com.ecotea.api.mapper.chai.ChaiBrandMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChaiBrandService {

    private static final TypeReference<List<ChaiBrand>> LIST_TYPE = new TypeReference<List<ChaiBrand>>() {
    };

    private final ChaiBrandMapper chaiBrandMapper;

    /**
     * 上架品牌：按首字母 A-Z，同字母再按每字首字母串；非 A-Z 垫底（与 admin listOnlineOrdered 一致）。
     * Redis → miss → MySQL → SET（DAY_EXPIRE）。
     */
    public List<ChaiBrand> listOnlineOrdered() {
        String key = RedisConstant.CHAI_BRAND_ONLINE_KEY;
        String cached = RedisUtils.get(RedisUtils.defaultRedis(), key);
        if (cached != null) {
            List<ChaiBrand> fromCache = JsonUtils.readValueForCache(cached, LIST_TYPE);
            return fromCache != null ? fromCache : Collections.emptyList();
        }

        List<ChaiBrand> list = chaiBrandMapper.selectList(new LambdaQueryWrapper<ChaiBrand>()
                .eq(ChaiBrand::getStatus, 1));
        list.sort(Comparator
                .comparingInt((ChaiBrand b) -> isLetterInitial(b.getNameInitial()) ? 0 : 1)
                .thenComparing(b -> nullToEmpty(b.getNameInitial()), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(b -> nullToEmpty(b.getNamePinyin()), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(b -> nullToEmpty(b.getName()), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(b -> b.getId() == null ? 0L : b.getId()));
        String json = JsonUtils.writeValueAsString(list);
        if (json != null) {
            RedisUtils.set(RedisUtils.defaultRedis(), key, json, RedisConstant.DAY_EXPIRE);
        }
        return list;
    }

    /**
     * 上架品牌中按名称精确匹配（关键词搜品牌），走 {@link #listOnlineOrdered()} 缓存。
     */
    public Long resolveOnlineIdByNameExact(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String name = keyword.trim();
        for (ChaiBrand brand : listOnlineOrdered()) {
            if (name.equals(brand.getName())) {
                return brand.getId();
            }
        }
        return null;
    }

    /**
     * 按 id 补品牌名：先上架列表缓存，未命中再查库（含已下架）。
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
        for (ChaiBrand brand : listOnlineOrdered()) {
            if (brand.getId() != null && need.contains(brand.getId())) {
                map.put(brand.getId(), brand.getName());
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
        for (ChaiBrand brand : chaiBrandMapper.selectBatchIds(missing)) {
            map.put(brand.getId(), brand.getName());
        }
        return map;
    }

    public List<ChaiBrand> listAll() {
        return chaiBrandMapper.selectList(new LambdaQueryWrapper<ChaiBrand>()
                .orderByDesc(ChaiBrand::getOrderNum)
                .orderByDesc(ChaiBrand::getUpdateTime));
    }

    private static boolean isLetterInitial(String initial) {
        if (initial == null || initial.length() != 1) {
            return false;
        }
        char c = initial.charAt(0);
        return c >= 'A' && c <= 'Z';
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
