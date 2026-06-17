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
import java.util.List;

/**
 * 知识库服务
 * <p>
 * 应用启动时从数据库加载物种数据并向量化索引到 SimpleVectorStore。
 * 支持向量索引持久化：启动时优先从磁盘加载已有索引，关闭时自动保存。
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
     * 优先从磁盘加载已有向量索引，若不存在则从数据库加载并保存。
     */
    @PostConstruct
    public void initKnowledgeBase() {
        try {
            File storeFile = new File(aiProperties.getVectorStorePath());

            // 尝试从磁盘加载已有索引
            if (storeFile.exists() && vectorStore instanceof SimpleVectorStore simpleStore) {
                log.info("从磁盘加载向量索引: {}", storeFile.getAbsolutePath());
                simpleStore.load(storeFile);
                log.info("向量索引加载完成");
                return;
            }

            // 从数据库加载并构建索引
            log.info("开始初始化海洋知识库...");
            List<Document> documents = documentLoader.loadSpeciesDocuments();

            if (documents.isEmpty()) {
                log.warn("species 表为空，知识库未加载任何文档");
                return;
            }

            vectorStore.add(documents);
            log.info("知识库初始化完成，共加载 {} 个文档", documents.size());

            // 持久化向量索引到磁盘
            if (vectorStore instanceof SimpleVectorStore simpleStore) {
                storeFile.getParentFile().mkdirs();
                simpleStore.save(storeFile);
                log.info("向量索引已保存到: {}", storeFile.getAbsolutePath());
            }
        } catch (Exception e) {
            log.error("知识库初始化失败（RAG 功能将降级为裸模型）: {}", e.getMessage(), e);
        }
    }

    /**
     * 应用关闭时保存向量索引
     */
    @PreDestroy
    public void saveOnShutdown() {
        try {
            if (vectorStore instanceof SimpleVectorStore simpleStore) {
                File storeFile = new File(aiProperties.getVectorStorePath());
                storeFile.getParentFile().mkdirs();
                simpleStore.save(storeFile);
                log.info("向量索引已保存到磁盘（应用关闭时）: {}", storeFile.getAbsolutePath());
            }
        } catch (Exception e) {
            log.error("保存向量索引失败: {}", e.getMessage(), e);
        }
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
}
