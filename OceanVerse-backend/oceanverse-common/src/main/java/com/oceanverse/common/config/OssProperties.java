package com.oceanverse.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云 OSS 配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "oceanverse.oss")
public class OssProperties {

    /** OSS Endpoint，例如 oss-cn-beijing.aliyuncs.com */
    private String endpoint;

    /** AccessKey ID */
    private String accessKeyId;

    /** AccessKey Secret */
    private String accessKeySecret;

    /** Bucket 名称 */
    private String bucketName;

    /** 文件存储目录前缀，默认为空 */
    private String dirPrefix = "";

    /** 自定义域名（如已绑定），为空则使用默认域名 */
    private String customDomain = "";
}
