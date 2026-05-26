package org.example.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "play_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "song_id", nullable = false)
    private Long songId;

    @Column(name = "song_name", nullable = false, length = 255)
    private String songName;

    @Column(name = "artist", nullable = false, length = 255)
    private String artist;

    @Column(name = "play_duration", columnDefinition = "int default 0")
    private Integer playDuration;

    @Column(name = "completed", columnDefinition = "tinyint(1) default 0")
    private Boolean completed;

    @Column(name = "genre", length = 50)
    private String genre;

    @Column(name = "genre_source", length = 20)
    private String genreSource;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
