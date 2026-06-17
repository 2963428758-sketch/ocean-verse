package com.oceanverse.ai.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 向量存储配置
 * <p>
 * 使用 Spring AI 内置的 SimpleVectorStore（内存向量存储），
 * 无需额外基础设施。EmbeddingModel 由 DashScope Starter 自动配置（text-embedding-v3）。
 * <p>
 * 后续如需持久化向量存储，可切换为 RedisVectorStore（需 Redis Stack + RediSearch）。
 */
@Configuration
public class VectorStoreConfig {

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
