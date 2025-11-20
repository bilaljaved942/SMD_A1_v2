# Implementation Checklist - Socially App Migration

## 📋 Complete Task List

### ✅ Phase 1: Foundation (COMPLETED)

- [x] Remove Firebase dependencies (except FCM)
- [x] Add Retrofit, Room, WorkManager, Security libraries
- [x] Create all SQLite entities (7 entities)
- [x] Create all DAOs with Flow support
- [x] Create Room Database
- [x] Create all Retrofit API service interfaces
- [x] Create API models (Request/Response)
- [x] Setup RetrofitClient with OkHttp logging
- [x] Create SecurePreferences for encrypted storage
- [x] Create utility classes (Network, Image, Date)
- [x] Create OfflineSyncWorker
- [x] Update splash screen with session checking
- [x] Create PHP database configuration
- [x] Create PHP auth utilities
- [x] Create PHP upload utilities
- [x] Create PHP auth endpoints (signup, login, logout, session)
- [x] Create MySQL database schema
- [x] Write comprehensive documentation

### 🔄 Phase 2: Authentication (IN PROGRESS)

#### Client-Side

- [x] Create SignupActivity with API integration
- [ ] Update MainActivity2 layout to work with SignupActivity
- [ ] Create LoginActivity with API integration
- [ ] Update MainActivity4 layout to work with LoginActivity
- [ ] Test signup flow
- [ ] Test login flow
- [ ] Test session persistence
- [ ] Test logout functionality
- [ ] Handle network errors gracefully
- [ ] Add loading indicators

#### Server-Side

- [x] Signup endpoint
- [x] Login endpoint
- [x] Logout endpoint
- [x] Session check endpoint
- [ ] Password reset endpoint
- [ ] Email verification (optional)

### ⏳ Phase 3: Posts & Feed System

#### Client-Side

- [ ] Update MainActivity5 (Home/Feed) to use REST API
- [ ] Replace Firebase post fetching with Retrofit calls
- [ ] Implement post creation with API
- [ ] Cache posts in SQLite
- [ ] Add pull-to-refresh
- [ ] Implement lazy loading/pagination
- [ ] Update FeedAdapter to use PostEntity
- [ ] Add offline post viewing
- [ ] Queue posts created offline
- [ ] Test post creation (online)
- [ ] Test post creation (offline + sync)

#### Server-Side

- [ ] Create `posts/create.php`
- [ ] Create `posts/feed.php` (with pagination)
- [ ] Create `posts/user.php` (user's posts)
- [ ] Create `posts/like.php`
- [ ] Create `posts/unlike.php`
- [ ] Create `posts/delete.php`

### ⏳ Phase 4: Stories System

#### Client-Side

- [ ] Update story fetching in MainActivity5
- [ ] Replace Firebase stories with API calls
- [ ] Implement story upload (image/video)
- [ ] Add 24-hour expiration check
- [ ] Update StoryAdapter to use StoryEntity
- [ ] Cache stories in SQLite
- [ ] Auto-delete expired stories locally
- [ ] Navigate to MainActivity20 for story viewing
- [ ] Test story creation (online)
- [ ] Test story creation (offline + sync)
- [ ] Test story expiration

#### Server-Side

- [ ] Create `stories/create.php` (with file upload)
- [ ] Create `stories/fetch.php` (active stories only)
- [ ] Create `stories/user.php` (user's stories)
- [ ] Create `stories/view.php` (mark as viewed)
- [ ] Create `stories/delete.php`
- [ ] Add cron job for auto-deletion (24h)

### ⏳ Phase 5: Comments System

#### Client-Side

- [ ] Update CommentsActivity to use REST API
- [ ] Replace Firebase comment fetching
- [ ] Implement add comment via API
- [ ] Update CommentsAdapter
- [ ] Cache comments in SQLite
- [ ] Queue comments created offline
- [ ] Test comment creation (online)
- [ ] Test comment creation (offline + sync)

#### Server-Side

- [ ] Create `comments/add.php`
- [ ] Create `comments/get.php` (by post ID)
- [ ] Create `comments/delete.php`

### ⏳ Phase 6: Messaging System (Complex)

#### Client-Side

- [ ] Create ChatActivity layout
- [ ] Create MessageAdapter
- [ ] Implement message sending via API
- [ ] Implement vanish mode toggle
- [ ] Add edit message (within 5 min)
- [ ] Add delete message (within 5 min)
- [ ] Implement media upload (images, videos, docs)
- [ ] Cache messages in SQLite
- [ ] Queue messages sent offline
- [ ] Implement real-time updates (polling or WebSocket)
- [ ] Mark messages as seen
- [ ] Show typing indicators (optional)
- [ ] Test text messages
- [ ] Test media messages
- [ ] Test edit functionality
- [ ] Test delete functionality
- [ ] Test vanish mode
- [ ] Test offline queue

#### Server-Side

- [ ] Create `messages/send.php`
- [ ] Create `messages/fetch.php` (conversation)
- [ ] Create `messages/edit.php` (5-min check)
- [ ] Create `messages/delete.php` (5-min check)
- [ ] Create `messages/uploadMedia.php`
- [ ] Create `messages/seen.php`
- [ ] Implement vanish mode logic

### ⏳ Phase 7: Follow System

#### Client-Side

- [ ] Update follow button logic in profile activities
- [ ] Implement send follow request
- [ ] Implement accept follow request
- [ ] Implement reject follow request
- [ ] Implement unfollow
- [ ] Show followers list (MainActivity)
- [ ] Show following list (MainActivity)
- [ ] Show pending requests
- [ ] Cache follow relationships in SQLite
- [ ] Queue follow actions offline
- [ ] Test follow request
- [ ] Test accept/reject
- [ ] Test unfollow

#### Server-Side

- [ ] Create `follow/request.php`
- [ ] Create `follow/accept.php`
- [ ] Create `follow/reject.php`
- [ ] Create `follow/unfollow.php`
- [ ] Create `follow/followers.php`
- [ ] Create `follow/following.php`
- [ ] Create `follow/pending.php`
- [ ] Create `follow/status.php`

### ⏳ Phase 8: User Profile

#### Client-Side

- [ ] Update profile picture upload
- [ ] Update cover photo upload
- [ ] Update bio
- [ ] Show user's posts
- [ ] Show user's followers/following count
- [ ] Test profile updates

#### Server-Side

- [ ] Create `users/profile.php` (get user)
- [ ] Create `users/update.php`
- [ ] Create `users/upload-profile.php`
- [ ] Create `users/upload-cover.php`

### ⏳ Phase 9: Search & Discovery

#### Client-Side

- [ ] Create search UI
- [ ] Implement user search
- [ ] Add filter by followers
- [ ] Add filter by following
- [ ] Show search results
- [ ] Test search functionality

#### Server-Side

- [ ] Create `users/search.php`

### ⏳ Phase 10: Online/Offline Status

#### Client-Side

- [ ] Implement heartbeat mechanism
- [ ] Update online status on app open
- [ ] Update offline status on app close
- [ ] Show online indicator in UI
- [ ] Update status badge in chat
- [ ] Show last seen time
- [ ] Test online status updates

#### Server-Side

- [ ] Create `users/online.php` (set online)
- [ ] Create `users/offline.php` (set offline)
- [ ] Create `users/status.php` (get status)

### ⏳ Phase 11: Voice & Video Calls (Agora)

#### Setup

- [ ] Get Agora App ID
- [ ] Create AgoraConfig class
- [ ] Add required permissions to manifest

#### Client-Side

- [ ] Create VoiceCallActivity
- [ ] Create VideoCallActivity
- [ ] Implement call invite via messaging
- [ ] Handle incoming call
- [ ] Show call UI
- [ ] Handle call end
- [ ] Test voice call
- [ ] Test video call
- [ ] Test call from chat

#### Server-Side (Optional)

- [ ] Create `calls/token.php` (Agora token generation)
- [ ] Store call logs

### ⏳ Phase 12: Push Notifications (FCM)

#### Client-Side

- [ ] Update FirebaseNotificationService
- [ ] Register FCM token with server
- [ ] Handle incoming notifications
- [ ] Show notification for new messages
- [ ] Show notification for follow requests
- [ ] Show notification for screenshots
- [ ] Test notification delivery

#### Server-Side

- [ ] Create `users/fcm-token.php` (register token)
- [ ] Create `notifications/send.php`
- [ ] Implement FCM server-side sending

### ⏳ Phase 13: Security Features

#### Screenshot Detection

- [ ] Add screenshot listener in ChatActivity
- [ ] Detect screenshot event
- [ ] Send notification to other user
- [ ] Test screenshot detection

#### Other Security

- [ ] Implement rate limiting on server
- [ ] Add input validation
- [ ] Sanitize user inputs
- [ ] Add SQL injection prevention
- [ ] Add XSS prevention

### ⏳ Phase 14: Offline Sync & WorkManager

#### Client-Side

- [ ] Schedule PeriodicWorkRequest
- [ ] Listen for network changes
- [ ] Trigger sync on connectivity
- [ ] Handle sync conflicts
- [ ] Prevent duplicate syncs
- [ ] Test offline message sync
- [ ] Test offline post sync
- [ ] Test offline story sync
- [ ] Test offline follow sync
- [ ] Test retry mechanism

### ⏳ Phase 15: Image Caching (Picasso)

#### Client-Side

- [ ] Replace Glide with Picasso where needed
- [ ] Implement offline image viewing
- [ ] Add disk cache
- [ ] Add memory cache
- [ ] Test image loading offline

### ⏳ Phase 16: Testing & Bug Fixes

#### Unit Tests

- [ ] Write tests for API services
- [ ] Write tests for DAOs
- [ ] Write tests for utilities
- [ ] Write tests for ViewModels

#### Instrumentation Tests

- [ ] Test splash screen navigation
- [ ] Test signup flow
- [ ] Test login flow
- [ ] Test post creation
- [ ] Test messaging

#### Manual Testing

- [ ] Test all features online
- [ ] Test all features offline
- [ ] Test sync after coming online
- [ ] Test edge cases
- [ ] Test error handling
- [ ] Fix bugs

### ⏳ Phase 17: Performance Optimization

- [ ] Optimize database queries
- [ ] Add database indexes
- [ ] Optimize image loading
- [ ] Reduce API call frequency
- [ ] Implement pagination everywhere
- [ ] Profile app performance
- [ ] Fix memory leaks

### ⏳ Phase 18: Documentation & Git

#### Documentation

- [ ] Update inline code comments
- [ ] Complete API documentation
- [ ] Write user manual (optional)

#### Git Commits

- [ ] Commit authentication feature
- [ ] Commit posts feature
- [ ] Commit stories feature
- [ ] Commit messaging feature
- [ ] Commit follow system
- [ ] Commit calls feature
- [ ] Commit notifications
- [ ] Commit offline sync
- [ ] Tag version 2.0.0

### ⏳ Phase 19: Deployment

#### Server

- [ ] Deploy PHP backend to production server
- [ ] Setup production MySQL database
- [ ] Configure SSL certificate
- [ ] Setup domain
- [ ] Update BASE_URL in app

#### App

- [ ] Generate signed APK
- [ ] Test production build
- [ ] Prepare for Play Store (optional)

### ✅ Phase 20: Final Review

- [ ] Code review
- [ ] Security audit
- [ ] Performance review
- [ ] Test all features end-to-end
- [ ] Create demo video
- [ ] Submit assignment

---

## 📊 Progress Tracker

**Total Tasks**: 200+  
**Completed**: ~50 (25%)  
**In Progress**: ~20 (10%)  
**Remaining**: ~130 (65%)

**Estimated Time**: 2-3 weeks for full implementation

---

## 🎯 Priority Matrix

| Priority    | Phase              | Estimated Time |
| ----------- | ------------------ | -------------- |
| 🔴 Critical | Authentication     | 2-3 days       |
| 🔴 Critical | Posts & Feed       | 3-4 days       |
| 🟠 High     | Messaging          | 4-5 days       |
| 🟠 High     | Stories            | 2-3 days       |
| 🟡 Medium   | Follow System      | 2 days         |
| 🟡 Medium   | Offline Sync       | 2 days         |
| 🟢 Low      | Voice/Video Calls  | 2-3 days       |
| 🟢 Low      | Push Notifications | 1-2 days       |

---

## 💡 Tips

1. **Test incrementally** - Test each feature before moving to next
2. **Commit frequently** - Use meaningful commit messages
3. **Handle errors** - Add proper try-catch and error messages
4. **Log everything** - Use Log.d/e for debugging
5. **Check network** - Always verify network before API calls
6. **Cache data** - Save everything to SQLite
7. **Use coroutines** - Never block main thread
8. **Validate inputs** - Both client and server side

---

**Last Updated**: November 15, 2025
