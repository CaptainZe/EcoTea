package com.ecotea.api.controller.chai;

import com.ecotea.api.common.result.ApiResult;
import com.ecotea.api.domain.chai.ChaiBrand;
import com.ecotea.api.service.chai.ChaiBrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/chai/brand")
@RequiredArgsConstructor
public class ChaiBrandController {

    private final ChaiBrandService chaiBrandService;

    /**
     * 上架品牌列表（只读，用于验证共享库 + MyBatis-Plus）
     */
    @GetMapping("/list")
    public ApiResult<List<ChaiBrand>> list() {
        return ApiResult.ok(chaiBrandService.listOnlineOrdered());
    }

    /**
     * 全部品牌（含下架）
     */
    @GetMapping("/listAll")
    public ApiResult<List<ChaiBrand>> listAll() {
        return ApiResult.ok(chaiBrandService.listAll());
    }
}
