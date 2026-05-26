package org.example.service;

import org.example.dto.AgentReply;
import org.springframework.stereotype.Service;

@Service
public class AiChatService {

    private final ChatOrchestratorService chatOrchestratorService;

    public AiChatService(ChatOrchestratorService chatOrchestratorService) {
        this.chatOrchestratorService = chatOrchestratorService;
    }

    public AgentReply chat(String message, Long userId) {
        return chatOrchestratorService.chat(message, userId);
    }
}
