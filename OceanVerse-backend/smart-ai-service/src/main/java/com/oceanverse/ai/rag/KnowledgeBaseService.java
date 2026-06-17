package com.oceanverse.ai.rag;

import com.oceanverse.ai.config.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 知识库服务
 * <p>
 * 应用启动时从数据库加载物种数据并向量化索引到 SimpleVectorStore。
 * 支持向量索引持久化：启动时优先从磁盘加载已有索引，关闭时自动保存。
 * 启动时通过物种数量比对检测数据库变更，自动重建索引。
 * 问答时根据用户问题进行语义检索，返回最相关的文档片段作为 RAG 上下文。
 * <p>
 * 技术栈：text-embedding-v3（向量化）+ SimpleVectorStore（内存向量存储 + 余弦相似度检索）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final DocumentLoader documentLoader;
    private final VectorStore vectorStore;
    private final AiProperties aiProperties;

    /**
     * 应用启动时初始化知识库
     * <p>
     * 优先从磁盘加载已有向量索引（需物种数量未变），否则从数据库加载并保存。
     */
    @PostConstruct
    public void initKnowledgeBase() {
        try {
            File storeFile = new File(aiProperties.getVectorStorePath());
            File metaFile = metaFile(storeFile);
            long dbCount = documentLoader.countSpecies();

            // 尝试从磁盘加载已有索引（需数量一致）
            if (storeFile.exists() && vectorStore instanceof SimpleVectorStore simpleStore
                    && metaFile.exists() && readMetaCount(metaFile) == dbCount) {
                log.info("从磁盘加载向量索引: {}（物种数: {}）", storeFile.getAbsolutePath(), dbCount);
                simpleStore.load(storeFile);
                log.info("向量索引加载完成");
                return;
            }

            // 数量不一致或无索引文件，从数据库重建
            if (storeFile.exists() && metaFile.exists() && readMetaCount(metaFile) != dbCount) {
                log.info("物种数量已变更（缓存: {}, 数据库: {}），重建向量索引", readMetaCount(metaFile), dbCount);
            }

            rebuildIndex();
        } catch (Exception e) {
            log.error("知识库初始化失败（RAG 功能将降级为裸模型）: {}", e.getMessage(), e);
        }
    }

    /**
     * 手动重建索引：从数据库重新加载并向量化，保存到磁盘。
     * 可在管理接口或物种数据变更后调用。
     */
    public void rebuildIndex() {
        try {
            log.info("开始初始化海洋知识库...");

            // 重建前清空内存中的旧数据（SimpleVectorStore 不支持 remove，需重建实例）
            // 由于 Spring 管理的 bean 无法替换，这里通过重新加载覆盖
            List<Document> documents = documentLoader.loadSpeciesDocuments();

            if (documents.isEmpty()) {
                log.warn("species 表为空，知识库未加载任何文档");
                return;
            }

            vectorStore.add(documents);
            log.info("知识库初始化完成，共加载 {} 个文档", documents.size());

            // 持久化向量索引和元数据到磁盘
            saveToDisk();
        } catch (Exception e) {
            log.error("知识库重建失败（RAG 功能将降级为裸模型）: {}", e.getMessage(), e);
        }
    }

    /**
     * 应用关闭时保存向量索引
     */
    @PreDestroy
    public void saveOnShutdown() {
        saveToDisk();
    }

    /**
     * 根据用户问题检索相关知识
     *
     * @param query 用户问题
     * @param topK  返回最相关的 K 个文档
     * @return 相关文档列表
     */
    public List<Document> search(String query, int topK) {
        try {
            return vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(query)
                            .topK(topK)
                            .similarityThreshold(aiProperties.getSimilarityThreshold())
                            .build()
            );
        } catch (Exception e) {
            log.warn("知识库检索失败，降级为裸模型: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 将检索结果格式化为上下文文本，供注入 System Prompt
     * <p>
     * 格式：
     * 【资料 1】
     * 物种名称: 绿海龟
     * ...
     * <p>
     * 【资料 2】
     * ...
     */
    public String formatContext(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder("以下是从知识库中检索到的参考资料：\n\n");
        for (int i = 0; i < documents.size(); i++) {
            sb.append(String.format("【资料 %d】\n%s\n\n", i + 1, documents.get(i).getText()));
        }
        return sb.toString();
    }

    // ==================== 内部方法 ====================

    private void saveToDisk() {
        try {
            if (vectorStore instanceof SimpleVectorStore simpleStore) {
                File storeFile = new File(aiProperties.getVectorStorePath());
                storeFile.getParentFile().mkdirs();
                simpleStore.save(storeFile);
                log.info("向量索引已保存到磁盘: {}", storeFile.getAbsolutePath());

                // 同时保存物种数量元数据
                File metaFile = metaFile(storeFile);
                long dbCount = documentLoader.countSpecies();
                Files.writeString(metaFile.toPath(), String.valueOf(dbCount), StandardCharsets.UTF_8);
                log.debug("物种数量元数据已保存: count={}", dbCount);
            }
        } catch (Exception e) {
            log.error("保存向量索引失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 元数据文件路径（与向量索引文件同目录，后缀 .meta）
     */
    private File metaFile(File storeFile) {
        return new File(storeFile.getAbsolutePath() + ".meta");
    }

    /**
     * 读取元数据中记录的物种数量
     */
    private long readMetaCount(File metaFile) {
        try {
            String content = Files.readString(metaFile.toPath(), StandardCharsets.UTF_8).trim();
            return Long.parseLong(content);
        } catch (IOException | NumberFormatException e) {
            log.warn("元数据文件读取失败，将重建索引: {}", e.getMessage());
            return -1; // 返回 -1 确保与任何 dbCount 都不匹配，触发重建
        }
    }
}
