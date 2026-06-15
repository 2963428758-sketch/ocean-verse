package com.oceanverse.common.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.oceanverse.common.config.OssProperties;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 阿里云 OSS 工具类
 * <p>
 * 提供文件上传、下载、删除等操作，
 * 自动按日期生成目录结构，支持自定义域名。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OssUtil {

    private final OssProperties ossProperties;
    private volatile OSS ossClient;

    /**
     * 获取 OSS 客户端（懒加载，线程安全）
     */
    private OSS getClient() {
        if (ossClient == null) {
            synchronized (this) {
                if (ossClient == null) {
                    ossClient = new OSSClientBuilder().build(
                            ossProperties.getEndpoint(),
                            ossProperties.getAccessKeyId(),
                            ossProperties.getAccessKeySecret());
                    log.info("OSS 客户端已初始化: endpoint={}, bucket={}",
                            ossProperties.getEndpoint(), ossProperties.getBucketName());
                }
            }
        }
        return ossClient;
    }

    /**
     * 上传 MultipartFile 到 OSS
     *
     * @param file 上传的文件
     * @return 文件访问 URL
     */
    public String upload(MultipartFile file) {
        String key = generateKey(file.getOriginalFilename());
        try (InputStream is = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());

            getClient().putObject(ossProperties.getBucketName(), key, is, metadata);
            String url = buildUrl(key);
            log.info("文件已上传至 OSS: key={}, url={}", key, url);
            return url;
        } catch (Exception e) {
            log.error("OSS 文件上传失败: fileName={}", file.getOriginalFilename(), e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    /**
     * 上传 InputStream 到 OSS
     *
     * @param key         对象 key（完整路径）
     * @param inputStream 输入流
     * @param contentType MIME 类型
     * @return 文件访问 URL
     */
    public String upload(String key, InputStream inputStream, String contentType) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            if (contentType != null) {
                metadata.setContentType(contentType);
            }
            getClient().putObject(ossProperties.getBucketName(), key, inputStream, metadata);
            String url = buildUrl(key);
            log.info("文件已上传至 OSS: key={}, url={}", key, url);
            return url;
        } catch (Exception e) {
            log.error("OSS 文件上传失败: key={}", key, e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    /**
     * 上传字节数组到 OSS
     *
     * @param key      对象 key
     * @param bytes    字节数组
     * @param contentType MIME 类型
     * @return 文件访问 URL
     */
    public String uploadBytes(String key, byte[] bytes, String contentType) {
        try (InputStream is = new java.io.ByteArrayInputStream(bytes)) {
            return upload(key, is, contentType);
        } catch (Exception e) {
            log.error("OSS 字节上传失败: key={}", key, e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    /**
     * 从 OSS 获取文件流
     *
     * @param key 对象 key
     * @return 输入流
     */
    public InputStream download(String key) {
        try {
            return getClient().getObject(ossProperties.getBucketName(), key).getObjectContent();
        } catch (Exception e) {
            log.error("OSS 文件下载失败: key={}", key, e);
            throw new RuntimeException("文件下载失败", e);
        }
    }

    /**
     * 删除 OSS 文件
     *
     * @param key 对象 key
     */
    public void delete(String key) {
        try {
            getClient().deleteObject(ossProperties.getBucketName(), key);
            log.info("OSS 文件已删除: key={}", key);
        } catch (Exception e) {
            log.error("OSS 文件删除失败: key={}", key, e);
            throw new RuntimeException("文件删除失败", e);
        }
    }

    /**
     * 判断文件是否存在
     *
     * @param key 对象 key
     * @return 是否存在
     */
    public boolean exists(String key) {
        return getClient().doesObjectExist(ossProperties.getBucketName(), key);
    }

    /**
     * 获取文件的完整访问 URL
     *
     * @param key 对象 key
     * @return 访问 URL
     */
    public String getUrl(String key) {
        return buildUrl(key);
    }

    /**
     * 从 URL 中提取 key
     *
     * @param url 文件 URL
     * @return 对象 key
     */
    public String extractKey(String url) {
        if (url == null) return null;
        String baseUrl = getBaseUrl();
        if (url.startsWith(baseUrl)) {
            return url.substring(baseUrl.length());
        }
        return url;
    }

    // ==================== 私有方法 ====================

    /**
     * 生成存储 key：dirPrefix/yyyy/MM/dd/uuid.ext
     */
    private String generateKey(String originalFilename) {
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = UUID.randomUUID().toString().replace("-", "");

        StringBuilder sb = new StringBuilder();
        if (ossProperties.getDirPrefix() != null && !ossProperties.getDirPrefix().isEmpty()) {
            sb.append(ossProperties.getDirPrefix());
            if (!ossProperties.getDirPrefix().endsWith("/")) {
                sb.append("/");
            }
        }
        sb.append(datePath).append("/").append(uuid).append(ext);
        return sb.toString();
    }

    /**
     * 构建文件访问 URL
     */
    private String buildUrl(String key) {
        String domain = ossProperties.getCustomDomain();
        if (domain != null && !domain.isEmpty()) {
            if (!domain.endsWith("/")) domain += "/";
            return domain + key;
        }
        return getBaseUrl() + key;
    }

    private String getBaseUrl() {
        String endpoint = ossProperties.getEndpoint();
        String bucket = ossProperties.getBucketName();
        // https://bucketName.oss-cn-beijing.aliyuncs.com/
        return "https://" + bucket + "." + endpoint + "/";
    }

    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
            log.info("OSS 客户端已关闭");
        }
    }
}
