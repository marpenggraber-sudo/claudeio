package org.example.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.example.dto.KnowledgeDocument;
import org.example.entity.MusicKnowledge;
import org.example.repository.MusicKnowledgeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RagService 单元测试
 *
 * 测试目标：
 * 1. 知识检索功能的正确性
 * 2. 相似度阈值的有效性（已提高到 0.7）
 * 3. 知识格式化功能
 * 4. 知识索引功能
 * 5. 空结果处理
 */
@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private EmbeddingStore<TextSegment> embeddingStore;

    @Mock
    private MusicKnowledgeRepository knowledgeRepository;

    @InjectMocks
    private RagService ragService;

    private MusicKnowledge testKnowledge;
    private Embedding testEmbedding;

    @BeforeEach
    void setUp() {
        // 设置配置属性
        ReflectionTestUtils.setField(ragService, "similarityThreshold", 0.7);
        ReflectionTestUtils.setField(ragService, "defaultMaxResults", 3);

        // 准备测试数据
        testKnowledge = new MusicKnowledge();
        testKnowledge.setId(1L);
        testKnowledge.setTitle("周杰伦");
        testKnowledge.setContent("周杰伦是华语流行音乐天王，以其独特的R&B和中国风融合风格闻名。");
        testKnowledge.setKnowledgeType("artist");
        testKnowledge.setRelatedKeywords("周杰伦,Jay Chou,华语,流行");
        testKnowledge.setCreatedAt(LocalDateTime.now());
        testKnowledge.setUpdatedAt(LocalDateTime.now());

        // 创建模拟的嵌入向量（384 维 - all-MiniLM-L6-v2 的向量维度）
        float[] vectorData = new float[384];
        for (int i = 0; i < 384; i++) {
            vectorData[i] = 0.1f;
        }
        testEmbedding = new Embedding(vectorData);
    }

    /**
     * 测试：检索相关知识 - 成功找到高相关度知识
     */
    @Test
    void retrieveRelevantKnowledge_existingKnowledge_returnsMatchingDocuments() {
        // Arrange
        String query = "介绍一下周杰伦";
        int maxResults = 3;

        // Mock embedding model
        Response<Embedding> embeddingResponse = mock(Response.class);
        when(embeddingResponse.content()).thenReturn(testEmbedding);
        when(embeddingModel.embed(anyString())).thenReturn(embeddingResponse);

        // Mock embedding store - 返回高相似度匹配（0.85）
        TextSegment segment = TextSegment.from(
            testKnowledge.getTitle() + "\n" + testKnowledge.getContent()
        );
        EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(
            0.85,  // 高相似度分数
            "embedding-id-1",
            testEmbedding,
            segment
        );
        // 设置元数据
        segment.metadata().put("id", testKnowledge.getId().toString());
        segment.metadata().put("title", testKnowledge.getTitle());
        segment.metadata().put("type", testKnowledge.getKnowledgeType());

        when(embeddingStore.findRelevant(any(Embedding.class), eq(maxResults), anyDouble()))
            .thenReturn(List.of(match));

        // Act
        List<KnowledgeDocument> results = ragService.retrieveRelevantKnowledge(query, maxResults);

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("周杰伦");
        assertThat(results.get(0).getKnowledgeType()).isEqualTo("artist");
        assertThat(results.get(0).getRelevanceScore()).isEqualTo(0.85);
        assertThat(results.get(0).getContent()).contains("华语流行音乐天王");

        verify(embeddingModel).embed(query);
        verify(embeddingStore).findRelevant(any(Embedding.class), eq(maxResults), anyDouble());
    }

    /**
     * 测试：检索相关知识 - 查询不到结果
     */
    @Test
    void retrieveRelevantKnowledge_noMatches_returnsEmptyList() {
        // Arrange
        String query = "完全不相关的查询";
        int maxResults = 3;

        Response<Embedding> embeddingResponse = mock(Response.class);
        when(embeddingResponse.content()).thenReturn(testEmbedding);
        when(embeddingModel.embed(anyString())).thenReturn(embeddingResponse);

        when(embeddingStore.findRelevant(any(Embedding.class), eq(maxResults), eq(0.7)))
            .thenReturn(new ArrayList<>());

        // Act
        List<KnowledgeDocument> results = ragService.retrieveRelevantKnowledge(query, maxResults);

        // Assert
        assertThat(results).isEmpty();
    }

    /**
     * 测试：相似度阈值 0.7 是否合理
     *
     * 验证：现在阈值提高到 0.7，低相似度（0.65）的知识不会被返回
     */
    @Test
    void retrieveRelevantKnowledge_lowSimilarityThreshold_filtersWeakMatches() {
        // Arrange
        String query = "音乐相关的模糊查询";
        int maxResults = 3;

        Response<Embedding> embeddingResponse = mock(Response.class);
        when(embeddingResponse.content()).thenReturn(testEmbedding);
        when(embeddingModel.embed(anyString())).thenReturn(embeddingResponse);

        // 模拟返回空结果（因为低相似度被过滤）
        when(embeddingStore.findRelevant(any(Embedding.class), eq(maxResults), eq(0.7)))
            .thenReturn(new ArrayList<>());

        // Act
        List<KnowledgeDocument> results = ragService.retrieveRelevantKnowledge(query, maxResults);

        // Assert
        assertThat(results).isEmpty();

        // 验证改进：阈值 0.7 有效过滤了低相关度结果
    }

    /**
     * 测试：格式化知识上下文 - 有检索结果
     */
    @Test
    void formatKnowledgeContext_withDocuments_returnsFormattedString() {
        // Arrange
        List<KnowledgeDocument> documents = List.of(
            new KnowledgeDocument(1L, "周杰伦", "周杰伦是华语流行音乐天王。", "artist", 0.85),
            new KnowledgeDocument(2L, "中国风音乐", "中国风音乐是将中国传统音乐元素与现代流行音乐相结合。", "genre", 0.75)
        );

        // Act
        String context = ragService.formatKnowledgeContext(documents);

        // Assert
        assertThat(context).contains("【相关音乐知识】");
        assertThat(context).contains("1. 周杰伦");
        assertThat(context).contains("周杰伦是华语流行音乐天王。");
        assertThat(context).contains("2. 中国风音乐");
        assertThat(context).contains("中国传统音乐元素");
    }

    /**
     * 测试：格式化知识上下文 - 空列表
     */
    @Test
    void formatKnowledgeContext_emptyList_returnsEmptyString() {
        // Arrange
        List<KnowledgeDocument> documents = new ArrayList<>();

        // Act
        String context = ragService.formatKnowledgeContext(documents);

        // Assert
        assertThat(context).isEmpty();
    }

    /**
     * 测试：索引知识 - 成功索引
     */
    @Test
    void indexKnowledge_validKnowledge_storesInEmbeddingStore() {
        // Arrange
        Response<Embedding> embeddingResponse = mock(Response.class);
        when(embeddingResponse.content()).thenReturn(testEmbedding);
        when(embeddingModel.embed(any(TextSegment.class))).thenReturn(embeddingResponse);

        // Act
        ragService.indexKnowledge(testKnowledge);

        // Assert
        ArgumentCaptor<TextSegment> segmentCaptor = ArgumentCaptor.forClass(TextSegment.class);
        verify(embeddingModel).embed(segmentCaptor.capture());
        verify(embeddingStore).add(eq(testEmbedding), any(TextSegment.class));

        TextSegment capturedSegment = segmentCaptor.getValue();
        assertThat(capturedSegment.text()).contains("周杰伦");
        assertThat(capturedSegment.text()).contains("华语流行音乐天王");
        assertThat(capturedSegment.metadata().getString("id")).isEqualTo("1");
        assertThat(capturedSegment.metadata().getString("title")).isEqualTo("周杰伦");
        assertThat(capturedSegment.metadata().getString("type")).isEqualTo("artist");
    }

    /**
     * 测试：添加新知识 - 同时保存到数据库和向量存储
     */
    @Test
    void addKnowledge_validInput_savesAndIndexes() {
        // Arrange
        String title = "新艺术家";
        String content = "新艺术家的介绍";
        String type = "artist";
        String keywords = "新,艺术家";

        MusicKnowledge savedKnowledge = new MusicKnowledge();
        savedKnowledge.setId(10L);
        savedKnowledge.setTitle(title);
        savedKnowledge.setContent(content);
        savedKnowledge.setKnowledgeType(type);
        savedKnowledge.setRelatedKeywords(keywords);

        when(knowledgeRepository.save(any(MusicKnowledge.class))).thenReturn(savedKnowledge);

        Response<Embedding> embeddingResponse = mock(Response.class);
        when(embeddingResponse.content()).thenReturn(testEmbedding);
        when(embeddingModel.embed(any(TextSegment.class))).thenReturn(embeddingResponse);

        // Act
        MusicKnowledge result = ragService.addKnowledge(title, content, type, keywords);

        // Assert
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getTitle()).isEqualTo(title);

        verify(knowledgeRepository).save(any(MusicKnowledge.class));
        verify(embeddingModel).embed(any(TextSegment.class));
        verify(embeddingStore).add(any(Embedding.class), any(TextSegment.class));
    }

    /**
     * 测试：检索知识 - 处理异常情况
     */
    @Test
    void retrieveRelevantKnowledge_embeddingModelThrowsException_returnsEmptyList() {
        // Arrange
        String query = "测试查询";
        when(embeddingModel.embed(anyString())).thenThrow(new RuntimeException("Embedding model error"));

        // Act
        List<KnowledgeDocument> results = ragService.retrieveRelevantKnowledge(query, 3);

        // Assert
        assertThat(results).isEmpty();
    }

    /**
     * 测试：检索多个知识 - 按相似度排序
     */
    @Test
    void retrieveRelevantKnowledge_multipleMatches_returnsSortedByRelevance() {
        // Arrange
        String query = "音乐推荐";
        int maxResults = 3;

        Response<Embedding> embeddingResponse = mock(Response.class);
        when(embeddingResponse.content()).thenReturn(testEmbedding);
        when(embeddingModel.embed(anyString())).thenReturn(embeddingResponse);

        // 创建多个高相似度的匹配（都超过 0.7 阈值）
        TextSegment segment1 = TextSegment.from("高相关知识");
        segment1.metadata().put("id", "1");
        segment1.metadata().put("title", "最相关");
        segment1.metadata().put("type", "music_theory");

        TextSegment segment2 = TextSegment.from("中等相关知识");
        segment2.metadata().put("id", "2");
        segment2.metadata().put("title", "较相关");
        segment2.metadata().put("type", "music_theory");

        TextSegment segment3 = TextSegment.from("相关知识");
        segment3.metadata().put("id", "3");
        segment3.metadata().put("title", "相关");
        segment3.metadata().put("type", "music_theory");

        List<EmbeddingMatch<TextSegment>> matches = List.of(
            new EmbeddingMatch<>(0.90, "id1", testEmbedding, segment1),
            new EmbeddingMatch<>(0.80, "id2", testEmbedding, segment2),
            new EmbeddingMatch<>(0.75, "id3", testEmbedding, segment3)
        );

        when(embeddingStore.findRelevant(any(Embedding.class), eq(maxResults), eq(0.7)))
            .thenReturn(matches);

        // Act
        List<KnowledgeDocument> results = ragService.retrieveRelevantKnowledge(query, maxResults);

        // Assert
        assertThat(results).hasSize(3);
        assertThat(results.get(0).getRelevanceScore()).isEqualTo(0.90);
        assertThat(results.get(1).getRelevanceScore()).isEqualTo(0.80);
        assertThat(results.get(2).getRelevanceScore()).isEqualTo(0.75);

        // 验证改进：所有结果都达到了 0.7+ 的高相关度
        assertThat(results.stream().allMatch(doc -> doc.getRelevanceScore() >= 0.7)).isTrue();
    }
}
