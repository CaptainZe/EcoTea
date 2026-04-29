package com.appsinnova.admin.business.service.base;

import com.appsinnova.admin.business.common.enums.AppSecretKeyType;
import com.appsinnova.admin.business.domain.sys.AppSecretKey;
import com.appsinnova.admin.business.service.sys.AppSecretKeyService;
import com.lark.oapi.Client;
import com.lark.oapi.service.im.v1.model.CreateImageReq;
import com.lark.oapi.service.im.v1.model.CreateImageReqBody;
import com.lark.oapi.service.im.v1.model.CreateImageResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeiShuImageService {
    private final AppSecretKeyService appSecretKeyService;

    /**
     * 通过图片 URL 上传到飞书，返回 image_key
     */
    public String uploadImageByUrl(String imageUrl) {
        File tempFile = null;

        try {
            // 1️⃣ 下载图片到临时文件
            tempFile = File.createTempFile("feishu-img-", ".png");

            try (InputStream in = new URL(imageUrl).openStream()) {
                Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            // 2️⃣ 使用 SDK 上传
            CreateImageReq req = CreateImageReq.newBuilder()
                    .createImageReqBody(
                            CreateImageReqBody.newBuilder()
                                    .imageType("message") // ✅ 固定值
                                    .image(tempFile)      // ✅ 直接传 File
                                    .build()
                    )
                    .build();

            AppSecretKey appSecretKey = appSecretKeyService.getSecretKey(AppSecretKeyType.FEISHU_APP.getCode());
            String appId = appSecretKey.getAccessKey();
            String appSecret = appSecretKey.getAccessSecret();

            Client feishuClient = Client.newBuilder(appId, appSecret).build();
            CreateImageResp resp = feishuClient
                    .im()
                    .v1()
                    .image()
                    .create(req);

            // 3️⃣ 错误处理
            if (!resp.success()) {
                log.error("Feishu upload image failed, code={}, msg={}, reqId={}",
                        resp.getCode(), resp.getMsg(), resp.getRequestId());
                throw new RuntimeException("飞书图片上传失败：" + resp.getMsg());
            }

            // 4️⃣ 返回 image_key
            return resp.getData().getImageKey();

        } catch (Exception e) {
            log.error("uploadImageByUrl error, url={}", imageUrl, e);
            throw new RuntimeException("通过 URL 上传飞书图片失败", e);

        } finally {
            // 5️⃣ 删除临时文件
            if (tempFile != null && tempFile.exists()) {
                boolean deleted = tempFile.delete();
                if (!deleted) {
                    log.warn("临时文件删除失败: {}", tempFile.getAbsolutePath());
                }
            }
        }
    }
}