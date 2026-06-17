package com.oceanverse.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 模块配置属性
 * <p>
 * 绑定 application.yml 中的 oceanverse.ai.* 配置段，
 * 集中管理模型名称、Token 限制、温度等可调参数。
 */
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
}
