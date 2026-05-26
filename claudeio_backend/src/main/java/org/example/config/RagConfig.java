package org.example.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfig {

    @Bean
    public EmbeddingModel embeddingModel() {
        // 使用本地的 all-MiniLM-L6-v2 模型进行文本嵌入
        // 这是一个轻量级的开源模型，不需要API调用
        return new AllMiniLmL6V2EmbeddingModel();
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        // 使用内存向量存储
        // 生产环境可以替换为 Milvus、Qdrant 或 Pinecone
        return new InMemoryEmbeddingStore<>();
    }
}
