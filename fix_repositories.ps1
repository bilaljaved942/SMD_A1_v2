# PowerShell script to fix all repository files

$repoPath = "d:\7th Sem\SMD_A1_v2\app\src\main\java\com\example\firstapp\repository"

# Define all the replacements needed
$replacements = @{
    '\.profile_picture' = '.profilePicture'
    '\.cover_photo'     = '.coverPhoto'
    '\.fcm_token'       = '.fcmToken'
    '\.last_online'     = '.lastOnline'
    '\.created_at'      = '.createdAt'
    '\.sender_id'       = '.senderId'
    '\.receiver_id'     = '.receiverId'
    '\.media_url'       = '.mediaUrl'
    '\.media_type'      = '.mediaType'
    '\.is_edited'       = '.isEdited'
    '\.edited_at'       = '.editedAt'
    '\.is_deleted'      = '.isDeleted'
    '\.deleted_at'      = '.deletedAt'
    '\.is_seen'         = '.isSeen'
    '\.seen_at'         = '.seenAt'
    '\.call_type'       = '.callType'
    '\.channel_name'    = '.channelName'
    '\.vanish_mode'     = '.vanishMode'
    '\.post_id'         = '.postId'
    '\.user_id'         = '.userId'
    '\.likes_count'     = '.likesCount'
    '\.comments_count'  = '.commentsCount'
    '\.expires_at'      = '.expiresAt'
    '\.is_viewed'       = '.isViewed'
    '\.is_video'        = '.isVideo'
}

# Process all repository files
Get-ChildItem -Path $repoPath -Filter "*Repository.kt" | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    
    foreach ($key in $replacements.Keys) {
        $content = $content -replace $key, $replacements[$key]
    }
    
    Set-Content -Path $_.FullName -Value $content -NoNewline
    Write-Host "Fixed: $($_.Name)"
}

Write-Host "All repository files fixed!"
