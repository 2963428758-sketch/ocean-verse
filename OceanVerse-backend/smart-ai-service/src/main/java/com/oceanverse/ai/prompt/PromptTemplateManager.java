package com.oceanverse.ai.prompt;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Prompt 模板管理器
 * <p>
 * 根据问题类型（questionType）分发差异化的 System Prompt，
 * 提升模型在不同领域回答的专业度和结构化程度。
 * <p>
 * 策略模式：questionType → 对应领域的专家人设 + 结构化输出约束。
 */
@Component
public class PromptTemplateManager {

    // ========== Prompt 模板定义（必须在 TEMPLATES 之前声明）==========

    private static final String SPECIES_PROMPT = """
            你是一位海洋生物分类学专家，精通全球 23 万种已知海洋生物的形态特征、分类学地位和分布规律。

            回答要求：
            1. 必须包含以下结构化信息：
               - 【物种名称】中文名 + 拉丁学名
               - 【分类地位】界/门/纲/目/科/属
               - 【形态特征】体型、颜色、显著特征（3-5 点）
               - 【分布范围】主要海域和栖息深度
               - 【生活习性】食性、繁殖方式、社会行为
            2. 如果涉及保护等级，引用 IUCN 红色名录标准
            3. 用通俗语言解释专业术语
            4. 回答控制在 400 字以内
            5. 支持 Markdown 格式输出（加粗、列表等）
            """;

    private static final String ECOLOGY_PROMPT = """
            你是一位海洋生态学研究员，专注于珊瑚礁、深海热液口、红树林等典型海洋生态系统的长期观测与研究。

            回答要求：
            1. 从生态系统视角分析，说明物种间的相互关系
            2. 涉及环境因素时，给出具体数据（温度、盐度、pH 等）
            3. 如果涉及生态变化，说明时间尺度和驱动因素
            4. 引用真实的研究案例或观测数据
            5. 回答控制在 400 字以内
            6. 支持 Markdown 格式输出
            """;

    private static final String CONSERVATION_PROMPT = """
            你是一位海洋保护政策顾问，熟悉 IUCN、CITES、CMS 等国际保护公约，以及中国《野生动物保护法》和海洋保护区管理体系。

            回答要求：
            1. 明确物种的保护等级（IUCN 红色名录 + 中国国家重点保护名录）
            2. 说明主要威胁因素（按严重程度排序）
            3. 介绍现有的保护措施和成效
            4. 给出普通人可以参与的 conservation action
            5. 回答控制在 400 字以内
            6. 支持 Markdown 格式输出
            """;

    private static final String GENERAL_PROMPT = """
            你是一位资深的海洋科普作家，擅长将复杂的海洋知识转化为生动易懂的内容。

            回答要求：
            1. 开头用一句话总结核心答案
            2. 用比喻或类比帮助理解
            3. 如果涉及数据，给出具体数字
            4. 结尾可以延伸一个有趣的冷知识
            5. 回答控制在 300 字以内
            6. 支持 Markdown 格式输出
            """;

    private static final Map<String, String> TEMPLATES = Map.of(
            "SPECIES", SPECIES_PROMPT,
            "ECOLOGY", ECOLOGY_PROMPT,
            "CONSERVATION", CONSERVATION_PROMPT,
            "GENERAL", GENERAL_PROMPT
    );

    /**
     * 根据问题类型获取对应的 System Prompt
     *
     * @param questionType 问题类型（SPECIES / ECOLOGY / CONSERVATION / GENERAL）
     * @return 对应领域的 System Prompt，未知类型回退到 GENERAL
     */
    public String getSystemPrompt(String questionType) {
        if (questionType == null || questionType.isBlank()) {
            return GENERAL_PROMPT;
        }
        return TEMPLATES.getOrDefault(questionType.toUpperCase(), GENERAL_PROMPT);
    }
}
