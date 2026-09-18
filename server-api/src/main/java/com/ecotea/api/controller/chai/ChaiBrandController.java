package com.ecotea.api.controller.chai;

import com.ecotea.api.common.result.ApiResult;
import com.ecotea.api.domain.chai.ChaiBrand;
import com.ecotea.api.service.chai.ChaiBrandService;
import com.ecotea.api.vo.chai.ChaiBrandVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 品牌只读接口（H5 筛选等）。
 */
@RestController
@RequestMapping("/chai/brand")
@RequiredArgsConstructor
public class ChaiBrandController {

    private final ChaiBrandService chaiBrandService;

    /**
     * 上架品牌列表（id / name），按排序号大到小。
     */
    @GetMapping("/online")
    public ApiResult<List<ChaiBrandVO>> online() {
        List<ChaiBrand> rows = chaiBrandService.listOnlineOrdered();
        if (rows == null || rows.isEmpty()) {
            return ApiResult.ok(Collections.emptyList());
        }
        List<ChaiBrandVO> list = new ArrayList<>(rows.size());
        for (ChaiBrand brand : rows) {
            ChaiBrandVO vo = new ChaiBrandVO();
            vo.setId(brand.getId());
            vo.setName(brand.getName());
            list.add(vo);
        }
        return ApiResult.ok(list);
    }
}
