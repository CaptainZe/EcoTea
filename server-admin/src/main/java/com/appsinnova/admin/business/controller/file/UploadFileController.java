package com.appsinnova.admin.business.controller.file;

import com.appsinnova.admin.business.service.base.OssService;
import com.appsinnova.admin.common.utils.ResultVoUtil;
import com.appsinnova.admin.common.vo.ResultVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/business/upload")
@RequiredArgsConstructor
public class UploadFileController {

    private final OssService ossService;

    @RequestMapping("/file")
    @ResponseBody
    public ResultVo<?> uploadFile(@RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            Integer fileSize = file.getBytes().length;
            String fileMd5 = DigestUtils.md5Hex(file.getBytes());

            String fileUrl = ossService.uploadFile(file);

            Map<String, Object> data = new HashMap<>(3);
            data.put("src", fileUrl);
            data.put("size", fileSize);
            data.put("md5", fileMd5);

            //兼容 layedit 图片返回值
            ResultVo<Object> ret = new ResultVo<>();
            ret.setCode(0);
            ret.setData(data);
            return ret;
        } catch (Exception e) {
            log.error("UploadFileController 上传文件异常: {}", e.getMessage());
            return ResultVoUtil.error("上传文件失败");
        }
    }
}