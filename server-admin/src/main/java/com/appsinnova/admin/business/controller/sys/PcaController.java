package com.appsinnova.admin.business.controller.sys;

import com.appsinnova.admin.business.common.pca.PcaCodeService;
import com.appsinnova.admin.business.vo.base.PcaOptionVo;
import com.appsinnova.admin.common.utils.ResultVoUtil;
import com.appsinnova.admin.common.vo.ResultVo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 省市区三级联动通用接口（数据来源于 {@link PcaCodeService}，不暴露原始 JSON）。
 */
@RestController
@RequestMapping("/business/sys/pca")
@RequiredArgsConstructor
public class PcaController {

    private final PcaCodeService pcaCodeService;

    @GetMapping("/provinces")
    public ResultVo<List<PcaOptionVo>> provinces() {
        return ResultVoUtil.success(pcaCodeService.listProvinces());
    }

    @GetMapping("/cities")
    public ResultVo<List<PcaOptionVo>> cities(@RequestParam("provinceCode") String provinceCode) {
        return ResultVoUtil.success(pcaCodeService.listCities(provinceCode));
    }

    @GetMapping("/districts")
    public ResultVo<List<PcaOptionVo>> districts(@RequestParam("cityCode") String cityCode) {
        return ResultVoUtil.success(pcaCodeService.listDistricts(cityCode));
    }
}
