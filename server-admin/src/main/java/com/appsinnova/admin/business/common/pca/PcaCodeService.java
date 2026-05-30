package com.appsinnova.admin.business.common.pca;

import com.appsinnova.admin.business.domain.tea.TeaPartner;
import com.appsinnova.admin.business.vo.base.PcaOptionVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;

/**
 * 省市区（PCA）编码目录唯一入口：加载、查询、校验、名称解析均经此服务，禁止业务代码直接读取 pca-code.json。
 */
@Service
public class PcaCodeService {

    private PcaCodeCatalog catalog;

    @PostConstruct
    public void init() {
        this.catalog = PcaCodeLoader.load();
    }

    public List<PcaOptionVo> listProvinces() {
        return catalog.listProvinces();
    }

    public List<PcaOptionVo> listCities(String provinceCode) {
        if (StringUtils.isBlank(provinceCode)) {
            return Collections.emptyList();
        }
        return catalog.listChildren(provinceCode.trim());
    }

    public List<PcaOptionVo> listDistricts(String cityCode) {
        if (StringUtils.isBlank(cityCode)) {
            return Collections.emptyList();
        }
        return catalog.listChildren(cityCode.trim());
    }

    /**
     * 编码转名称；未知编码时返回原编码（兼容未迁移的历史数据）。
     */
    public String resolveName(String code) {
        if (StringUtils.isBlank(code)) {
            return "";
        }
        String name = catalog.getName(code.trim());
        return name != null ? name : code.trim();
    }

    /**
     * @return 错误信息；合法时返回 null
     */
    public String validateRegion(String provinceCode, String cityCode, String districtCode) {
        return catalog.validateRegion(provinceCode, cityCode, districtCode);
    }

    /** 填充列表/表单展示用名称 */
    public void fillRegionNames(TeaPartner partner) {
        if (partner == null) {
            return;
        }
        partner.setProvinceName(resolveName(partner.getProvince()));
        partner.setCityName(resolveName(partner.getCity()));
        partner.setDistrictName(resolveName(partner.getDistrict()));
    }
}
