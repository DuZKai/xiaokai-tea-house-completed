package com.xiaokai.controller.admin;

import com.aliyun.oss.*;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.EnvironmentVariableCredentialsProvider;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.xiaokai.constant.MessageConstant;
import com.xiaokai.result.Result;
import com.xiaokai.util.OssUtil;
import com.xiaokai.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/**
 * 公共接口
 */
@RestController
@RequestMapping("/admin/common")
@Api(tags = "公共接口")
@Slf4j
public class CommonContorller {

    @Autowired
    private AliOssUtil aliOssUtil;

    private OssUtil ossUtil;

    public CommonContorller(OssUtil ossUtil) {
        this.ossUtil = ossUtil;
    }

    /**
     * 文件上传
     * @param file
     * @return
     */
    @PostMapping("/upload")
    @ApiOperation("文件上传")
    public Result<String> upload(MultipartFile file) {
        log.info("文件上传: {}", file.getOriginalFilename());

        try {
            // 获取文件名
            String originalFilename = file.getOriginalFilename();
            // 获取文件后缀
            String extension = null;
            if (originalFilename != null) {
                extension = Objects.requireNonNull(file.getOriginalFilename()).substring(originalFilename.lastIndexOf("."));
            }
            // 生成新唯一文件名
            String objectName = UUID.randomUUID().toString() + extension;
            // 文件请求路径
            aliOssUtil.upload(file.getBytes(), objectName);
            String filepath = ossUtil.getOssUrl(objectName);
            return Result.success(filepath);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return Result.error(MessageConstant.UPLOAD_FAILED);
    }
}