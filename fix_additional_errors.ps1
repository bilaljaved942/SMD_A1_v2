# Additional fixes for specific issues
$ErrorActionPreference = "Continue"
$repoPath = "D:\7th Sem\SMD_A1_v2\app\src\main\java\com\example\firstapp\repository"

Write-Host "Applying additional targeted fixes..." -ForegroundColor Green

# Fix MessageRepository - remove .message from error
$messageFile = Join-Path $repoPath "MessageRepository.kt"
$content = Get-Content $messageFile -Raw
$content = $content -replace 'response\.body\(\)\?\.message \?\: "Failed to send message"', '"Failed to send message"'
Set-Content -Path $messageFile -Value $content -NoNewline
Write-Host "Fixed MessageRepository error handling" -ForegroundColor Green

# Fix PostRepository - specific issues
$postFile = Join-Path $repoPath "PostRepository.kt"
$content = Get-Content $postFile -Raw

# Fix toggleLike method - LikeRequest needs userId and postId, API doesn't need Bearer token
$content = $content -replace 'val token = prefs\.getAuthToken\(\)!!\s*val request = LikeRequest\(postId\)', 'val userId = prefs.getUserId() ?: return@withContext Result.failure(Exception("Not logged in")); val request = LikeRequest(postId, userId)'
$content = $content -replace 'postApi\.unlikePost\("Bearer \$token",\s*request\)', 'postApi.unlikePost(postId, userId)'  
$content = $content -replace 'postApi\.likePost\("Bearer \$token",\s*request\)', 'postApi.likePost(postId, request)'

Set-Content -Path $postFile -Value $content -NoNewline
Write-Host "Fixed PostRepository toggleLike method" -ForegroundColor Green

Write-Host "`nAdditional fixes completed!" -ForegroundColor Green
