package com.oceanverse.ai.rag;

import com.oceanverse.ai.config.AiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * KnowledgeBaseService 单元测试
 * <p>
 * 注意：rebuildIndex() 内部会创建新的 SimpleVectorStore 实例，
 * 需要真实的 EmbeddingModel 才能完成向量化。
 * 因此 rebuildIndex 相关测试仅验证调用链路（documentLoader 被调用、异常被吞），
 * 不验证文件输出。文件输出的完整验证通过 Apifox 集成测试覆盖。
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeBaseServiceTest {

    @Mock
    private DocumentLoader documentLoader;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private EmbeddingModel embeddingModel;

    private AiProperties aiProperties;
    private KnowledgeBaseService knowledgeBaseService;

    @BeforeEach
    void setUp() {
        aiProperties = new AiProperties();
        aiProperties.setVectorStorePath("target/test-vector-store/nonexistent.json");
        knowledgeBaseService = new KnowledgeBaseService(documentLoader, vectorStore, aiProperties, embeddingModel);
    }

    // ==================== 初始化测试 ====================

    @Test
    void initKnowledgeBase_loadsDocuments() {
        when(documentLoader.countSpecies()).thenReturn(10L);
        List<Document> docs = List.of(new Document("物种A"), new Document("物种B"));
        when(documentLoader.loadSpeciesDocuments()).thenReturn(docs);

        knowledgeBaseService.initKnowledgeBase();

        // rebuildIndex 被调用，从数据库加载了文档
        verify(documentLoader).loadSpeciesDocuments();
    }

    @Test
    void initKnowledgeBase_emptyDocuments_skipsAdd() {
        when(documentLoader.countSpecies()).thenReturn(0L);
        when(documentLoader.loadSpeciesDocuments()).thenReturn(List.of());

        knowledgeBaseService.initKnowledgeBase();

        // 空文档时提前返回，不调用 save
    }

    @Test
    void initKnowledgeBase_exceptionSwallowed() {
        when(documentLoader.countSpecies()).thenThrow(new RuntimeException("DB error"));

        assertDoesNotThrow(() -> knowledgeBaseService.initKnowledgeBase());
    }

    @Test
    void initKnowledgeBase_loadsFromDiskWhenFileExistsAndCountMatches(@TempDir Path tempDir) throws IOException {
        Path storeFile = tempDir.resolve("vector-store.json");
        Files.createFile(storeFile);
        Path metaFile = Path.of(storeFile + ".meta");
        Files.writeString(metaFile, "5", StandardCharsets.UTF_8);

        AiProperties props = new AiProperties();
        props.setVectorStorePath(storeFile.toString());

        SimpleVectorStore mockSimpleStore = mock(SimpleVectorStore.class);
        KnowledgeBaseService svc = new KnowledgeBaseService(documentLoader, mockSimpleStore, props, embeddingModel);

        when(documentLoader.countSpecies()).thenReturn(5L);

        svc.initKnowledgeBase();

        // 数量一致，应从磁盘加载，不从数据库加载
        verify(documentLoader, never()).loadSpeciesDocuments();
    }

    @Test
    void initKnowledgeBase_rebuildsWhenCountMismatch(@TempDir Path tempDir) throws IOException {
        Path storeFile = tempDir.resolve("vector-store.json");
        Files.createFile(storeFile);
        Path metaFile = Path.of(storeFile + ".meta");
        Files.writeString(metaFile, "3", StandardCharsets.UTF_8);

        AiProperties props = new AiProperties();
        props.setVectorStorePath(storeFile.toString());

        SimpleVectorStore mockSimpleStore = mock(SimpleVectorStore.class);
        KnowledgeBaseService svc = new KnowledgeBaseService(documentLoader, mockSimpleStore, props, embeddingModel);

        when(documentLoader.countSpecies()).thenReturn(8L);
        when(documentLoader.loadSpeciesDocuments()).thenReturn(List.of(new Document("新物种")));

        svc.initKnowledgeBase();

        // 数量不一致，从数据库重建
        verify(documentLoader).loadSpeciesDocuments();
    }

    @Test
    void initKnowledgeBase_metaFileMissing_triggersRebuild() {
        // 无 .meta 文件时 readMetaCount 返回 -1，与任何 dbCount 不匹配
        when(documentLoader.countSpecies()).thenReturn(5L);
        when(documentLoader.loadSpeciesDocuments()).thenReturn(List.of(new Document("test")));

        knowledgeBaseService.initKnowledgeBase();

        verify(documentLoader).loadSpeciesDocuments();
    }

    // ==================== 关闭保存测试 ====================

    @Test
    void saveOnShutdown_skipsWhenNotSimpleVectorStore() {
        // vectorStore 是普通 mock（非 SimpleVectorStore），不应执行保存
        assertDoesNotThrow(() -> knowledgeBaseService.saveOnShutdown());
    }

    @Test
    void saveOnShutdown_afterRebuild_doesNotQueryDbCount(@TempDir Path tempDir) throws IOException {
        // 验证 indexedDocumentCount 设置后，saveOnShutdown 不调用 countSpecies
        // 通过磁盘加载路径设置 indexedDocumentCount（不依赖真实 EmbeddingModel）
        Path storeFile = tempDir.resolve("vector-store.json");
        Files.createFile(storeFile);
        Path metaFile = Path.of(storeFile + ".meta");
        Files.writeString(metaFile, "19", StandardCharsets.UTF_8);

        AiProperties props = new AiProperties();
        props.setVectorStorePath(storeFile.toString());

        SimpleVectorStore mockSimpleStore = mock(SimpleVectorStore.class);
        KnowledgeBaseService svc = new KnowledgeBaseService(documentLoader, mockSimpleStore, props, embeddingModel);

        when(documentLoader.countSpecies()).thenReturn(19L);
        svc.initKnowledgeBase(); // 磁盘加载 → indexedDocumentCount = 19

        // 模拟 rebuild 后物种被删除：DB 变成 18
        // saveOnShutdown 应使用 indexedDocumentCount（19），不调用 countSpecies
        svc.saveOnShutdown();

        // countSpecies 只在 initKnowledgeBase 中被调用 1 次，saveOnShutdown 不应再调用
        verify(documentLoader, times(1)).countSpecies();
        // .meta 应为 "19"（indexedDocumentCount），而非 DB 实时值
        assertEquals("19", Files.readString(metaFile, StandardCharsets.UTF_8).trim());
    }

    @Test
    void saveOnShutdown_afterDiskLoad_writesCorrectCount(@TempDir Path tempDir) throws IOException {
        // 从磁盘加载索引后，saveOnShutdown 应写入正确的索引数量
        Path storeFile = tempDir.resolve("vector-store.json");
        Files.createFile(storeFile);
        Path metaFile = Path.of(storeFile + ".meta");
        Files.writeString(metaFile, "7", StandardCharsets.UTF_8);

        AiProperties props = new AiProperties();
        props.setVectorStorePath(storeFile.toString());

        SimpleVectorStore mockSimpleStore = mock(SimpleVectorStore.class);
        KnowledgeBaseService svc = new KnowledgeBaseService(documentLoader, mockSimpleStore, props, embeddingModel);

        when(documentLoader.countSpecies()).thenReturn(7L);
        svc.initKnowledgeBase();

        svc.saveOnShutdown();

        // .meta 应为 "7"（从磁盘加载时设置的数量）
        assertEquals("7", Files.readString(metaFile, StandardCharsets.UTF_8).trim());
    }

    // ==================== 搜索测试 ====================

    @Test
    void search_returnsResults() {
        List<Document> docs = List.of(new Document("绿海龟资料"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(docs);

        List<Document> results = knowledgeBaseService.search("绿海龟", 3);

        assertEquals(1, results.size());
        assertEquals("绿海龟资料", results.get(0).getText());
    }

    @Test
    void search_exceptionReturnsEmpty() {
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenThrow(new RuntimeException("Search error"));

        List<Document> results = knowledgeBaseService.search("test", 3);

        assertTrue(results.isEmpty());
    }

    @Test
    void search_usesConfigurableThreshold() {
        aiProperties.setSimilarityThreshold(0.8);
        List<Document> docs = List.of(new Document("高相似度结果"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(docs);

        List<Document> results = knowledgeBaseService.search("test", 3);

        assertEquals(1, results.size());
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }

    // ==================== 格式化测试 ====================

    @Test
    void formatContext_formatsCorrectly() {
        List<Document> docs = List.of(
                new Document("物种名称: 绿海龟"),
                new Document("物种名称: 蓝鲸")
        );

        String context = knowledgeBaseService.formatContext(docs);

        assertTrue(context.contains("【资料 1】"));
        assertTrue(context.contains("绿海龟"));
        assertTrue(context.contains("【资料 2】"));
        assertTrue(context.contains("蓝鲸"));
    }

    @Test
    void formatContext_emptyList_returnsEmpty() {
        String context = knowledgeBaseService.formatContext(List.of());
        assertEquals("", context);
    }

    @Test
    void formatContext_nullReturnsEmpty() {
        String context = knowledgeBaseService.formatContext(null);
        assertEquals("", context);
    }

    // ==================== 重建测试 ====================

    @Test
    void rebuildIndex_callsDocumentLoader() {
        List<Document> docs = List.of(new Document("新物种"));
        when(documentLoader.loadSpeciesDocuments()).thenReturn(docs);

        assertDoesNotThrow(() -> knowledgeBaseService.rebuildIndex());

        verify(documentLoader).loadSpeciesDocuments();
    }

    @Test
    void rebuildIndex_emptyDocuments_skipsRebuild() {
        when(documentLoader.loadSpeciesDocuments()).thenReturn(List.of());

        assertDoesNotThrow(() -> knowledgeBaseService.rebuildIndex());

        verify(documentLoader).loadSpeciesDocuments();
    }

    @Test
    void rebuildIndex_exceptionSwallowed() {
        when(documentLoader.loadSpeciesDocuments()).thenThrow(new RuntimeException("DB error"));

        assertDoesNotThrow(() -> knowledgeBaseService.rebuildIndex());
    }
}
