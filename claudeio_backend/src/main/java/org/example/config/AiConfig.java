package org.example.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    public ChatLanguageModel chatLanguageModel(
            @Value("${music.ai.api-key:}") String apiKey,
            @Value("${music.ai.base-url:https://api.deepseek.com}") String baseUrl,
            @Value("${music.ai.model:deepseek-v4-pro}") String modelName) {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }
}
