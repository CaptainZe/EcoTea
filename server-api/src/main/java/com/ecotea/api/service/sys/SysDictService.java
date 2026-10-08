package com.ecotea.api.service.sys;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecotea.api.common.constant.RedisConstant;
import com.ecotea.api.common.utils.JsonUtils;
import com.ecotea.api.common.utils.RedisUtils;
import com.ecotea.api.domain.sys.SysDict;
import com.ecotea.api.mapper.sys.SysDictMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SysDictService {

    /** 与 EcoTea StatusEnum.OK 一致 */
    private static final byte STATUS_OK = 1;

    private final SysDictMapper sysDictMapper;

    /**
     * 按标识取启用字典：Redis → miss → MySQL → SET（DAY_EXPIRE；未命中库缓存 "null" 短 TTL）。
     */
    public SysDict getByNameOk(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        String dictName = name.trim();
        String key = String.format(RedisConstant.DICT_KEY, dictName);
        String cached = RedisUtils.get(RedisUtils.defaultRedis(), key);
        if (cached != null) {
            return JsonUtils.readValueForCache(cached, SysDict.class);
        }

        SysDict dict = sysDictMapper.selectOne(new LambdaQueryWrapper<SysDict>()
                .eq(SysDict::getName, dictName)
                .eq(SysDict::getStatus, STATUS_OK)
                .last("LIMIT 1"));
        if (dict != null) {
            String json = JsonUtils.writeValueAsString(dict);
            if (json != null) {
                RedisUtils.set(RedisUtils.defaultRedis(), key, json, RedisConstant.DAY_EXPIRE);
            }
        } else {
            RedisUtils.set(RedisUtils.defaultRedis(), key, "null", RedisConstant.FIVE_MINUTE_EXPIRE);
        }
        return dict;
    }
}
