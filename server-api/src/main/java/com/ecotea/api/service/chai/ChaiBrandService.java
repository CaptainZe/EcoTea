package com.ecotea.api.service.chai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecotea.api.common.constant.RedisConstant;
import com.ecotea.api.common.utils.JsonUtils;
import com.ecotea.api.common.utils.RedisUtils;
import com.ecotea.api.domain.chai.ChaiBrand;
import com.ecotea.api.mapper.chai.ChaiBrandMapper;
import com.ecotea.api.vo.chai.ChaiBrandPickerGroupVO;
import com.ecotea.api.vo.chai.ChaiBrandPickerItemVO;
import com.ecotea.api.vo.chai.ChaiBrandPickerVO;
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
import java.util.LinkedHashMap;
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
     * 上架品牌（H5）：常用（common_flag=1）置顶，再按首字母 A-Z / 全字母 / 名称。
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
                .comparingInt((ChaiBrand b) -> Integer.valueOf(1).equals(b.getCommonFlag()) ? 0 : 1)
                .thenComparingInt(b -> isLetterInitial(b.getNameInitial()) ? 0 : 1)
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
     * 品牌选页结构：常用快捷区 + 全量字母分组 + 右侧索引。
     * <p>复用 {@link #listOnlineOrdered()} 缓存，不改变其排序与 H5 行为；
     * 字母区为全量（含常用品牌），常用区单独便于快选。</p>
     */
    public ChaiBrandPickerVO buildOnlinePicker() {
        List<ChaiBrand> rows = listOnlineOrdered();
        ChaiBrandPickerVO vo = new ChaiBrandPickerVO();
        if (rows == null || rows.isEmpty()) {
            return vo;
        }

        Comparator<ChaiBrand> alphaCmp = Comparator
                .comparingInt((ChaiBrand b) -> isLetterInitial(normalizeInitial(b.getNameInitial())) ? 0 : 1)
                .thenComparing(b -> normalizeInitial(b.getNameInitial()), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(b -> nullToEmpty(b.getNamePinyin()), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(b -> nullToEmpty(b.getName()), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(b -> b.getId() == null ? 0L : b.getId());

        List<ChaiBrand> commonRows = new ArrayList<>();
        for (ChaiBrand brand : rows) {
            if (Integer.valueOf(1).equals(brand.getCommonFlag())) {
                commonRows.add(brand);
            }
        }
        commonRows.sort(alphaCmp);
        for (ChaiBrand brand : commonRows) {
            vo.getCommon().add(toPickerItem(brand));
        }

        Map<String, List<ChaiBrand>> byInitial = new LinkedHashMap<>();
        for (char c = 'A'; c <= 'Z'; c++) {
            byInitial.put(String.valueOf(c), new ArrayList<>());
        }
        byInitial.put("#", new ArrayList<>());
        for (ChaiBrand brand : rows) {
            String initial = normalizeInitial(brand.getNameInitial());
            byInitial.get(initial).add(brand);
        }

        for (Map.Entry<String, List<ChaiBrand>> e : byInitial.entrySet()) {
            List<ChaiBrand> bucket = e.getValue();
            if (bucket.isEmpty()) {
                continue;
            }
            bucket.sort(alphaCmp);
            ChaiBrandPickerGroupVO group = new ChaiBrandPickerGroupVO();
            group.setInitial(e.getKey());
            for (ChaiBrand brand : bucket) {
                group.getItems().add(toPickerItem(brand));
            }
            vo.getGroups().add(group);
        }

        if (!vo.getCommon().isEmpty()) {
            vo.getIndex().add("常");
        }
        for (ChaiBrandPickerGroupVO group : vo.getGroups()) {
            vo.getIndex().add(group.getInitial());
        }
        return vo;
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

    private static ChaiBrandPickerItemVO toPickerItem(ChaiBrand brand) {
        ChaiBrandPickerItemVO item = new ChaiBrandPickerItemVO();
        item.setId(brand.getId());
        item.setName(brand.getName());
        item.setNameInitial(normalizeInitial(brand.getNameInitial()));
        item.setNamePinyin(brand.getNamePinyin());
        item.setLogo(brand.getLogo());
        return item;
    }

    /** 非 A-Z 一律归为 # */
    private static String normalizeInitial(String initial) {
        if (initial == null || initial.isEmpty()) {
            return "#";
        }
        String t = initial.trim().toUpperCase();
        if (t.length() == 1 && t.charAt(0) >= 'A' && t.charAt(0) <= 'Z') {
            return t;
        }
        return "#";
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
