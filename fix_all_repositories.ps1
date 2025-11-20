# Complete Repository Fix Script - Corrects all API response handling

Write-Host "Starting comprehensive repository fixes..."

# Fix MessageRepository - API returns MessageResponse directly, not wrapped
$msgRepo = "d:\7th Sem\SMD_A1_v2\app\src\main\java\com\example\firstapp\repository\MessageRepository.kt"
$content = Get-Content $msgRepo -Raw

# Fix sendMessage - response.body() IS MessageResponse
$content = $content -replace 'if \(response\.isSuccessful && response\.body\(\)\?\.success == true\) \{[^}]+val messageResponse = response\.body\(\)!!', 'if (response.isSuccessful) {
                        val messageResponse = response.body()!!'

$content = $content -replace 'val messageEntity = MessageEntity\([^)]+id = messageResponse\.message\.', 'val messageEntity = MessageEntity(
                            id = messageResponse.'

$content = $content -replace 'messageResponse\.message\.([a-zA-Z]+)', 'messageResponse.$1'

# Fix request creation - needs SendMessageRequest object
$content = $content -replace 'val response = messageApi\.sendMessage\("Bearer \$token", request\)', 'val response = messageApi.sendMessage(request)'

# Fix getUserId calls
$content = $content -replace 'val senderId = prefs\.getUserId\(\)!!', 'val senderId = prefs.getUserId() ?: ""'
$content = $content -replace 'prefs\.getAuthToken\(\)!!', 'prefs.getAuthToken() ?: ""'

# Fix deletedAt and vanishMode references (MessageResponse doesn't have these in DTO but MessageEntity does)
$content = $content -replace 'deletedAt = messageResponse\.deletedAt', 'deletedAt = null'
$content = $content -replace 'vanishMode = messageResponse\.vanishMode', 'vanishMode = vanishMode'

# Fix editMessage - needs EditMessageRequest
$content = $content -replace 'val response = messageApi\.editMessage\(messageId, newContent\)', 'val response = messageApi.editMessage(EditMessageRequest(messageId, newContent))'

# Fix deleteMessage - needs DeleteMessageRequest  
$content = $content -replace 'val response = messageApi\.deleteMessage\(messageId, senderId\)', 'val response = messageApi.deleteMessage(DeleteMessageRequest(messageId))'

# Fix DAO method calls - markAsDeleted takes 2 params
$content = $content -replace 'messageDao\.markAsDeleted\(messageId, System\.currentTimeMillis\(\)\)', 'messageDao.markMessageAsDeleted(messageId, System.currentTimeMillis())'
$content = $content -replace 'messageDao\.markAsSeen\(messageId\)', 'messageDao.markMessageAsSeen(messageId, System.currentTimeMillis())'

# Fix updateMessageContent - remove boolean param
$content = $content -replace 'messageDao\.updateMessageContent\(messageId, newContent, true, System\.currentTimeMillis\(\)\)', 'messageDao.updateMessageContent(messageId, newContent, System.currentTimeMillis())'

# Fix timestamp parameter in PendingActionEntity
$content = $content -replace 'timestamp = System\.currentTimeMillis\(\)\),', 'timestamp = System.currentTimeMillis(),'

Set-Content -Path $msgRepo -Value $content -NoNewline
Write-Host "✓ Fixed MessageRepository"

# Fix PostRepository
$postRepo = "d:\7th Sem\SMD_A1_v2\app\src\main\java\com\example\firstapp\repository\PostRepository.kt"
$content = Get-Content $postRepo -Raw

# Fix createPost - needs CreatePostRequest object and response handling
$content = $content -replace 'val response = postApi\.createPost\(userId, caption, location, mediaBase64, type\)', 'val response = postApi.createPost(CreatePostRequest(userId ?: "", caption ?: "", location ?: "", mediaBase64 ?: "", type))'

$content = $content -replace 'if \(response\.isSuccessful && response\.body\(\)\?\.success == true\) \{[^}]+val postResponse = response\.body\(\)!!', 'if (response.isSuccessful) {
                        val postResponse = response.body()!!'

$content = $content -replace 'postResponse\.post\.([a-zA-Z]+)', 'postResponse.$1'

# Fix getUserFeed - method doesn't exist, use getUserFeed  
$content = $content -replace '\.getFeed\(', '.getUserFeed('

# Fix getUserPosts - remove extra parameters
$content = $content -replace 'val response = postApi\.getUserPosts\(userId, page, limit\)', 'val response = postApi.getUserPosts(userId)'

# Fix likePost - needs correct signature
$content = $content -replace 'val response = postApi\.likePost\(postId\)', 'val response = postApi.likePost(postId, LikeRequest(postId, userId ?: ""))'

# Fix deletePost - remove userId param
$content = $content -replace 'val response = postApi\.deletePost\(postId, senderId\)', 'val response = postApi.deletePost(postId)'

# Fix deletePost DAO call - needs PostEntity not String
$content = $content -replace 'postDao\.deletePost\(postId\)', 'postDao.deletePostById(postId)'

# Fix mediaType reference (doesn't exist in PostResponse)
$content = $content -replace 'mediaType = post\.mediaType', 'mediaType = "image"'

# Fix Flow return - wrap in flow builder
$content = $content -replace '(fun observeAllPosts\(\): Flow<List<PostEntity>> =)\s+postDao\.getAllPosts\(\)', '$1 postDao.getAllPostsFlow()'
$content = $content -replace '(fun observeUserPosts\([^)]+\): Flow<List<PostEntity>> =)\s+postDao\.getPostsByUser\(', '$1 postDao.getPostsByUserFlow('

# Fix timestamp parameter
$content = $content -replace 'timestamp = [^,\)]+,', 'timestamp = System.currentTimeMillis(),'

# Fix null handling
$content = $content -replace 'val userId = prefs\.getUserId\(\)', 'val userId = prefs.getUserId() ?: ""'
$content = $content -replace 'val token = prefs\.getAuthToken\(\)', 'val token = prefs.getAuthToken() ?: ""'

Set-Content -Path $postRepo -Value $content -NoNewline
Write-Host "✓ Fixed PostRepository"

# Fix StoryRepository
$storyRepo = "d:\7th Sem\SMD_A1_v2\app\src\main\java\com\example\firstapp\repository\StoryRepository.kt"
$content = Get-Content $storyRepo -Raw

# Fix createStory
$content = $content -replace 'val response = storyApi\.createStory\(userId, mediaBase64, isVideo\)', 'val response = storyApi.createStory(CreateStoryRequest(userId ?: "", mediaBase64 ?: "", isVideo))'

$content = $content -replace 'if \(response\.isSuccessful && response\.body\(\)\?\.success == true\) \{[^}]+val storyResponse = response\.body\(\)!!', 'if (response.isSuccessful) {
                        val storyResponse = response.body()!!'

$content = $content -replace 'storyResponse\.story\.([a-zA-Z]+)', 'storyResponse.$1'

# Fix fetchStories - use correct API method
$content = $content -replace '\.fetchStories\(', '.getUserFeed('

# Fix getUserStories - remove extra params
$content = $content -replace 'val response = storyApi\.getUserStories\(userId, since\)', 'val response = storyApi.getUserStories(userId)'

# Fix isViewed reference (API uses 'viewed')
$content = $content -replace 'isViewed = story\.isViewed', 'isViewed = story.viewed'

# Fix markStoryAsViewed - only takes storyId
$content = $content -replace 'storyDao\.markStoryAsViewed\(storyId, System\.currentTimeMillis\(\)\)', 'storyDao.markStoryAsViewed(storyId)'

# Fix viewStory API call - doesn't exist
$content = $content -replace 'val response = storyApi\.viewStory\(storyId\)', '// Story view tracked locally
                        Result.success(Unit)'

# Fix deleteStory
$content = $content -replace 'val response = storyApi\.deleteStory\(storyId, senderId\)', 'val response = storyApi.deleteStory(storyId)'
$content = $content -replace 'storyDao\.deleteStory\(storyId\)', 'storyDao.deleteStoryById(storyId)'

# Fix Flow return
$content = $content -replace '(fun observeUserStories\([^)]+\): Flow<List<StoryEntity>> =)\s+storyDao\.getActiveStoriesByUser\(', '$1 storyDao.getActiveStoriesByUserFlow('

# Fix null userId
$content = $content -replace 'val userId = prefs\.getUserId\(\) \?: return', 'val userId = prefs.getUserId() ?: ""
            if (userId.isEmpty()) return'

Set-Content -Path $storyRepo -Value $content -NoNewline
Write-Host "✓ Fixed StoryRepository"

# Fix FollowRepository  
$followRepo = "d:\7th Sem\SMD_A1_v2\app\src\main\java\com\example\firstapp\repository\FollowRepository.kt"
$content = Get-Content $followRepo -Raw

# Fix followUser - needs FollowRequest object
$content = $content -replace 'val response = followApi\.follow\(followingId\)', 'val response = followApi.sendFollowRequest(FollowRequest(followerId ?: "", followingId))'
$content = $content -replace 'followApi\.followRequest\(', 'followApi.sendFollowRequest('

# Fix unfollowUser - use correct API method
$content = $content -replace 'val response = followApi\.unfollow\(followerId, followingId\)', 'val response = followApi.unfollowUser(followerId ?: "", followingId)'

# Fix deleteFollow DAO call
$content = $content -replace 'followDao\.deleteFollow\(followerId, followingId\)', 'followDao.deleteFollowRelation(followerId ?: "", followingId)'

# Fix getFollowers/getFollowing - remove extra params
$content = $content -replace 'val response = followApi\.getFollowers\(userId, null\)', 'val response = followApi.getFollowers(userId)'
$content = $content -replace 'val response = followApi\.getFollowing\(userId, null\)', 'val response = followApi.getFollowing(userId)'

# Fix UserResponse to UserEntity conversion - uid is String in API, Int in DB
$content = $content -replace '(?<=UserEntity\(\s+uid = )user\.uid,', 'user.uid.toIntOrNull() ?: 0,'

# Remove createdAt references
$content = $content -replace ',\s*createdAt = user\.createdAt', ''

# Fix getMutualFollowers - convert Int to String for API call
$content = $content -replace 'followApi\.getFollowers\(userId\)', 'followApi.getFollowers(userId.toString())'
$content = $content -replace 'followEntity\.followerId == userId', 'followEntity.followerId == userId.toString()'

# Fix Flow returns
$content = $content -replace '(fun observeFollowers\([^)]+\): Flow<List<FollowEntity>> =)\s+followDao\.getFollowers\(', '$1 followDao.getFollowingFlow('
$content = $content -replace '(fun observeFollowing\([^)]+\): Flow<List<FollowEntity>> =)\s+followDao\.getFollowing\(', '$1 followDao.getFollowingFlow('

Set-Content -Path $followRepo -Value $content -NoNewline
Write-Host "✓ Fixed FollowRepository"

# Fix UserRepository
$userRepo = "d:\7th Sem\SMD_A1_v2\app\src\main\java\com\example\firstapp\repository\UserRepository.kt"
$content = Get-Content $userRepo -Raw

# Fix getUserById - uid conversion
$content = $content -replace '(?<=UserEntity\(\s+uid = )user\.uid,', 'user.uid.toIntOrNull() ?: 0,'
$content = $content -replace ',\s*createdAt = user\.createdAt', ''

# Fix setOnlineStatus - needs OnlineStatusRequest
$content = $content -replace 'val response = userApi\.updateOnlineStatus\(userId, online\)', 'val response = userApi.setOnlineStatus(OnlineStatusRequest(userId, online))'

# Fix getUserStatus - use correct method name
$content = $content -replace 'val response = userApi\.getUserStatus\(userId\)', 'val response = userApi.getOnlineStatus(userId)'

# Fix registerFcmToken - needs FcmTokenRequest
$content = $content -replace 'val response = userApi\.registerFcmToken\(userId, fcmToken\)', 'val response = userApi.registerFcmToken(FcmTokenRequest(userId ?: "", fcmToken ?: ""))'

# Fix Flow returns
$content = $content -replace '(fun observeOnlineUsers\(\): Flow<List<UserEntity>> =)\s+userDao\.getOnlineUsers\(\)', '$1 flow { emit(userDao.getOnlineUsers()) }'
$content = $content -replace '(fun observeUserById\([^)]+\): Flow<UserEntity\?> =)\s+userDao\.getUserById\(', '$1 flow { emit(userDao.getUserById('
$content = $content -replace '(fun searchUsersLocally\([^)]+\): Flow<List<UserEntity>> =)\s+userDao\.searchUsersByName\(', '$1 flow { emit(userDao.searchUsersByName('

# Close emit calls
$content = $content -replace '(flow \{ emit\(userDao\.[a-zA-Z]+\([^)]*\))\)', '$1)) }'

Set-Content -Path $userRepo -Value $content -NoNewline
Write-Host "✓ Fixed UserRepository"

# Fix AuthRepository
$authRepo = "d:\7th Sem\SMD_A1_v2\app\src\main\java\com\example\firstapp\repository\AuthRepository.kt"
$content = Get-Content $authRepo -Raw

# Fix getUserId and getAuthToken null handling
$content = $content -replace 'val token = prefs\.getAuthToken\(\) \?: ""', 'val token = prefs.getAuthToken() ?: ""'
$content = $content -replace 'val userId = prefs\.getUserId\(\) \?: ""', 'val userId = prefs.getUserId() ?: ""'

# Fix uid conversion and createdAt removal (already done by earlier script)
# Just ensure null safety on token param order
$content = $content -replace 'prefs\.saveUserSession\(\s*authResponse\.token', 'authResponse.token?.let { token ->
                            prefs.saveUserSession(authResponse.user?.uid ?: "", authResponse.user?.email ?: "", authResponse.user?.name ?: "", token)
                        } ?: run { /* No token'

Set-Content -Path $authRepo -Value $content -NoNewline
Write-Host "✓ Fixed AuthRepository"

# Fix LoginActivity
$loginActivity = "d:\7th Sem\SMD_A1_v2\app\src\main\java\com\example\firstapp\ui\auth\LoginActivity.kt"
$content = Get-Content $loginActivity -Raw

# Fix null safety on UserResponse
$content = $content -replace 'authResponse\.user\.uid', 'authResponse.user?.uid ?: ""'
$content = $content -replace 'authResponse\.user\.email', 'authResponse.user?.email ?: ""'
$content = $content -replace 'authResponse\.user\.name', 'authResponse.user?.name ?: ""'
$content = $content -replace 'authResponse\.token', 'authResponse.token ?: ""'

Set-Content -Path $loginActivity -Value $content -NoNewline
Write-Host "Fixed LoginActivity"

Write-Host "All repository fixes complete"
