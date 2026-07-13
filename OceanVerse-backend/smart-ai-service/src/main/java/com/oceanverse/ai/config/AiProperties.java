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
    private Double cacheSemanticThreshold = 0.80;

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

    /**
     * 识别配额预警阈值（剩余次数 ≤ 此值时触发通知）
     */
    private Integer recognitionWarningThreshold = 2;

    /**
     * 对话配额预警阈值（剩余次数 ≤ 此值时触发通知）
     */
    private Integer chatWarningThreshold = 5;

    // ==================== 路径规范化 ====================

    /**
     * 启动时规范化文件路径，确保从不同工作目录启动都能定位到
     * OceanVerse-backend/smart-ai-service/data/ai/ 目录。
     */
    @PostConstruct
    public void resolvePaths() {
        vectorStorePath = resolveDataPath(vectorStorePath);
        cacheVectorStorePath = resolveDataPath(cacheVectorStorePath);
    }

    /**
     * 解析相对路径，定位到 Maven 多模块项目根目录下。
     * <p>
     * 策略：
     * 1. 当前工作目录已包含 smart-ai-service/ → 就是项目根，直接拼接
     * 2. 否则向上遍历，找到包含 smart-ai-service/ 子目录的那一层
     * 3. 兜底返回工作目录下的绝对路径
     * <p>
     * 示例（path = smart-ai-service/data/ai/vector-store.json）：
     * - 从 OceanVerse-backend/ 启动 → 当前目录包含 smart-ai-service/ ✓
     * - 从 OceanVerse/ 启动 → 向上找，在 OceanVerse-backend/ 下找到 ✓
     * - 从 oceanverse-app/ 启动 → 向上到 OceanVerse-backend/ 找到 ✓
     */
    private String resolveDataPath(String path) {
        if (path == null || path.isEmpty() || new File(path).isAbsolute()) {
            return path;
        }

        File workDir = new File(System.getProperty("user.dir"));

        // 当前工作目录已包含 smart-ai-service/ → 直接拼接
        if (new File(workDir, "smart-ai-service").isDirectory()) {
            File resolved = new File(workDir, path);
            log.debug("路径解析: {} → {}", path, resolved.getAbsolutePath());
            return resolved.getAbsolutePath();
        }

        // 当前目录的子目录中查找（覆盖从上层目录启动的场景，如 OceanVerse/ → OceanVerse-backend/smart-ai-service）
        File[] children = workDir.listFiles(File::isDirectory);
        if (children != null) {
            for (File child : children) {
                if (new File(child, "smart-ai-service").isDirectory()) {
                    File resolved = new File(child, path);
                    log.debug("路径解析(子目录): {} → {}", path, resolved.getAbsolutePath());
                    return resolved.getAbsolutePath();
                }
            }
        }

        // 向上遍历父目录
        File dir = workDir.getParentFile();
        for (int i = 0; i < 5 && dir != null; i++) {
            if (new File(dir, "smart-ai-service").isDirectory()) {
                File resolved = new File(dir, path);
                log.debug("路径解析(父目录): {} → {}", path, resolved.getAbsolutePath());
                return resolved.getAbsolutePath();
            }
            dir = dir.getParentFile();
        }

        // 兜底：返回工作目录下的绝对路径
        log.warn("未找到 smart-ai-service 模块目录，使用工作目录: {}", workDir);
        return new File(path).getAbsolutePath();
    }
}
