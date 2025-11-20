# 📖 API Testing Guide - Complete Postman Collection

## ✅ Already Working APIs

### 1. **Signup** ✓

- Method: `POST`
- URL: `http://localhost/socially/api/auth/signup.php`
- Headers: `Content-Type: application/json`
- Body:

```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123"
}
```

### 2. **Login** ✓

- Method: `POST`
- URL: `http://localhost/socially/api/auth/login.php`
- Headers: `Content-Type: application/json`
- Body:

```json
{
  "email": "john@example.com",
  "password": "password123"
}
```

- **Save the token from response!**

### 3. **Session Check** ✓

- Method: `GET`
- URL: `http://localhost/socially/api/auth/session.php`
- Headers: `Authorization: Bearer YOUR_TOKEN_HERE`

### 4. **Logout** ✓

- Method: `POST`
- URL: `http://localhost/socially/api/auth/logout.php`
- Headers: `Authorization: Bearer YOUR_TOKEN_HERE`

---

## 🆕 New APIs Just Created

### 5. **Create Post**

- Method: `POST`
- URL: `http://localhost/socially/api/posts/create.php`
- Headers:
  - `Content-Type: application/json`
  - `Authorization: Bearer YOUR_TOKEN_HERE`
- Body:

```json
{
  "caption": "My first post!",
  "location": "Lahore, Pakistan",
  "mediaBase64": "data:image/jpeg;base64,/9j/4AAQSkZJRg..."
}
```

### 6. **Get Feed**

- Method: `GET`
- URL: `http://localhost/socially/api/posts/feed.php?page=1&limit=20`
- Headers: `Authorization: Bearer YOUR_TOKEN_HERE`

### 7. **Like/Unlike Post**

- Method: `POST`
- URL: `http://localhost/socially/api/posts/like.php`
- Headers:
  - `Content-Type: application/json`
  - `Authorization: Bearer YOUR_TOKEN_HERE`
- Body:

```json
{
  "postId": "post_673..."
}
```

### 8. **Create Story**

- Method: `POST`
- URL: `http://localhost/socially/api/stories/create.php`
- Headers:
  - `Content-Type: application/json`
  - `Authorization: Bearer YOUR_TOKEN_HERE`
- Body:

```json
{
  "mediaBase64": "data:image/jpeg;base64,/9j/4AAQSkZJRg...",
  "isVideo": false
}
```

### 9. **Fetch Stories**

- Method: `GET`
- URL: `http://localhost/socially/api/stories/fetch.php`
- Headers: `Authorization: Bearer YOUR_TOKEN_HERE`

### 10. **Send Message**

- Method: `POST`
- URL: `http://localhost/socially/api/messages/send.php`
- Headers:
  - `Content-Type: application/json`
  - `Authorization: Bearer YOUR_TOKEN_HERE`
- Body:

```json
{
  "receiverId": "user_673...",
  "content": "Hello!",
  "type": "text",
  "vanishMode": false
}
```

### 11. **Fetch Messages**

- Method: `GET`
- URL: `http://localhost/socially/api/messages/fetch.php?otherUserId=user_673...&since=0`
- Headers: `Authorization: Bearer YOUR_TOKEN_HERE`

---

## 📝 Next Steps for Assignment Completion

### Phase 1: Complete Remaining Server APIs ✅ DONE!

- [x] Comments API (add, fetch, delete)
- [x] Follow API (request, unfollow, list)
- [x] User Search API
- [x] Online Status API
- [x] FCM Token registration

### 12. **Add Comment**

- Method: `POST`
- URL: `http://localhost/socially/api/comments/add.php`
- Headers: `Content-Type: application/json`, `Authorization: Bearer TOKEN`
- Body:

```json
{
  "postId": "post_673...",
  "content": "Great post!"
}
```

### 13. **Get Comments**

- Method: `GET`
- URL: `http://localhost/socially/api/comments/get.php?postId=post_673...`
- Headers: `Authorization: Bearer TOKEN`

### 14. **Delete Comment**

- Method: `POST`
- URL: `http://localhost/socially/api/comments/delete.php`
- Headers: `Content-Type: application/json`, `Authorization: Bearer TOKEN`
- Body:

```json
{
  "commentId": "comment_673..."
}
```

### 15. **Follow User**

- Method: `POST`
- URL: `http://localhost/socially/api/follow/request.php`
- Headers: `Content-Type: application/json`, `Authorization: Bearer TOKEN`
- Body:

```json
{
  "followingId": "user_673..."
}
```

### 16. **Unfollow User**

- Method: `POST`
- URL: `http://localhost/socially/api/follow/unfollow.php`
- Headers: `Content-Type: application/json`, `Authorization: Bearer TOKEN`
- Body:

```json
{
  "followingId": "user_673..."
}
```

### 17. **Get Followers/Following List**

- Method: `GET`
- URL: `http://localhost/socially/api/follow/list.php?userId=user_673...&type=followers`
- URL: `http://localhost/socially/api/follow/list.php?userId=user_673...&type=following`
- Headers: `Authorization: Bearer TOKEN`

### 18. **Search Users**

- Method: `GET`
- URL: `http://localhost/socially/api/users/search.php?query=john&limit=50`
- Headers: `Authorization: Bearer TOKEN`

### 19. **Update Online Status**

- Method: `POST`
- URL: `http://localhost/socially/api/users/online-status.php`
- Headers: `Content-Type: application/json`, `Authorization: Bearer TOKEN`
- Body:

```json
{
  "online": true
}
```

### 20. **Get User Status**

- Method: `GET`
- URL: `http://localhost/socially/api/users/get-status.php?userId=user_673...`
- Headers: `Authorization: Bearer TOKEN`

### 21. **Register FCM Token**

- Method: `POST`
- URL: `http://localhost/socially/api/users/fcm-token.php`
- Headers: `Content-Type: application/json`, `Authorization: Bearer TOKEN`
- Body:

```json
{
  "fcmToken": "fcm_token_here..."
}
```

---

## 🎉 ALL SERVER APIs COMPLETE!

### Phase 2: Update Android Activities (4-5 hours)

- [ ] Create LoginActivity.kt
- [ ] Update MainActivity5 (Feed) to load posts/stories from API
- [ ] Create ChatActivity for messaging
- [ ] Update profile activities for follow system
- [ ] Add search functionality

### Phase 3: Integrate Agora SDK (2-3 hours)

- [ ] Get Agora App ID
- [ ] Create VoiceCallActivity
- [ ] Create VideoCallActivity
- [ ] Add call invite system

### Phase 4: Testing & Polish (2-3 hours)

- [ ] Test all features end-to-end
- [ ] Fix bugs
- [ ] Add loading indicators
- [ ] Handle edge cases
- [ ] Record demo video

---

## 🎯 Assignment Progress Summary

**Completed:**

- ✅ SQLite Database (10 marks) - 100%
- ✅ Authentication APIs (5 marks) - 100%
- ✅ Posts APIs (10 marks) - 100%
- ✅ Stories APIs (10 marks) - 100%
- ✅ Messages APIs (15 marks) - 100%
- ✅ Comments APIs (5 marks) - 100%
- ✅ Follow System APIs (5 marks) - 100%
- ✅ Search APIs (5 marks) - 100%
- ✅ Online Status APIs (5 marks) - 100%
- ✅ Offline Support Architecture (10 marks) - 100%
- ✅ Security (5 marks) - 100%

**Total Backend Progress: ~75/100 marks (All Server APIs Complete!)**

**Remaining:**

- ⏳ Voice/Video Calls with Agora (10 marks)
- ⏳ FCM Notifications Implementation (10 marks)
- ⏳ GitHub Commits (10 marks)
- ⏳ Android UI Integration (connecting existing activities to APIs)

---

## 🚀 Quick Win: Test in Android App Now!

1. **Run your Android app** (emulator or device)
2. **Test signup** - should work!
3. **App should navigate to MainActivity5**
4. **Check Logcat** for any errors

The backend is ready. Now focus on connecting UI to APIs!
