package com.oceanverse.ai.prompt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PromptTemplateManager 单元测试
 */
class PromptTemplateManagerTest {

    private PromptTemplateManager manager;

    @BeforeEach
    void setUp() {
        manager = new PromptTemplateManager();
    }

    @Test
    void getSystemPrompt_SPECIES_returnsSpeciesPrompt() {
        String prompt = manager.getSystemPrompt("SPECIES");
        assertNotNull(prompt);
        assertTrue(prompt.contains("分类学专家"), "SPECIES prompt should mention taxonomy expertise");
        assertTrue(prompt.contains("物种名称"), "SPECIES prompt should require species name");
    }

    @Test
    void getSystemPrompt_ECOLOGY_returnsEcologyPrompt() {
        String prompt = manager.getSystemPrompt("ECOLOGY");
        assertNotNull(prompt);
        assertTrue(prompt.contains("生态学研究员"), "ECOLOGY prompt should mention ecology research");
    }

    @Test
    void getSystemPrompt_CONSERVATION_returnsConservationPrompt() {
        String prompt = manager.getSystemPrompt("CONSERVATION");
        assertNotNull(prompt);
        assertTrue(prompt.contains("保护政策顾问"), "CONSERVATION prompt should mention conservation policy");
        assertTrue(prompt.contains("IUCN"), "CONSERVATION prompt should reference IUCN");
    }

    @Test
    void getSystemPrompt_GENERAL_returnsGeneralPrompt() {
        String prompt = manager.getSystemPrompt("GENERAL");
        assertNotNull(prompt);
        assertTrue(prompt.contains("科普作家"), "GENERAL prompt should mention science writer");
    }

    @Test
    void getSystemPrompt_null_returnsGeneralPrompt() {
        String prompt = manager.getSystemPrompt(null);
        assertEquals(manager.getSystemPrompt("GENERAL"), prompt);
    }

    @Test
    void getSystemPrompt_blank_returnsGeneralPrompt() {
        String prompt = manager.getSystemPrompt("");
        assertEquals(manager.getSystemPrompt("GENERAL"), prompt);

        prompt = manager.getSystemPrompt("   ");
        assertEquals(manager.getSystemPrompt("GENERAL"), prompt);
    }

    @Test
    void getSystemPrompt_unknownType_returnsGeneralPrompt() {
        String prompt = manager.getSystemPrompt("UNKNOWN_TYPE");
        assertEquals(manager.getSystemPrompt("GENERAL"), prompt);
    }

    @Test
    void getSystemPrompt_caseInsensitive() {
        String upper = manager.getSystemPrompt("SPECIES");
        String lower = manager.getSystemPrompt("species");
        String mixed = manager.getSystemPrompt("Species");
        assertEquals(upper, lower);
        assertEquals(upper, mixed);
    }
}
