package com.ecotea.api.controller.chai;

import com.ecotea.api.common.result.ApiResult;
import com.ecotea.api.service.chai.ChaiWarehouseQueryService;
import com.ecotea.api.vo.chai.ChaiWarehouseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 仓库只读接口（内部价目筛选等）。
 */
@RestController
@RequestMapping("/chai/warehouse")
@RequiredArgsConstructor
public class ChaiWarehouseController {

    private final ChaiWarehouseQueryService chaiWarehouseQueryService;

    /**
     * 上架仓库列表（id / name / shortName），按排序号大到小。
     */
    @GetMapping("/online")
    public ApiResult<List<ChaiWarehouseVO>> online() {
        return ApiResult.ok(chaiWarehouseQueryService.listOnline());
    }
}
