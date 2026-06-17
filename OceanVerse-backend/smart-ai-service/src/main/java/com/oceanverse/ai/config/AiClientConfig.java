package com.oceanverse.ai.config;

import org.springframework.context.annotation.Configuration;

/**
 * AI 模块配置
 * <p>
 * DashScope Starter 自动配置了 ChatModel bean（基于 spring.ai.dashscope.* 配置）。
 * <p>
 * AiServiceImpl 直接注入 ChatModel，按请求类型构建不同的 ChatClient：
 * - 普通问答：使用默认的 qwen-plus 模型
 * - 图像识别：使用 DashScopeChatOptions 切换到 qwen-vl-max 视觉模型
 */
@Configuration
public class AiClientConfig {
    // ChatModel bean 由 spring-ai-alibaba-starter-dashscope 自动配置
    // AiServiceImpl 中按需构建 ChatClient（见 AiServiceImpl 中的 buildChatClient / buildVisionClient）
}
