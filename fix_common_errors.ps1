# Comprehensive repository fix script

$files = @{
    "AuthRepository"    = "d:\7th Sem\SMD_A1_v2\app\src\main\java\com\example\firstapp\repository\AuthRepository.kt"
    "MessageRepository" = "d:\7th Sem\SMD_A1_v2\app\src\main\java\com\example\firstapp\repository\MessageRepository.kt"
    "PostRepository"    = "d:\7th Sem\SMD_A1_v2\app\src\main\java\com\example\firstapp\repository\PostRepository.kt"
    "StoryRepository"   = "d:\7th Sem\SMD_A1_v2\app\src\main\java\com\example\firstapp\repository\StoryRepository.kt"
    "FollowRepository"  = "d:\7th Sem\SMD_A1_v2\app\src\main\java\com\example\firstapp\repository\FollowRepository.kt"
    "UserRepository"    = "d:\7th Sem\SMD_A1_v2\app\src\main\java\com\example\firstapp\repository\UserRepository.kt"
    "LoginActivity"     = "d:\7th Sem\SMD_A1_v2\app\src\main\java\com\example\firstapp\ui\auth\LoginActivity.kt"
}

# Fix common issues across all files
foreach ($key in $files.Keys) {
    $file = $files[$key]
    if (Test-Path $file) {
        $content = Get-Content $file -Raw
        
        # Fix getUserId() null handling
        $content = $content -replace 'prefs\.getUserId\(\)', 'prefs.getUserId() ?: ""'
        $content = $content -replace 'securePreferences\.getUserId\(\)', 'securePreferences.getUserId() ?: ""'
        $content = $content -replace 'securePrefs\.getUserId\(\)', 'securePrefs.getUserId() ?: ""'
        
        # Fix getAuthToken() null handling  
        $content = $content -replace '(?<!?: )prefs\.getAuthToken\(\)(?! \?)', 'prefs.getAuthToken() ?: ""'
        
        # Fix uid String to Int conversion for UserEntity
        $content = $content -replace '(?<=uid = )([a-zA-Z\.]+)\.uid,', '$1.uid.toIntOrNull() ?: 0,'
        $content = $content -replace '(?<=uid = )user\.uid(?!\.)', 'user.uid.toIntOrNull() ?: 0'
        
        # Remove createdAt from UserEntity (not in UserResponse)
        $content = $content -replace ',\s*createdAt = [^,\n]+(?=\s*\))', ''
        $content = $content -replace 'createdAt = [^,\n]+,\s*', ''
        
        # Fix timestamp parameter name (should be createdAt for FollowEntity but doesn't exist in API)
        $content = $content -replace 'timestamp = [^,\)]+', 'timestamp = System.currentTimeMillis()'
        
        # Fix Flow return types - wrap suspend calls in flow { }
        $content = $content -replace '(?s)(fun observe\w+\([^\)]*\): Flow<[^>]+> =\s+)([a-zA-Z]+Dao\.[a-zA-Z]+\([^\)]*\))(?!\s*\{)', '$1flow { emit($2) }'
        
        Set-Content -Path $file -Value $content -NoNewline
        Write-Host "Fixed common issues in: $key"
    }
}

Write-Host "Common fixes complete!"
