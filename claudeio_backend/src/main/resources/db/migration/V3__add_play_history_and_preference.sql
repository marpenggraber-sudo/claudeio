-- 创建播放历史表和用户偏好表
-- 执行时间: 2026-05-10

CREATE TABLE IF NOT EXISTS play_history (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT                               NOT NULL COMMENT '用户ID',
    song_id       BIGINT                               NOT NULL COMMENT '歌曲ID',
    song_name     VARCHAR(255)                         NOT NULL COMMENT '歌曲名称',
    artist        VARCHAR(255)                         NOT NULL COMMENT '艺术家',
    play_duration INT        DEFAULT 0                 NULL COMMENT '播放时长(秒)',
    completed     TINYINT(1) DEFAULT 0                 NULL COMMENT '是否播放完成(1=是,0=否)',
    created_at    TIMESTAMP  DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '播放时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户播放历史';

CREATE INDEX idx_play_history_user_id ON play_history (user_id);
CREATE INDEX idx_play_history_song_id ON play_history (song_id);
CREATE INDEX idx_play_history_created_at ON play_history (created_at);

CREATE TABLE IF NOT EXISTS user_preference (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT                              NOT NULL COMMENT '用户ID',
    artist         VARCHAR(255)                        NOT NULL COMMENT '艺术家名称',
    play_count     INT       DEFAULT 1                 NULL COMMENT '播放次数',
    last_played_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '最后播放时间',
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT uk_user_artist UNIQUE (user_id, artist)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户艺术家偏好统计';

CREATE INDEX idx_user_preference_user_id ON user_preference (user_id);
CREATE INDEX idx_user_preference_play_count ON user_preference (play_count);