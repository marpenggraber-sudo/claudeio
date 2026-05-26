package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KnowledgeDocument {
    private Long id;
    private String title;
    private String content;
    private String knowledgeType;
    private double relevanceScore;
}
