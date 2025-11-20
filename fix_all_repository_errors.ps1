# PowerShell script to fix all repository errors systematically
$ErrorActionPreference = "Continue"
$repoPath = "D:\7th Sem\SMD_A1_v2\app\src\main\java\com\example\firstapp\repository"

Write-Host "Starting comprehensive repository fixes..." -ForegroundColor Green

# Fix AuthRepository
Write-Host "`nFixing AuthRepository.kt..." -ForegroundColor Yellow
$authFile = Join-Path $repoPath "AuthRepository.kt"
$content = Get-Content $authFile -Raw

# Fix: authResponse.token (can be null) - add !! or handle null
$content = $content -replace 'prefs\.saveUserSession\(\s*authResponse\.token,', 'prefs.saveUserSession(authResponse.token!!,'

Set-Content -Path $authFile -Value $content -NoNewline
Write-Host "Fixed AuthRepository.kt" -ForegroundColor Green

# Fix FollowRepository
Write-Host "`nFixing FollowRepository.kt..." -ForegroundColor Yellow
$followFile = Join-Path $repoPath "FollowRepository.kt"
$content = Get-Content $followFile -Raw

# Fix: FollowRequest needs both followerId and followingId
$content = $content -replace 'val request = FollowRequest\(followingId\)', 'val request = FollowRequest(followerId, followingId)'

# Fix: API call method name (followRequest should be sendFollowRequest)
$content = $content -replace 'followApi\.followRequest\(', 'followApi.sendFollowRequest('

# Fix: Remove timestamp parameter from FollowEntity constructor (doesn't exist)
$content = $content -replace '(\s+status\s*=\s*"[^"]+",)\s*timestamp\s*=\s*System\.currentTimeMillis\(\),', '$1'

# Fix: unfollow method call (should be unfollowUser)
$content = $content -replace 'followApi\.unfollow\(', 'followApi.unfollowUser('

# Fix: deleteFollow needs FollowEntity, not two strings - use deleteFollowRelation
$content = $content -replace 'followDao\.deleteFollow\(followerId,\s*followingId\)', 'followDao.deleteFollowRelation(followerId, followingId)'

# Fix: getFollowers and getFollowing API calls - remove extra token parameter
$content = $content -replace 'followApi\.getFollowers\("Bearer \$token",\s*userId\)', 'followApi.getFollowers(userId)'
$content = $content -replace 'followApi\.getFollowing\("Bearer \$token",\s*userId\)', 'followApi.getFollowing(userId)'

# Fix: Type mismatch in isFollowing - currentUserId is Int but comparing with String
$content = $content -replace 'if \(followers\.any \{ it\.uid == currentUserId \}\)', 'if (followers.any { it.uid.toString() == currentUserId })'

# Fix: Flow return type issues - wrap DAO calls in flow {}
$content = $content -replace '(fun getFollowersFlow\(userId: String\): Flow<List<FollowEntity>> \{\s*return) followDao\.getFollowers\(userId\)', '$1 flow { emit(followDao.getFollowers(userId)) }'
$content = $content -replace '(fun getFollowingFlow\(userId: String\): Flow<List<FollowEntity>> \{\s*return) followDao\.getFollowing\(userId\)', '$1 flow { emit(followDao.getFollowing(userId)) }'

Set-Content -Path $followFile -Value $content -NoNewline
Write-Host "Fixed FollowRepository.kt" -ForegroundColor Green

# Fix MessageRepository
Write-Host "`nFixing MessageRepository.kt..." -ForegroundColor Yellow
$messageFile = Join-Path $repoPath "MessageRepository.kt"
$content = Get-Content $messageFile -Raw

# Fix: SendMessageRequest needs senderId parameter
$content = $content -replace 'val request = SendMessageRequest\(\s*receiverId\s*=\s*receiverId,', 'val request = SendMessageRequest(senderId = senderId, receiverId = receiverId,'

# Fix: sendMessage API doesn't need Bearer token, just request
$content = $content -replace 'messageApi\.sendMessage\(request\)', 'messageApi.sendMessage(request)'

# Fix: MessageResponse fields are direct, not nested under .message
$content = $content -replace 'id\s*=\s*messageResponse\.receiverId,', 'id = messageResponse.id, senderId = messageResponse.senderId, receiverId = messageResponse.receiverId,'

# Fix: Remove timestamp parameter from PendingActionEntity (doesn't have it)
$content = $content -replace '(\s+payload\s*=\s*payload,)\s*timestamp\s*=\s*System\.currentTimeMillis\(\),', '$1'

# Fix: Add token parameter back to API calls
$content = $content -replace 'prefs\.getAuthToken\(\) \?\: ""', 'prefs.getAuthToken() ?: return@withContext Result.failure(Exception("Not logged in"))'

# Fix: fetchMessages needs senderId parameter
$content = $content -replace 'val response = messageApi\.fetchMessages\(receiverId,', 'val response = messageApi.fetchMessages(senderId, receiverId,'

# Fix: MessageResponse doesn't have deletedAt or vanishMode fields
$content = $content -replace 'deletedAt\s*=\s*messageResponse\.deletedAt,', 'deletedAt = null,'
$content = $content -replace 'vanishMode\s*=\s*messageResponse\.vanishMode,', 'vanishMode = vanishMode,'

# Fix: editMessage and deleteMessage take request objects
$content = $content -replace 'messageApi\.editMessage\("Bearer \$token",\s*messageId,\s*newContent\)', 'messageApi.editMessage(EditMessageRequest(messageId, newContent))'
$content = $content -replace 'messageApi\.deleteMessage\("Bearer \$token",\s*messageId\)', 'messageApi.deleteMessage(DeleteMessageRequest(messageId))'

# Fix: DAO method names - markAsSeen should be markMessageAsSeen
$content = $content -replace 'messageDao\.markAsSeen\(', 'messageDao.markMessageAsSeen('
$content = $content -replace 'messageDao\.markAsDeleted\(', 'messageDao.markMessageAsDeleted('

Set-Content -Path $messageFile -Value $content -NoNewline
Write-Host "Fixed MessageRepository.kt" -ForegroundColor Green

# Fix PostRepository
Write-Host "`nFixing PostRepository.kt..." -ForegroundColor Yellow
$postFile = Join-Path $repoPath "PostRepository.kt"
$content = Get-Content $postFile -Raw

# Fix: CreatePostRequest constructor parameters (userId, caption, location, mediaBase64, mediaType)
$content = $content -replace 'val request = CreatePostRequest\(caption,\s*mediaBase64,\s*location\)', 'val request = CreatePostRequest(userId, caption, location, mediaBase64, if (mediaBase64?.startsWith("data:video") == true) "video" else "image")'

# Fix: Remove Bearer token from createPost call
$content = $content -replace 'postApi\.createPost\("Bearer \$token",\s*request\)', 'postApi.createPost(request)'

# Fix: PostResponse fields are direct, not nested under .post or .success
$content = $content -replace 'response\.body\(\)\?\.success == true', 'response.isSuccessful'
$content = $content -replace 'postResponse\.post\.', 'postResponse.'

# Fix: Remove .message from error handling
$content = $content -replace 'response\.body\(\)\?\.message \?\:', '"Failed to create post"'

# Fix: Remove timestamp parameter from PendingActionEntity
$content = $content -replace '(\s+payload\s*=\s*payload,)\s*timestamp\s*=\s*System\.currentTimeMillis\(\),', '$1'

# Fix: Add missing imports for EditMessageRequest, DeleteMessageRequest if needed
# Fix: getUserFeed should be called correctly
$content = $content -replace 'postApi\.getFeed\(', 'postApi.getUserFeed('

# Fix: PostResponse.mediaType doesn't exist
$content = $content -replace 'mediaType\s*=\s*postResponse\.mediaType,', 'mediaType = if (postResponse.mediaUrl?.contains("video") == true) "video" else "image",'

# Fix: Remove page and limit from getUserPosts call
$content = $content -replace 'postApi\.getUserPosts\(userId,\s*page,\s*limit\)', 'postApi.getUserPosts(userId)'

# Fix: likePost takes postId and LikeRequest
$content = $content -replace 'val response = postApi\.likePost\(postId,', 'val request = LikeRequest(postId, userId); val response = postApi.likePost(postId, request'

# Fix: Remove timestamp from PendingActionEntity
# Already done above

# Fix: deletePost API takes only postId
$content = $content -replace 'postApi\.deletePost\("Bearer \$token",\s*postId\)', 'postApi.deletePost(postId)'

# Fix: postDao.deletePost takes PostEntity, use deletePostById instead
$content = $content -replace 'postDao\.deletePost\(postId\)', 'postDao.deletePostById(postId)'

# Fix: Flow return types - wrap DAO calls
$content = $content -replace '(fun getAllPostsFlow\(\): Flow<List<PostEntity>> \{\s*return) postDao\.getAllPosts\(\)', '$1 postDao.getAllPostsFlow()'
$content = $content -replace '(fun getUserPostsFlow\(userId: String\): Flow<List<PostEntity>> \{\s*return) postDao\.getPostsByUser\(userId\)', '$1 postDao.getPostsByUserFlow(userId)'

Set-Content -Path $postFile -Value $content -NoNewline
Write-Host "Fixed PostRepository.kt" -ForegroundColor Green

# Fix StoryRepository
Write-Host "`nFixing StoryRepository.kt..." -ForegroundColor Yellow
$storyFile = Join-Path $repoPath "StoryRepository.kt"
$content = Get-Content $storyFile -Raw

# Fix: CreateStoryRequest needs userId and mediaBase64
$content = $content -replace 'val request = CreateStoryRequest\(mediaBase64,', 'val request = CreateStoryRequest(userId, mediaBase64,'

# Fix: Remove Bearer token from createStory
$content = $content -replace 'storyApi\.createStory\("Bearer \$token",\s*request\)', 'storyApi.createStory(request)'

# Fix: Remove .message from error
$content = $content -replace 'response\.body\(\)\?\.message \?\:', '"Failed to create story"'

# Fix: Remove timestamp from PendingActionEntity
$content = $content -replace '(\s+payload\s*=\s*payload,)\s*timestamp\s*=\s*System\.currentTimeMillis\(\),', '$1'

# Fix: mediaBase64 can be null in offline mode, handle it
$content = $content -replace 'mediaBase64\s*=\s*null,', 'mediaBase64 = null,'

# Fix: getUserFeed should be fetchActiveStories
$content = $content -replace 'storyApi\.getUserFeed\(', 'storyApi.fetchActiveStories('

# Fix: StoryResponse doesn't have isViewed, it has viewed
$content = $content -replace 'viewed\s*=\s*storyResponse\.isViewed', 'viewed = storyResponse.viewed'

# Fix: Remove page and limit from getUserStories
$content = $content -replace 'storyApi\.getUserStories\(userId,\s*page,\s*limit\)', 'storyApi.getUserStories(userId)'

# Fix: markStoryAsViewed DAO method takes only storyId
$content = $content -replace 'storyDao\.markStoryAsViewed\(storyId,\s*userId\)', 'storyDao.markStoryAsViewed(storyId)'

# Fix: viewStory should be markStoryAsViewed in API
$content = $content -replace 'storyApi\.viewStory\(', 'storyApi.markStoryAsViewed('

# Fix: deleteStory API takes only storyId
$content = $content -replace 'storyApi\.deleteStory\("Bearer \$token",\s*storyId\)', 'storyApi.deleteStory(storyId)'

# Fix: Flow return type
$content = $content -replace '(fun getActiveStoriesFlow\(userId: String\): Flow<List<StoryEntity>> \{\s*return) storyDao\.getActiveStoriesByUser\(userId, System\.currentTimeMillis\(\)\)', '$1 flow { emit(storyDao.getActiveStoriesByUser(userId, System.currentTimeMillis())) }'

Set-Content -Path $storyFile -Value $content -NoNewline
Write-Host "Fixed StoryRepository.kt" -ForegroundColor Green

# Fix UserRepository
Write-Host "`nFixing UserRepository.kt..." -ForegroundColor Yellow
$userFile = Join-Path $repoPath "UserRepository.kt"
if (Test-Path $userFile) {
    $content = Get-Content $userFile -Raw
    
    # Fix: setOnlineStatus takes OnlineStatusRequest
    $content = $content -replace 'val response = userApi\.setOnlineStatus\(userId,', 'val request = OnlineStatusRequest(userId, online); val response = userApi.setOnlineStatus(request'
    
    # Fix: updateOnlineStatus should be setOnlineStatus
    $content = $content -replace 'userApi\.updateOnlineStatus\(', 'userApi.setOnlineStatus('
    
    # Fix: getUserStatus should be getOnlineStatus
    $content = $content -replace 'userApi\.getUserStatus\(', 'userApi.getOnlineStatus('
    
    # Fix: registerFcmToken takes FcmTokenRequest
    $content = $content -replace 'val response = userApi\.registerFcmToken\(userId,', 'val request = FcmTokenRequest(userId, token); val response = userApi.registerFcmToken(request'
    
    # Fix: Flow return types
    $content = $content -replace '(fun getOnlineUsersFlow\(\): Flow<List<UserEntity>> \{\s*return) userDao\.getOnlineUsers\(\)', '$1 flow { emit(userDao.getOnlineUsers()) }'
    $content = $content -replace '(fun getUserByIdFlow\(userId: String\): Flow<UserEntity\?> \{\s*return) userDao\.getUserById\(userId\)', '$1 flow { emit(userDao.getUserById(userId)) }'
    $content = $content -replace '(fun searchUsersFlow\(query: String\): Flow<List<UserEntity>> \{\s*return) userDao\.searchUsersByName\(query\)', '$1 flow { emit(userDao.searchUsersByName(query)) }'
    
    Set-Content -Path $userFile -Value $content -NoNewline
    Write-Host "Fixed UserRepository.kt" -ForegroundColor Green
}
else {
    Write-Host "UserRepository.kt not found" -ForegroundColor Red
}

Write-Host "`nAll repository fixes completed!" -ForegroundColor Green
Write-Host "Please review the changes and run Gradle sync." -ForegroundColor Cyan
