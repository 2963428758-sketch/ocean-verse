package com.oceanverse.ai.rag;

import com.oceanverse.pojo.entity.Species;
import com.oceanverse.species.mapper.SpeciesMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识文档加载器
 * <p>
 * 从 species 表加载所有物种数据，转为 Spring AI Document 列表，
 * 供 KnowledgeBaseService 进行向量化索引。
 * <p>
 * 每个物种生成一个 Document，包含分类信息、形态描述、生态特征和保护状态。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentLoader {

    private final SpeciesMapper speciesMapper;

    /**
     * 从 species 表加载所有物种数据，转为 Document 列表
     */
    public List<Document> loadSpeciesDocuments() {
        List<Species> speciesList = speciesMapper.selectList(null);

        if (speciesList == null || speciesList.isEmpty()) {
            log.warn("species 表为空，知识库未加载任何文档");
            return List.of();
        }

        log.info("从数据库加载了 {} 条物种记录", speciesList.size());

        return speciesList.stream()
                .map(this::toDocument)
                .collect(Collectors.toList());
    }

    /**
     * 将 Species 实体转为 Document
     * <p>
     * 内容格式化的目标是让向量检索时语义匹配度更高：
     * - 包含中文名、学名、分类信息便于物种相关查询命中
     * - 包含形态、生态描述便于生态相关查询命中
     * - 包含保护状态便于保护相关查询命中
     */
    private Document toDocument(Species species) {
        StringBuilder content = new StringBuilder();

        // 基本名称信息
        content.append("物种名称: ").append(nullSafe(species.getChineseName()));
        if (species.getCommonName() != null && !species.getCommonName().equals(species.getChineseName())) {
            content.append(" (").append(species.getCommonName()).append(")");
        }
        content.append("\n");

        content.append("学名: ").append(nullSafe(species.getScientificName())).append("\n");

        // 分类信息
        content.append("分类: ")
                .append(nullSafe(species.getKingdom())).append(" / ")
                .append(nullSafe(species.getPhylum())).append(" / ")
                .append(nullSafe(species.getClassName())).append(" / ")
                .append(nullSafe(species.getOrderName())).append(" / ")
                .append(nullSafe(species.getFamily())).append(" / ")
                .append(nullSafe(species.getGenus()))
                .append("\n");

        // 形态特征
        if (species.getMorphology() != null && !species.getMorphology().isBlank()) {
            content.append("形态特征: ").append(species.getMorphology()).append("\n");
        }

        // 生态信息
        if (species.getEcology() != null && !species.getEcology().isBlank()) {
            content.append("生态信息: ").append(species.getEcology()).append("\n");
        }

        // 描述
        if (species.getDescription() != null && !species.getDescription().isBlank()) {
            content.append("描述: ").append(species.getDescription()).append("\n");
        }

        // 保护状态
        if (species.getIucnStatus() != null && !species.getIucnStatus().isBlank()) {
            content.append("IUCN保护等级: ").append(species.getIucnStatus()).append("\n");
        }
        if (species.getProtectionLevel() != null && !species.getProtectionLevel().isBlank()) {
            content.append("中国保护等级: ").append(species.getProtectionLevel()).append("\n");
        }

        return new Document(content.toString().trim());
    }

    /**
     * 返回 species 表中的记录总数，用于增量更新检测
     */
    public long countSpecies() {
        Long count = speciesMapper.selectCount(null);
        return count != null ? count : 0L;
    }

    private String nullSafe(String value) {
        return value != null ? value : "未知";
    }
}
