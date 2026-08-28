package com.ecotea.api.service.sys;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

    public SysDict getByNameOk(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        return sysDictMapper.selectOne(new LambdaQueryWrapper<SysDict>()
                .eq(SysDict::getName, name.trim())
                .eq(SysDict::getStatus, STATUS_OK)
                .last("LIMIT 1"));
    }
}
