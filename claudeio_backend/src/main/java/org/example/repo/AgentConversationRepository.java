package org.example.repo;

import org.example.entity.AgentConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentConversationRepository extends JpaRepository<AgentConversation, Long> {
    List<AgentConversation> findTop10ByUser_MusicUserIdOrderByCreatedAtDesc(Long musicUserId);
    List<AgentConversation> findTop20ByUser_IdOrderByCreatedAtDesc(Long userId);
}
