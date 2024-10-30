package com.xiaokai.util;

import com.aliyun.oss.*;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.xiaokai.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Date;

@Service
@Slf4j
public class OssUtil {

    @Autowired
    private AliOssUtil aliOssUtil;

    public String getOssUrl(String objectName) {
        if(objectName == null || objectName.isEmpty() || objectName.contains("/")){
            return objectName;
        }
        else{
            return useNameGetOssUrl(objectName);
        }
    }

    /**
     * 生成OSS真正URL
     * @param objectName: 文件名，例如exampleobject.txt。Object完整路径中不能包含Bucket名称。
     * @return
     * @throws Throwable
     */
    public String useNameGetOssUrl(String objectName){

        String accessKeyId = aliOssUtil.getAccessKeyId();
        String secretAccessKey = aliOssUtil.getAccessKeySecret();
        String endpoint = aliOssUtil.getEndpoint();
        String bucketName = aliOssUtil.getBucketName();

        // 创建OSSClient实例
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, secretAccessKey);

        try {
            // 指定生成的签名URL过期时间，单位为毫秒。本示例以设置过期时间为1小时为例。
            Date expiration = new Date(new Date().getTime() + 3600 * 1000L);

            // 生成签名URL。
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, objectName, HttpMethod.GET);
            // 设置过期时间。
            request.setExpiration(expiration);

            // 通过HTTP GET请求生成签名URL。
            URL signedUrl = ossClient.generatePresignedUrl(request);
            // 打印签名URL。
            // log.info("真实请求URL:{}", signedUrl);
            return signedUrl.toString();
        } catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
        } catch (ClientException ce) {
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message:" + ce.getMessage());
        }
        return null;
    }


    /**
     * 根据url获取图片名称
     * @param url
     */
    public String use_name_get_url(String url){
        if(url != null && url.startsWith("http")) {
            url = url.replace("http://", "");
            url = url.replace("https://", "");
            String[] splitByQuestionMark = url.split("\\?");
            if(splitByQuestionMark.length >= 1)
                url = splitByQuestionMark[0];
            String[] splitBySlash = url.split("/");
            if(splitBySlash.length >= 2)
                url = splitBySlash[1];
            return url;
        }
        return url;
    }

}
