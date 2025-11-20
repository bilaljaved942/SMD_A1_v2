# Socially App - Migration from Firebase to Custom REST API

## Project Status: MAJOR UPGRADE IN PROGRESS

This document outlines the comprehensive migration from Firebase to custom REST APIs with MySQL/SQLite backend.

---

## ✅ COMPLETED WORK

### 1. **Gradle Dependencies Updated** ✓

- Removed all Firebase dependencies except FCM (for push notifications only)
- Added Retrofit 2.11.0 for REST API calls
- Added Room Database 2.6.1 for SQLite offline support
- Added Jetpack Security for encrypted session storage
- Added WorkManager for offline sync
- Added Picasso for image caching
- Added Coroutines for async operations
- Added OkHttp logging interceptor

### 2. **SQLite Database Architecture Created** ✓

Created complete Room database with entities:

- `UserEntity` - User profiles with online status
- `MessageEntity` - Messages with vanish mode, edit/delete support
- `PostEntity` - Posts with media and engagement metrics
- `StoryEntity` - Stories with 24h expiration
- `CommentEntity` - Post comments
- `FollowEntity` - Follow relationships with status
- `PendingActionEntity` - Queue for offline actions

Created DAOs for all entities with Flow support for reactive UI updates.

### 3. **Retrofit API Client Built** ✓

Created comprehensive API service interfaces:

- `AuthApiService` - Signup, Login, Logout, Session management
- `MessageApiService` - Send, fetch, edit, delete messages
- `PostApiService` - Create, fetch, like, unlike posts
- `StoryApiService` - Create, fetch, view stories
- `CommentApiService` - Add, fetch, delete comments
- `FollowApiService` - Follow requests, accept, reject, unfollow
- `UserApiService` - Search, online status, FCM tokens

### 4. **Utilities Created** ✓

- `SecurePreferences` - Encrypted SharedPreferences for session storage
- `NetworkUtils` - Network connectivity checker
- `ImageUtils` - Base64 conversion, image validation
- `DateUtils` - Timestamp formatting, expiration checks
- `OfflineSyncWorker` - WorkManager for background sync

### 5. **Splash Screen Updated** ✓

New `SplashActivity` with:

- 5-second display
- Automatic session checking
- Navigation logic: logged-in → Home, first-time → Profile Setup, else → Login

---

## 🚧 WORK IN PROGRESS / TODO

### Priority 1: Authentication System

**Files to Update:**

1. `MainActivity2.kt` → Replace with `SignupActivity.kt` (created)
2. `MainActivity4.kt` → Create `LoginActivity.kt`
3. Need to implement login API calls
4. Add logout functionality

### Priority 2: Update All Activities

Replace Firebase calls with REST API + SQLite in:

- `MainActivity5.kt` (Home/Feed) - Update stories and posts loading
- All MainActivity files (6-23) - Replace Firebase with API calls
- `CommentsActivity.kt` - Use Comment API
- Adapters: `FeedAdapter`, `StoryAdapter`, `MessageAdapter`, etc.

### Priority 3: Messaging System

Create complete messaging module:

- Chat UI with vanish mode toggle
- Edit message (within 5 minutes)
- Delete message (within 5 minutes)
- Media uploads (images, videos, documents)
- Offline message queue
- Real-time updates via polling or WebSocket

### Priority 4: Agora Integration

- Voice call activity
- Video call activity
- Call invite through messaging system
- Permissions handling

### Priority 5: Push Notifications (FCM)

Update `FirebaseNotificationService.kt`:

- Register FCM token with API
- Handle incoming notifications
- Send notifications via API for:
  - New messages
  - Follow requests
  - Screenshot alerts

### Priority 6: Offline Sync

- Schedule WorkManager periodic sync
- Handle network changes
- Retry failed operations
- Prevent duplicates

### Priority 7: Screenshot Detection

Add security feature to detect screenshots in chat.

---

## 📁 SERVER-SIDE REQUIREMENTS

### PHP API Structure (Create these files)

```
server/
├── api/
│   ├── auth/
│   │   ├── signup.php
│   │   ├── login.php
│   │   ├── logout.php
│   │   └── session.php
│   ├── messages/
│   │   ├── send.php
│   │   ├── fetch.php
│   │   ├── edit.php
│   │   ├── delete.php
│   │   └── uploadMedia.php
│   ├── posts/
│   │   ├── create.php
│   │   ├── feed.php
│   │   ├── like.php
│   │   └── unlike.php
│   ├── stories/
│   │   ├── create.php
│   │   ├── fetch.php
│   │   └── view.php
│   ├── comments/
│   │   ├── add.php
│   │   ├── get.php
│   │   └── delete.php
│   ├── follow/
│   │   ├── request.php
│   │   ├── accept.php
│   │   ├── reject.php
│   │   └── unfollow.php
│   └── users/
│       ├── search.php
│       ├── online.php
│       └── fcm-token.php
├── uploads/
│   ├── profiles/
│   ├── posts/
│   ├── stories/
│   └── messages/
├── config/
│   ├── database.php
│   └── constants.php
└── utils/
    ├── auth.php
    └── upload.php
```

---

## 🗄️ MySQL DATABASE SCHEMA

```sql
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
```

---

## 🔧 CONFIGURATION NEEDED

### 1. Update Base URL

In `RetrofitClient.kt`:

```kotlin
private const val BASE_URL = "http://YOUR_SERVER_IP:8000/api/"
// Or for production: "https://yourdomain.com/api/"
```

### 2. Agora Configuration

Get Agora App ID from https://www.agora.io/ and add to your project.

### 3. FCM Configuration

Keep existing `google-services.json` for FCM only.

---

## 📝 GIT COMMIT MESSAGES

After implementing each feature, use these commit messages:

```bash
# After authentication
git add .
git commit -m "feat: Replace Firebase Auth with custom REST API authentication

- Implemented signup with email/password
- Implemented login with session storage
- Added encrypted SharedPreferences for security
- Integrated with SQLite for offline user data
- Added session validation on splash screen"

# After messaging system
git commit -m "feat: Implement custom messaging system with vanish mode

- Built messaging API integration
- Added edit/delete within 5 minutes
- Implemented vanish mode for messages
- Added media upload support
- Integrated offline message queue with WorkManager"

# After stories
git commit -m "feat: Replace Firebase Stories with custom API

- Implemented story upload (image/video)
- Added 24-hour auto-expiration
- Built horizontal stories bar in feed
- Integrated with SQLite for offline support"

# After follow system
git commit -m "feat: Implement follow system via REST API

- Added send/accept/reject follow requests
- Built followers/following lists
- Implemented follow status checking
- Added offline follow queue"

# After FCM
git commit -m "feat: Integrate FCM for push notifications

- Registered FCM token with backend
- Implemented notification handler
- Added notifications for messages, follows, screenshots"

# After offline sync
git commit -m "feat: Add comprehensive offline support with SQLite

- Implemented WorkManager for background sync
- Added pending actions queue
- Built auto-retry mechanism
- Ensured no duplicate syncs"
```

---

## ⚠️ IMPORTANT NOTES

1. **Base URL**: Update `RetrofitClient.kt` with your actual server URL
2. **API Keys**: Get Agora App ID for video/audio calls
3. **Server Setup**: Deploy PHP backend with MySQL database
4. **Testing**: Test offline functionality extensively
5. **Security**: Implement proper authentication tokens and encryption

---

## 🎓 LEARNING RESOURCES

- Retrofit: https://square.github.io/retrofit/
- Room Database: https://developer.android.com/training/data-storage/room
- WorkManager: https://developer.android.com/topic/libraries/architecture/workmanager
- Agora: https://docs.agora.io/en/video-calling/get-started/get-started-sdk

---

## 📞 NEXT STEPS

1. Complete authentication (Login/Signup activities)
2. Update all activities to use REST APIs instead of Firebase
3. Implement messaging system with all features
4. Integrate Agora for calls
5. Set up FCM notification handling
6. Test offline sync thoroughly
7. Deploy backend PHP APIs
8. Configure MySQL database
9. Test end-to-end functionality

**Estimated Time**: 2-3 weeks for complete implementation

---

Generated on: November 15, 2025
