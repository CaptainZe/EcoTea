package com.ecotea.api.controller.chai;

import com.ecotea.api.common.constant.ChaiConstant;
import com.ecotea.api.common.result.ApiResult;
import com.ecotea.api.common.utils.DictUtils;
import com.ecotea.api.vo.chai.ChaiDictOptionVO;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 字典只读接口（H5 筛选等）。仅开放白名单 label。
 */
@RestController
@RequestMapping("/chai/dict")
public class ChaiDictController {

    private static final Set<String> ALLOWED = new LinkedHashSet<>();

    static {
        ALLOWED.add(ChaiConstant.DICT_CHAI_TYPE);
    }

    /**
     * 字典选项列表。label 例：CHAI_TYPE（茶类）。
     */
    @GetMapping("/options")
    public ApiResult<List<ChaiDictOptionVO>> options(@RequestParam String label) {
        String key = label == null ? "" : label.trim();
        if (!StringUtils.hasText(key) || !ALLOWED.contains(key)) {
            return ApiResult.fail("不支持的字典");
        }
        Map<String, String> map = DictUtils.value(key);
        if (map == null || map.isEmpty()) {
            return ApiResult.ok(Collections.emptyList());
        }
        List<ChaiDictOptionVO> list = new ArrayList<>(map.size());
        for (Map.Entry<String, String> e : map.entrySet()) {
            ChaiDictOptionVO vo = new ChaiDictOptionVO();
            vo.setCode(e.getKey());
            vo.setText(e.getValue());
            list.add(vo);
        }
        return ApiResult.ok(list);
    }
}
