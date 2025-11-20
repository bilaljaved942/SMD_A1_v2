-- Socially App Database Setup
-- Run this script in phpMyAdmin or MySQL CLI

-- Create database
CREATE DATABASE IF NOT EXISTS socially_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE socially_db;

-- Users table
CREATE TABLE users (
    uid VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    profile_picture TEXT,
    cover_photo TEXT,
    bio TEXT,
    fcm_token VARCHAR(255),
    online BOOLEAN DEFAULT FALSE,
    last_online BIGINT,
    created_at BIGINT NOT NULL,
    INDEX idx_email (email),
    INDEX idx_online (online)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Messages table
CREATE TABLE messages (
    id VARCHAR(255) PRIMARY KEY,
    sender_id VARCHAR(255) NOT NULL,
    receiver_id VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    timestamp BIGINT NOT NULL,
    media_url TEXT,
    type VARCHAR(50) DEFAULT 'text',
    is_edited BOOLEAN DEFAULT FALSE,
    edited_at BIGINT,
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at BIGINT,
    is_seen BOOLEAN DEFAULT FALSE,
    seen_at BIGINT,
    call_type VARCHAR(50),
    channel_name VARCHAR(255),
    vanish_mode BOOLEAN DEFAULT FALSE,
    INDEX idx_conversation (sender_id, receiver_id, timestamp),
    INDEX idx_timestamp (timestamp),
    FOREIGN KEY (sender_id) REFERENCES users(uid) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(uid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Posts table
CREATE TABLE posts (
    post_id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    media_url TEXT,
    media_type VARCHAR(50) DEFAULT 'image',
    caption TEXT,
    location VARCHAR(255),
    timestamp BIGINT NOT NULL,
    likes_count INT DEFAULT 0,
    comments_count INT DEFAULT 0,
    INDEX idx_user_posts (user_id, timestamp),
    INDEX idx_timestamp (timestamp),
    FOREIGN KEY (user_id) REFERENCES users(uid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Stories table
CREATE TABLE stories (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    media_url TEXT NOT NULL,
    timestamp BIGINT NOT NULL,
    expires_at BIGINT NOT NULL,
    is_video BOOLEAN DEFAULT FALSE,
    INDEX idx_user_stories (user_id, expires_at),
    INDEX idx_expiration (expires_at),
    FOREIGN KEY (user_id) REFERENCES users(uid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Story views table
CREATE TABLE story_views (
    story_id VARCHAR(255),
    viewer_id VARCHAR(255),
    viewed_at BIGINT NOT NULL,
    PRIMARY KEY (story_id, viewer_id),
    FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE CASCADE,
    FOREIGN KEY (viewer_id) REFERENCES users(uid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Comments table
CREATE TABLE comments (
    comment_id VARCHAR(255) PRIMARY KEY,
    post_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    timestamp BIGINT NOT NULL,
    INDEX idx_post_comments (post_id, timestamp),
    FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(uid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Likes table
CREATE TABLE likes (
    post_id VARCHAR(255),
    user_id VARCHAR(255),
    timestamp BIGINT NOT NULL,
    PRIMARY KEY (post_id, user_id),
    INDEX idx_post_likes (post_id),
    FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(uid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Follows table
CREATE TABLE follows (
    follower_id VARCHAR(255),
    following_id VARCHAR(255),
    status VARCHAR(50) DEFAULT 'pending',
    timestamp BIGINT NOT NULL,
    PRIMARY KEY (follower_id, following_id),
    INDEX idx_follower (follower_id, status),
    INDEX idx_following (following_id, status),
    FOREIGN KEY (follower_id) REFERENCES users(uid) ON DELETE CASCADE,
    FOREIGN KEY (following_id) REFERENCES users(uid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Notifications table
CREATE TABLE notifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    data JSON,
    is_read BOOLEAN DEFAULT FALSE,
    created_at BIGINT NOT NULL,
    INDEX idx_user_notifications (user_id, created_at),
    FOREIGN KEY (user_id) REFERENCES users(uid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Verify tables were created
SHOW TABLES;

-- Display structure of key tables
DESCRIBE users;
DESCRIBE messages;
DESCRIBE posts;

-- Success message
SELECT 'Database setup complete! All 9 tables created successfully.' AS status;
