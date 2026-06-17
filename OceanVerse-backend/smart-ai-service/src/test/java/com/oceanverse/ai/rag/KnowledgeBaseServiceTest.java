package com.oceanverse.ai.rag;

import com.oceanverse.ai.config.AiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
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
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeBaseServiceTest {

    @Mock
    private DocumentLoader documentLoader;

    @Mock
    private VectorStore vectorStore;

    private AiProperties aiProperties;
    private KnowledgeBaseService knowledgeBaseService;

    @BeforeEach
    void setUp() {
        aiProperties = new AiProperties();
        // 默认使用不存在的路径，跳过磁盘加载，走 DB 初始化路径
        aiProperties.setVectorStorePath("target/test-vector-store/nonexistent.json");
        knowledgeBaseService = new KnowledgeBaseService(documentLoader, vectorStore, aiProperties);
    }

    @Test
    void initKnowledgeBase_loadsDocuments() {
        when(documentLoader.countSpecies()).thenReturn(10L);
        List<Document> docs = List.of(new Document("物种A"), new Document("物种B"));
        when(documentLoader.loadSpeciesDocuments()).thenReturn(docs);

        knowledgeBaseService.initKnowledgeBase();

        verify(vectorStore).add(docs);
    }

    @Test
    void initKnowledgeBase_emptyDocuments_skipsAdd() {
        when(documentLoader.countSpecies()).thenReturn(0L);
        when(documentLoader.loadSpeciesDocuments()).thenReturn(List.of());

        knowledgeBaseService.initKnowledgeBase();

        verify(vectorStore, never()).add(any());
    }

    @Test
    void initKnowledgeBase_exceptionSwallowed() {
        when(documentLoader.countSpecies()).thenThrow(new RuntimeException("DB error"));

        assertDoesNotThrow(() -> knowledgeBaseService.initKnowledgeBase());
    }

    @Test
    void initKnowledgeBase_loadsFromDiskWhenFileExistsAndCountMatches(@TempDir Path tempDir) throws IOException {
        // 创建临时向量索引文件
        Path storeFile = tempDir.resolve("vector-store.json");
        Files.createFile(storeFile);

        // 创建元数据文件，物种数量为 5
        Path metaFile = Path.of(storeFile + ".meta");
        Files.writeString(metaFile, "5", StandardCharsets.UTF_8);

        AiProperties props = new AiProperties();
        props.setVectorStorePath(storeFile.toString());

        SimpleVectorStore mockSimpleStore = mock(SimpleVectorStore.class);
        KnowledgeBaseService svc = new KnowledgeBaseService(documentLoader, mockSimpleStore, props);

        // DB 中也是 5 条，数量一致
        when(documentLoader.countSpecies()).thenReturn(5L);

        svc.initKnowledgeBase();

        // 应该从磁盘加载，而不是从数据库加载
        // 注意：不直接 verify load(File) 因为 Mockito 对 SimpleVectorStore 的重载方法有歧义
        verify(documentLoader, never()).loadSpeciesDocuments();
    }

    @Test
    void initKnowledgeBase_rebuildsWhenCountMismatch(@TempDir Path tempDir) throws IOException {
        // 创建旧的向量索引文件和元数据（物种数量=3）
        Path storeFile = tempDir.resolve("vector-store.json");
        Files.createFile(storeFile);
        Path metaFile = Path.of(storeFile + ".meta");
        Files.writeString(metaFile, "3", StandardCharsets.UTF_8);

        AiProperties props = new AiProperties();
        props.setVectorStorePath(storeFile.toString());

        SimpleVectorStore mockSimpleStore = mock(SimpleVectorStore.class);
        KnowledgeBaseService svc = new KnowledgeBaseService(documentLoader, mockSimpleStore, props);

        // DB 中现在是 8 条，数量不一致
        when(documentLoader.countSpecies()).thenReturn(8L);
        List<Document> docs = List.of(new Document("新物种"));
        when(documentLoader.loadSpeciesDocuments()).thenReturn(docs);

        svc.initKnowledgeBase();

        // 应该从数据库重建（因为数量不匹配），不从磁盘加载
        // 验证 add 被调用证明走了 DB 加载路径
        verify(mockSimpleStore).add(docs);
        verify(mockSimpleStore).save(storeFile.toFile());
    }

    @Test
    void initKnowledgeBase_savesToDiskAfterDBLoad(@TempDir Path tempDir) {
        Path storeFile = tempDir.resolve("vector-store.json");

        AiProperties props = new AiProperties();
        props.setVectorStorePath(storeFile.toString());

        SimpleVectorStore mockSimpleStore = mock(SimpleVectorStore.class);
        KnowledgeBaseService svc = new KnowledgeBaseService(documentLoader, mockSimpleStore, props);

        when(documentLoader.countSpecies()).thenReturn(2L);
        List<Document> docs = List.of(new Document("物种A"));
        when(documentLoader.loadSpeciesDocuments()).thenReturn(docs);

        svc.initKnowledgeBase();

        verify(mockSimpleStore).add(docs);
        verify(mockSimpleStore).save(storeFile.toFile());
    }

    @Test
    void saveOnShutdown_savesWhenSimpleVectorStore(@TempDir Path tempDir) {
        AiProperties props = new AiProperties();
        props.setVectorStorePath(tempDir.resolve("vector-store.json").toString());

        SimpleVectorStore mockSimpleStore = mock(SimpleVectorStore.class);
        KnowledgeBaseService svc = new KnowledgeBaseService(documentLoader, mockSimpleStore, props);

        when(documentLoader.countSpecies()).thenReturn(5L);
        svc.saveOnShutdown();

        verify(mockSimpleStore).save(any(File.class));
    }

    @Test
    void saveOnShutdown_skipsWhenNotSimpleVectorStore() {
        // vectorStore 是普通 mock（非 SimpleVectorStore），不应执行保存
        assertDoesNotThrow(() -> knowledgeBaseService.saveOnShutdown());
    }

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

    @Test
    void rebuildIndex_reloadsFromDB(@TempDir Path tempDir) {
        Path storeFile = tempDir.resolve("vector-store.json");

        AiProperties props = new AiProperties();
        props.setVectorStorePath(storeFile.toString());

        SimpleVectorStore mockSimpleStore = mock(SimpleVectorStore.class);
        KnowledgeBaseService svc = new KnowledgeBaseService(documentLoader, mockSimpleStore, props);

        when(documentLoader.countSpecies()).thenReturn(3L);
        List<Document> docs = List.of(new Document("新物种"));
        when(documentLoader.loadSpeciesDocuments()).thenReturn(docs);

        svc.rebuildIndex();

        verify(mockSimpleStore).add(docs);
        verify(mockSimpleStore).save(storeFile.toFile());
    }
}
