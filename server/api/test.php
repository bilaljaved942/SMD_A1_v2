<?php
/**
 * Simple Test Endpoint
 * GET /api/test.php
 * No authentication required - just to verify server is working
 */

header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");

$response = [
    'success' => true,
    'message' => 'Socially API is running successfully!',
    'timestamp' => time(),
    'server' => 'Apache/PHP',
    'total_endpoints' => 21,
    'endpoints' => [
        'auth' => [
            'signup' => 'POST /api/auth/signup.php',
            'login' => 'POST /api/auth/login.php',
            'logout' => 'POST /api/auth/logout.php (requires auth)',
            'session' => 'GET /api/auth/session.php (requires auth)'
        ],
        'posts' => [
            'create' => 'POST /api/posts/create.php (requires auth)',
            'feed' => 'GET /api/posts/feed.php?page=1&limit=20 (requires auth)',
            'like' => 'POST /api/posts/like.php (requires auth)'
        ],
        'stories' => [
            'create' => 'POST /api/stories/create.php (requires auth)',
            'fetch' => 'GET /api/stories/fetch.php (requires auth)'
        ],
        'messages' => [
            'send' => 'POST /api/messages/send.php (requires auth)',
            'fetch' => 'GET /api/messages/fetch.php?otherUserId=xxx (requires auth)'
        ],
        'comments' => [
            'add' => 'POST /api/comments/add.php (requires auth)',
            'get' => 'GET /api/comments/get.php?postId=xxx (requires auth)',
            'delete' => 'POST /api/comments/delete.php (requires auth)'
        ],
        'follow' => [
            'request' => 'POST /api/follow/request.php (requires auth)',
            'unfollow' => 'POST /api/follow/unfollow.php (requires auth)',
            'list' => 'GET /api/follow/list.php?userId=xxx&type=followers (requires auth)'
        ],
        'users' => [
            'search' => 'GET /api/users/search.php?query=john (requires auth)',
            'online_status' => 'POST /api/users/online-status.php (requires auth)',
            'get_status' => 'GET /api/users/get-status.php?userId=xxx (requires auth)',
            'fcm_token' => 'POST /api/users/fcm-token.php (requires auth)'
        ]
    ],
    'database_status' => checkDatabase(),
    'upload_directories' => checkUploadDirs()
];

function checkDatabase() {
    try {
        require_once '../config/database.php';
        $database = new Database();
        $db = $database->getConnection();
        
        $query = "SELECT COUNT(*) as user_count FROM users";
        $stmt = $db->prepare($query);
        $stmt->execute();
        $result = $stmt->fetch(PDO::FETCH_ASSOC);
        
        return [
            'connected' => true,
            'total_users' => (int)$result['user_count']
        ];
    } catch (Exception $e) {
        return [
            'connected' => false,
            'error' => $e->getMessage()
        ];
    }
}

function checkUploadDirs() {
    $dirs = ['posts', 'stories', 'messages', 'profiles'];
    $status = [];
    
    foreach ($dirs as $dir) {
        $path = "../../uploads/$dir";
        $status[$dir] = [
            'exists' => file_exists($path),
            'writable' => is_writable($path)
        ];
    }
    
    return $status;
}

echo json_encode($response, JSON_PRETTY_PRINT);
?>
