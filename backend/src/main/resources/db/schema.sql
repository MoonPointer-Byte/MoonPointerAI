-- EnglishAI MySQL 数据库初始化脚本
-- 数据库会在首次启动时由 Spring 自动创建 (createDatabaseIfNotExist=true)
-- 也可手动执行: mysql -u root -p < schema.sql

CREATE DATABASE IF NOT EXISTS englishai
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE englishai;

-- 学习笔记（支持 AI / 手写双模式）
CREATE TABLE IF NOT EXISTS notes (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  client_session_id VARCHAR(64)  NOT NULL COMMENT '浏览器会话 ID',
  video_url         VARCHAR(512)          COMMENT '关联视频链接',
  title             VARCHAR(256)          COMMENT '笔记标题',
  content           TEXT                  COMMENT '兼容旧版：等同 manual_content',
  manual_content    TEXT                  COMMENT '手写笔记内容',
  ai_content        TEXT                  COMMENT 'AI 生成笔记内容',
  preferred_mode    VARCHAR(16)  NOT NULL DEFAULT 'MANUAL' COMMENT 'MANUAL | AI',
  ai_status         VARCHAR(16)  NOT NULL DEFAULT 'IDLE' COMMENT 'IDLE | GENERATING | DONE | FAILED',
  source_lang       VARCHAR(8)            COMMENT '源语言',
  target_lang       VARCHAR(8)            COMMENT '目标语言',
  last_ai_generated_at DATETIME           COMMENT '上次 AI 生成时间',
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_client_session (client_session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 实时字幕片段（供 AI 笔记生成使用）
CREATE TABLE IF NOT EXISTS transcript_segments (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  client_session_id VARCHAR(64) NOT NULL COMMENT '浏览器会话 ID',
  segment_id      VARCHAR(64)  NOT NULL COMMENT '字幕块 ID',
  source_text     TEXT         NOT NULL COMMENT '原文',
  translated_text TEXT                  COMMENT '译文',
  timestamp_ms    BIGINT                COMMENT '会话内时间戳(ms)',
  is_final        TINYINT(1)   NOT NULL DEFAULT 1,
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_session_segment (client_session_id, segment_id),
  INDEX idx_seg_session (client_session_id),
  INDEX idx_seg_time (client_session_id, timestamp_ms)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
