package org.example.service;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.example.dto.KnowledgeDocument;
import org.example.entity.MusicKnowledge;
import org.example.repository.MusicKnowledgeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final MusicKnowledgeRepository knowledgeRepository;

    @Value("${rag.similarity.threshold:0.7}")
    private double similarityThreshold;

    @Value("${rag.max.results:3}")
    private int defaultMaxResults;

    public RagService(EmbeddingModel embeddingModel,
                      EmbeddingStore<TextSegment> embeddingStore,
                      MusicKnowledgeRepository knowledgeRepository) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.knowledgeRepository = knowledgeRepository;
    }

    /**
     * 初始化时加载所有知识到向量存储
     */
    @PostConstruct
    public void initializeKnowledgeBase() {
        log.info("[RAG] 开始初始化知识库...");
        try {
            List<MusicKnowledge> allKnowledge = knowledgeRepository.findAll();
            if (allKnowledge.isEmpty()) {
                log.info("[RAG] 知识库为空，将创建默认知识");
                createDefaultKnowledge();
                allKnowledge = knowledgeRepository.findAll();
            }

            for (MusicKnowledge knowledge : allKnowledge) {
                indexKnowledge(knowledge);
            }
            log.info("[RAG] 知识库初始化完成，共加载 {} 条知识", allKnowledge.size());
        } catch (Exception e) {
            log.error("[RAG] 知识库初始化失败", e);
        }
    }

    /**
     * 将知识文档索引到向量存储
     */
    @Transactional
    public void indexKnowledge(MusicKnowledge knowledge) {
        try {
            // 组合标题和内容作为文本段
            String text = knowledge.getTitle() + "\n" + knowledge.getContent();

            // 创建元数据
            Metadata metadata = new Metadata();
            metadata.put("id", knowledge.getId().toString());
            metadata.put("title", knowledge.getTitle());
            metadata.put("type", knowledge.getKnowledgeType());

            // 创建文本段
            TextSegment segment = TextSegment.from(text, metadata);

            // 生成嵌入向量
            Embedding embedding = embeddingModel.embed(segment).content();

            // 存储到向量数据库
            embeddingStore.add(embedding, segment);

            log.debug("[RAG] 已索引知识: id={}, title={}", knowledge.getId(), knowledge.getTitle());
        } catch (Exception e) {
            log.error("[RAG] 索引知识失败: id={}", knowledge.getId(), e);
        }
    }

    /**
     * 根据查询检索相关知识
     */
    public List<KnowledgeDocument> retrieveRelevantKnowledge(String query, int maxResults) {
        try {
            log.info("[RAG] 检索相关知识: query={}, maxResults={}, threshold={}", query, maxResults, similarityThreshold);

            // 生成查询的嵌入向量
            Embedding queryEmbedding = embeddingModel.embed(query).content();

            // 在向量存储中搜索最相似的文档
            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(
                queryEmbedding,
                maxResults,
                similarityThreshold  // 使用配置的阈值（默认 0.7）
            );

            // 转换为知识文档
            List<KnowledgeDocument> results = matches.stream()
                .map(match -> {
                    TextSegment segment = match.embedded();
                    Metadata metadata = segment.metadata();

                    return new KnowledgeDocument(
                        Long.parseLong(metadata.getString("id")),
                        metadata.getString("title"),
                        segment.text(),
                        metadata.getString("type"),
                        match.score()
                    );
                })
                .collect(Collectors.toList());

            log.info("[RAG] 检索完成: query={}, foundCount={}, scores={}",
                query, results.size(),
                results.stream().map(doc -> String.format("%.2f", doc.getRelevanceScore())).toList());
            return results;

        } catch (Exception e) {
            log.error("[RAG] 检索知识失败: query={}", query, e);
            return new ArrayList<>();
        }
    }

    /**
     * 添加新知识到知识库
     */
    @Transactional
    public MusicKnowledge addKnowledge(String title, String content, String type, String keywords) {
        MusicKnowledge knowledge = new MusicKnowledge();
        knowledge.setTitle(title);
        knowledge.setContent(content);
        knowledge.setKnowledgeType(type);
        knowledge.setRelatedKeywords(keywords);

        knowledge = knowledgeRepository.save(knowledge);
        indexKnowledge(knowledge);

        log.info("[RAG] 已添加新知识: id={}, title={}", knowledge.getId(), title);
        return knowledge;
    }

    /**
     * 格式化检索结果为提示词上下文
     */
    public String formatKnowledgeContext(List<KnowledgeDocument> documents) {
        if (documents.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder("\n\n【相关音乐知识】\n");
        for (int i = 0; i < documents.size(); i++) {
            KnowledgeDocument doc = documents.get(i);
            context.append(String.format("%d. %s\n%s\n\n",
                i + 1,
                doc.getTitle(),
                doc.getContent()
            ));
        }

        return context.toString();
    }

    /**
     * 创建默认音乐知识库
     */
    @Transactional
    public void createDefaultKnowledge() {
        log.info("[RAG] 创建默认音乐知识库...");

        // 艺术家知识
        addKnowledge(
            "周杰伦",
            "周杰伦是华语流行音乐天王，以其独特的R&B和中国风融合风格闻名。代表作包括《晴天》《稻香》《青花瓷》等。他的音乐风格多样，涵盖流行、摇滚、嘻哈、中国风等多种元素。",
            "artist",
            "周杰伦,Jay Chou,华语,流行,R&B,中国风"
        );

        addKnowledge(
            "Taylor Swift",
            "Taylor Swift是美国著名流行歌手和词曲作者，以其叙事性歌词和多样化的音乐风格著称。从乡村音乐起步，逐渐转向流行音乐。代表作包括《Love Story》《Shake It Off》《Anti-Hero》等。",
            "artist",
            "Taylor Swift,泰勒斯威夫特,欧美,流行,乡村"
        );

        addKnowledge(
            "陈奕迅",
            "陈奕迅是香港著名歌手，被誉为华语乐坛最具实力的男歌手之一。他的歌声富有情感，演唱风格细腻。代表作包括《十年》《浮夸》《富士山下》《好久不见》等。",
            "artist",
            "陈奕迅,Eason Chan,粤语,华语,流行"
        );

        // 音乐风格知识
        addKnowledge(
            "中国风音乐",
            "中国风音乐是将中国传统音乐元素与现代流行音乐相结合的音乐风格。常使用古筝、琵琶、二胡等传统乐器，歌词多采用古诗词意境。代表歌手有周杰伦、许嵩等。",
            "genre",
            "中国风,古风,传统,流行"
        );

        addKnowledge(
            "R&B音乐",
            "R&B（Rhythm and Blues）节奏布鲁斯，是一种融合了爵士、福音和布鲁斯的音乐风格。特点是强烈的节奏感和情感丰富的演唱。在华语乐坛，周杰伦、陶喆等歌手擅长这种风格。",
            "genre",
            "R&B,节奏布鲁斯,流行,爵士"
        );

        addKnowledge(
            "治愈系音乐",
            "治愈系音乐通常旋律温暖、歌词积极向上，能够给人带来心灵慰藉。常见于轻音乐、民谣、轻快的流行歌曲。适合在心情低落或需要放松时聆听。",
            "genre",
            "治愈,轻音乐,民谣,放松"
        );

        addKnowledge(
            "伤感音乐",
            "伤感音乐通常旋律忧郁、歌词感人，表达失恋、思念、遗憾等情感。适合在心情不好或需要情感宣泄时聆听。代表歌曲如《十年》《后来》《遗憾》等。",
            "genre",
            "伤感,忧郁,失恋,情歌"
        );

        // 音乐理论知识
        addKnowledge(
            "音乐推荐原则",
            "为用户推荐音乐时应考虑：1) 用户的历史播放记录和偏好；2) 用户当前的心情和场景；3) 音乐的流行度和质量；4) 风格的多样性，避免过于单一。推荐时应该平衡用户熟悉的风格和新的探索。",
            "music_theory",
            "推荐,算法,个性化"
        );

        addKnowledge(
            "歌单策划技巧",
            "优秀的歌单应该：1) 有明确的主题（如心情、场景、风格）；2) 歌曲之间有良好的过渡和节奏变化；3) 时长适中（通常30-60分钟）；4) 包含经典和新歌的平衡；5) 考虑听众的接受度。",
            "music_theory",
            "歌单,策划,编排"
        );

        log.info("[RAG] 默认知识库创建完成");
    }

    /**
     * 重建整个知识库索引
     */
    @Transactional
    public void rebuildIndex() {
        log.info("[RAG] 开始重建知识库索引...");
        // 注意：InMemoryEmbeddingStore 没有 clear 方法，需要重新创建
        // 生产环境中使用持久化向量数据库时可以实现清空操作
        List<MusicKnowledge> allKnowledge = knowledgeRepository.findAll();
        for (MusicKnowledge knowledge : allKnowledge) {
            indexKnowledge(knowledge);
        }
        log.info("[RAG] 知识库索引重建完成，共 {} 条", allKnowledge.size());
    }
}
