-- 添加音乐风格字段到 play_history 表
-- 执行时间: 2026-05-10

ALTER TABLE play_history
ADD COLUMN genre VARCHAR(50) NULL COMMENT '音乐风格',
ADD COLUMN genre_source VARCHAR(20) NULL COMMENT '风格来源: CACHE/DATABASE/AI/DEFAULT';

-- 创建索引以提高查询性能
CREATE INDEX idx_play_history_genre ON play_history(genre);
CREATE INDEX idx_play_history_song_id_genre ON play_history(song_id, genre);

-- 验证字段已添加
SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE, COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'music_agent'
  AND TABLE_NAME = 'play_history'
  AND COLUMN_NAME IN ('genre', 'genre_source');
