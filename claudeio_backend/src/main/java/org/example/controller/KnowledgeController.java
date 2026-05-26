package org.example.controller;

import org.example.dto.KnowledgeDocument;
import org.example.entity.MusicKnowledge;
import org.example.service.RagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/music/knowledge")
public class KnowledgeController {

    private final RagService ragService;

    public KnowledgeController(RagService ragService) {
        this.ragService = ragService;
    }

    /**
     * 添加新知识
     */
    @PostMapping("/add")
    public ResponseEntity<MusicKnowledge> addKnowledge(@RequestBody Map<String, String> request) {
        String title = request.get("title");
        String content = request.get("content");
        String type = request.get("type");
        String keywords = request.get("keywords");

        MusicKnowledge knowledge = ragService.addKnowledge(title, content, type, keywords);
        return ResponseEntity.ok(knowledge);
    }

    /**
     * 检索相关知识
     */
    @GetMapping("/search")
    public ResponseEntity<List<KnowledgeDocument>> searchKnowledge(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit) {
        List<KnowledgeDocument> results = ragService.retrieveRelevantKnowledge(query, limit);
        return ResponseEntity.ok(results);
    }

    /**
     * 重建知识库索引
     */
    @PostMapping("/rebuild-index")
    public ResponseEntity<String> rebuildIndex() {
        ragService.rebuildIndex();
        return ResponseEntity.ok("知识库索引重建完成");
    }

    /**
     * 初始化默认知识库
     */
    @PostMapping("/init-default")
    public ResponseEntity<String> initDefaultKnowledge() {
        ragService.createDefaultKnowledge();
        return ResponseEntity.ok("默认知识库创建完成");
    }
}
