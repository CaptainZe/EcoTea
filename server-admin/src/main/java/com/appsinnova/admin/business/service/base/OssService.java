package com.appsinnova.admin.business.service.base;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
public class OssService {

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;
    @Value("${aliyun.oss.bucket}")
    private String bucketName;
    @Value("${aliyun.oss.access-key-id}")
    private String accessKeyId;
    @Value("${aliyun.oss.access-key-secret}")
    private String accessKeySecret;
    @Value("${aliyun.oss.host}")
    private String host;
    @Value("${aliyun.oss.path}")
    private String path;


    public String uploadFile(MultipartFile file) {
        try {
            OSS ossClient = new OSSClientBuilder()
                    .build(endpoint, accessKeyId, accessKeySecret);

            // ✅ 文件名处理
            String originalFilename = file.getOriginalFilename();
            String suffix = null;
            if (originalFilename != null) {
                suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID() + suffix;

            // ✅ OSS 路径
            String objectName = path + "/" + fileName;

            ossClient.putObject(
                    bucketName,
                    objectName,
                    file.getInputStream()
            );

            ossClient.shutdown();

            return host + "/" + objectName;
        } catch (Exception e) {
            log.error("OssService 上传文件异常: {}", e.getMessage());
            return null;
        }
    }
}
