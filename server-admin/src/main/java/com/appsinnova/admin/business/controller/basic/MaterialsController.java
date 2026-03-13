package com.appsinnova.admin.business.controller.basic;

import com.appsinnova.admin.business.domain.basic.MaterialsModel;
import com.appsinnova.admin.business.service.basic.MaterialsService;
import com.appsinnova.admin.common.utils.JsonUtils;
import com.appsinnova.admin.common.utils.ResultVoUtil;
import com.appsinnova.admin.common.vo.ResultVo;
import com.appsinnova.admin.component.shiro.ShiroUtil;
import com.appsinnova.admin.system.domain.User;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/business/basic/materials")
@RequiredArgsConstructor
public class MaterialsController {

    private final MaterialsService materialsService;

    // 列表页面
    @GetMapping("/index")
    @RequiresPermissions("business:basic:materials:index")
    public String index(Model model, MaterialsModel materials) {
        Page<MaterialsModel> list = materialsService.getPageList(materials);
        list.forEach(item -> {
            if (StringUtils.isNotBlank(item.getImageUrls())) {
                String urlShow = "<div class=\"photo-group\">";
                List<String> urlList = JsonUtils.readValue(item.getImageUrls(), new TypeReference<List<String>>() {});

                for (String imgItem : urlList) {
                    if (StringUtils.isBlank(imgItem)) {
                        continue;
                    }

                    urlShow += "<img class=\"preview-img\" layer-src=\"" + imgItem +
                            "\" src=\"" + imgItem +
                            "\" style=\"width:60px;cursor:pointer;\">";

                    urlShow += "&nbsp;&nbsp;";
                }

                urlShow += "</div>";
                item.setUrlShow(urlShow);
            }
        });

        // 封装数据
        model.addAttribute("list", list.getContent());
        model.addAttribute("page", list);
        return "/business/basic/materials/index";
    }

    // 跳转到编辑页面
    @GetMapping({"/edit", "/edit/{id}"})
    @RequiresPermissions("business:basic:materials:index")
    public String toEdit(@PathVariable(value = "id", required = false) MaterialsModel config, Model model) {
        // 初始化（可设置默认值）
        if (config == null) {
            config = new MaterialsModel();
        }

        model.addAttribute("config", config);
        return "/business/basic/materials/edit";
    }

    // 跳转到拷贝页面
    @GetMapping("/copy/{id}")
    @RequiresPermissions("business:basic:materials:index")
    public String toCopy(@PathVariable(value = "id", required = false) MaterialsModel config, Model model) {
        config.setId(null);

        model.addAttribute("config", config);
        return "/business/basic/materials/edit";
    }

    // 保存
    @PostMapping("/save")
    @RequiresPermissions("business:basic:materials:index")
    @ResponseBody
    public ResultVo<?> save(MaterialsModel config) {
        Long currentTime = System.currentTimeMillis();

        if (config.getId() != null) {
            MaterialsModel oldEntity = materialsService.getById(config.getId());
            if (oldEntity == null) {
                return ResultVoUtil.error("无效的配置ID");
            }

            config.setUpdateTime(currentTime);
            config.setCreateTime(oldEntity.getCreateTime());
        } else {
            config.setUpdateTime(currentTime);
            config.setCreateTime(currentTime);
        }

        User user = ShiroUtil.getSubject();
        config.setOperator(user.getNickname());
        materialsService.save(config);
        return ResultVoUtil.SAVE_SUCCESS;
    }

    // 删除
    @RequestMapping("/delete")
    @RequiresPermissions("business:basic:materials:index")
    @ResponseBody
    public ResultVo<?> toDelete(@RequestParam(value = "ids", required = false) List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return ResultVoUtil.error("请选择要删除的数据");
        }

        materialsService.deleteByIdIn(ids);
        return ResultVoUtil.success("删除成功");
    }
}