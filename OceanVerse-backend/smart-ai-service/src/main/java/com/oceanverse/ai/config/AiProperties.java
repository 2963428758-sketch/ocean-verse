package com.oceanverse.ai.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * AI 模块配置属性
 * <p>
 * 绑定 application.yml 中的 oceanverse.ai.* 配置段，
 * 集中管理模型名称、Token 限制、温度等可调参数。
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "oceanverse.ai")
public class AiProperties {

    /**
     * 图像识别模型名称（多模态视觉模型）
     */
    private String imageModel = "qwen-vl-max";

    /**
     * 智能对话模型名称
     */
    private String chatModel = "qwen-plus";

    /**
     * 向量化模型名称（用于 RAG 检索）
     */
    private String embeddingModel = "text-embedding-v3";

    /**
     * 单次对话最大 Token 数
     */
    private Integer maxTokens = 2048;

    /**
     * 问答温度参数（0-1，越高越随机）
     */
    private Double temperature = 0.7;

    /**
     * 多轮对话保留的历史轮数
     */
    private Integer historyRounds = 10;

    /**
     * 会话 TTL（分钟），超过此时间无活动后会话自动过期
     */
    private Long sessionTtlMinutes = 30L;

    /**
     * RAG 语义检索相似度阈值（0-1），低于此值的文档将被过滤
     */
    private Double similarityThreshold = 0.7;

    /**
     * 向量索引持久化文件路径（SimpleVectorStore save/load），
     * 为空时不持久化，每次重启重新向量化
     */
    private String vectorStorePath = "smart-ai-service/data/ai/vector-store.json";

    // ==================== 语义缓存配置（任务 3.1）====================

    /**
     * 语义缓存 TTL（小时），Redis 缓存过期时间
     */
    private Integer cacheTtlHours = 24;

    /**
     * 语义缓存向量相似度阈值（0-1），
     * 实测语义等价但措辞不同的问题余弦相似度约 0.886-0.897，取 0.85 较合理
     */
    private Double cacheSemanticThreshold = 0.85;

    /**
     * 语义缓存向量存储持久化文件路径
     */
    private String cacheVectorStorePath = "smart-ai-service/data/ai/cache-vector-store.json";

    // ==================== 限流配置（任务 3.2）====================

    /**
     * 每日问答调用上限
     */
    private Integer dailyChatLimit = 50;

    /**
     * 每日图像识别调用上限
     */
    private Integer dailyRecognitionLimit = 10;

    // ==================== 路径规范化 ====================

    /**
     * 启动时规范化文件路径，确保从不同工作目录启动都能定位到正确的 data/ai/ 目录。
     * <p>
     * 例如：从 OceanVerse/ 启动时 data/ai/xxx.json 直接可用；
     * 从 OceanVerse-backend/ 或子模块启动时，向上查找 data/ai/ 目录。
     */
    @PostConstruct
    public void resolvePaths() {
        vectorStorePath = resolveDataPath(vectorStorePath);
        cacheVectorStorePath = resolveDataPath(cacheVectorStorePath);
    }

    /**
     * 解析相对路径，确保指向项目根目录下的 data/ai/ 位置。
     * <p>
     * 策略：
     * 1. 如果当前工作目录下该路径存在 → 直接使用
     * 2. 否则向上遍历父目录，找到包含 data/ai/ 的项目根目录
     * 3. 如果仍未找到 → 保持原路径（首次运行时会新建）
     */
    private String resolveDataPath(String path) {
        if (path == null || path.isEmpty() || new File(path).isAbsolute()) {
            return path;
        }

        // 当前工作目录下该路径存在 → 直接返回
        if (new File(path).exists()) {
            return new File(path).getAbsolutePath();
        }

        // 向上查找包含 data/ai/ 的项目根目录
        File dir = new File(System.getProperty("user.dir"));
        for (int i = 0; i < 5 && dir != null; i++) {
            File candidate = new File(dir, path);
            if (candidate.getParentFile() != null && candidate.getParentFile().exists()) {
                log.debug("路径解析: {} → {}", path, candidate.getAbsolutePath());
                return candidate.getAbsolutePath();
            }
            dir = dir.getParentFile();
        }

        // 兜底：返回当前工作目录下的路径
        return new File(path).getAbsolutePath();
    }
}
