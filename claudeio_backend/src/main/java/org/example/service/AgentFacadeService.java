package org.example.service;

import org.example.dto.AgentReply;
import org.springframework.stereotype.Service;

@Service
public class AgentFacadeService {

    private final AiChatService aiChatService;
    private final ChatOrchestratorService chatOrchestratorService;

    public AgentFacadeService(AiChatService aiChatService, ChatOrchestratorService chatOrchestratorService) {
        this.aiChatService = aiChatService;
        this.chatOrchestratorService = chatOrchestratorService;
    }

    public AgentReply chat(String message, Long userId) {
        return aiChatService.chat(message, userId);
    }

    public AgentReply generateGreeting(Long userId) {
        return chatOrchestratorService.generateGreeting(userId);
    }
}
