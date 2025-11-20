# PowerShell script to fix type conversion and null safety issues

$repoPath = "d:\7th Sem\SMD_A1_v2\app\src\main\java\com\example\firstapp\repository"

# Fix AuthRepository
$authFile = Join-Path $repoPath "AuthRepository.kt"
$content = Get-Content $authFile -Raw

# Fix uid type conversion and null safety
$content = $content -replace 'uid = authResponse\.user\.uid,', 'uid = authResponse.user?.uid?.toIntOrNull() ?: 0,'
$content = $content -replace 'name = authResponse\.user\.name,', 'name = authResponse.user?.name ?: "",'
$content = $content -replace 'email = authResponse\.user\.email,', 'email = authResponse.user?.email ?: "",'
$content = $content -replace 'profilePictureBase64 = authResponse\.user\.profilePicture,', 'profilePictureBase64 = authResponse.user?.profilePicture,'
$content = $content -replace 'coverPhotoBase64 = authResponse\.user\.coverPhoto,', 'coverPhotoBase64 = authResponse.user?.coverPhoto,'
$content = $content -replace 'bio = authResponse\.user\.bio,', 'bio = authResponse.user?.bio,'
$content = $content -replace 'fcmToken = authResponse\.user\.fcmToken,', 'fcmToken = authResponse.user?.fcmToken,'
$content = $content -replace 'online = authResponse\.user\.online,', 'online = authResponse.user?.online ?: false,'
$content = $content -replace 'lastOnline = authResponse\.user\.lastOnline,', 'lastOnline = authResponse.user?.lastOnline ?: 0L,'
$content = $content -replace 'createdAt = authResponse\.user\.createdAt,', 'createdAt = System.currentTimeMillis(),'

# Fix saveUserSession parameter order
$content = $content -replace 'prefs\.saveUserSession\(\s+authResponse\.token,\s+authResponse\.user\.uid,\s+authResponse\.user\.email,\s+authResponse\.user\.name\s+\)', 'authResponse.user?.uid?.let { uid ->`n                            authResponse.token?.let { token ->`n                                prefs.saveUserSession(uid, authResponse.user.email, authResponse.user.name, token)`n                            }`n                        }'

# Fix update profile uid conversion
$content = $content -replace 'uid = userEntity\.uid,', 'uid = userEntity.uid.toString(),'

Set-Content -Path $authFile -Value $content -NoNewline
Write-Host "Fixed: AuthRepository.kt"
