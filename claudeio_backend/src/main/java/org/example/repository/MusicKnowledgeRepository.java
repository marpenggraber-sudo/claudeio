package org.example.repository;

import org.example.entity.MusicKnowledge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MusicKnowledgeRepository extends JpaRepository<MusicKnowledge, Long> {

    List<MusicKnowledge> findByKnowledgeType(String knowledgeType);

    @Query("SELECT mk FROM MusicKnowledge mk WHERE " +
           "LOWER(mk.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(mk.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(mk.relatedKeywords) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<MusicKnowledge> searchByKeyword(@Param("keyword") String keyword);
}
