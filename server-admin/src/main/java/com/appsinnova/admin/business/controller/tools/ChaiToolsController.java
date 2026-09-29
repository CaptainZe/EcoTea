package com.appsinnova.admin.business.controller.tools;

import com.appsinnova.admin.business.service.chai.ChaiBrandService;
import com.appsinnova.admin.business.service.chai.ChaiSpuService;
import com.appsinnova.admin.common.utils.ResultVoUtil;
import com.appsinnova.admin.common.vo.ResultVo;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * 茶业数据工具（历史数据刷库等）。与业务 CRUD 分离，后续同类工具集中挂此目录。
 */
@Controller
@RequestMapping("/business/tools/chai")
@RequiredArgsConstructor
public class ChaiToolsController {

    private final ChaiBrandService chaiBrandService;
    private final ChaiSpuService chaiSpuService;

    @GetMapping("/index")
    @RequiresPermissions("business:tools:chai:index")
    public String index(Model model) {
        model.addAttribute("brandMissingLetters", chaiBrandService.countMissingNameLetters());
        Map<String, Long> searchTextMissing = chaiSpuService.countMissingSearchText();
        model.addAttribute("spuMissingSearchText", searchTextMissing.getOrDefault("spu", 0L));
        model.addAttribute("skuMissingSearchText", searchTextMissing.getOrDefault("sku", 0L));
        return "/business/tools/chai/index";
    }

    /**
     * 回填品牌 name_initial / name_pinyin。
     *
     * @param forceAll true=全量重算；false=仅空值
     */
    @PostMapping("/brand/fillNameLetters")
    @RequiresPermissions("business:tools:chai:index")
    @ResponseBody
    public ResultVo<?> fillBrandNameLetters(
            @RequestParam(value = "forceAll", defaultValue = "false") boolean forceAll) {
        Map<String, Integer> result = chaiBrandService.fillNameLettersBatch(forceAll);
        String mode = forceAll ? "全量重算" : "仅空值";
        String msg = String.format("%s完成：共 %d 条，更新 %d，跳过 %d",
                mode, result.get("total"), result.get("updated"), result.get("skipped"));
        return ResultVoUtil.success(msg, result);
    }

    /**
     * 回填 SPU / SKU 的 search_text（品牌名 + 商品名 + keywords）。
     *
     * @param forceAll true=全量重算；false=仅空值
     */
    @PostMapping("/searchText/fill")
    @RequiresPermissions("business:tools:chai:index")
    @ResponseBody
    public ResultVo<?> fillSearchText(
            @RequestParam(value = "forceAll", defaultValue = "false") boolean forceAll) {
        Map<String, Integer> result = chaiSpuService.fillSearchTextBatch(forceAll);
        String mode = forceAll ? "全量重算" : "仅空值";
        String msg = String.format(
                "%s完成：SPU 共 %d 更新 %d 跳过 %d；SKU 共 %d 更新 %d 跳过 %d",
                mode,
                result.get("spuTotal"), result.get("spuUpdated"), result.get("spuSkipped"),
                result.get("skuTotal"), result.get("skuUpdated"), result.get("skuSkipped"));
        return ResultVoUtil.success(msg, result);
    }
}
