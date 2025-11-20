<?php
/**
 * Get Feed API Endpoint
 * GET /api/posts/feed.php?page=1&limit=20
 * Requires Authorization header
 */

header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: GET");
header("Access-Control-Max-Age: 3600");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

require_once '../../config/database.php';
require_once '../../utils/auth.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    http_response_code(405);
    echo json_encode(['success' => false, 'message' => 'Method not allowed']);
    exit();
}

$database = new Database();
$db = $database->getConnection();

// Validate authorization
$authHeader = Auth::getAuthorizationHeader();
if (empty($authHeader)) {
    http_response_code(401);
    echo json_encode(['success' => false, 'message' => 'No authorization token provided']);
    exit();
}

$token = str_replace('Bearer ', '', $authHeader);
if (!Auth::validateToken($token)) {
    http_response_code(401);
    echo json_encode(['success' => false, 'message' => 'Invalid token format']);
    exit();
}

$userId = Auth::getUserIdFromToken($token);

// Pagination parameters
$page = isset($_GET['page']) ? (int)$_GET['page'] : 1;
$limit = isset($_GET['limit']) ? (int)$_GET['limit'] : 20;
$offset = ($page - 1) * $limit;

try {
    // Fetch posts from users the current user follows + own posts
    $query = "SELECT p.*, u.name, u.profile_picture,
              EXISTS(SELECT 1 FROM likes WHERE post_id = p.post_id AND user_id = :current_user) as is_liked
              FROM posts p 
              JOIN users u ON p.user_id = u.uid 
              WHERE p.user_id = :current_user 
                 OR p.user_id IN (SELECT following_id FROM follows WHERE follower_id = :current_user AND status = 'accepted')
              ORDER BY p.timestamp DESC 
              LIMIT :limit OFFSET :offset";
    
    $stmt = $db->prepare($query);
    $stmt->bindParam(':current_user', $userId);
    $stmt->bindParam(':limit', $limit, PDO::PARAM_INT);
    $stmt->bindParam(':offset', $offset, PDO::PARAM_INT);
    $stmt->execute();
    
    $posts = [];
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        $posts[] = [
            'postId' => $row['post_id'],
            'userId' => $row['user_id'],
            'userName' => $row['name'],
            'userProfilePicture' => $row['profile_picture'],
            'mediaUrl' => $row['media_url'],
            'mediaType' => $row['media_type'],
            'caption' => $row['caption'],
            'location' => $row['location'],
            'timestamp' => (int)$row['timestamp'],
            'likesCount' => (int)$row['likes_count'],
            'commentsCount' => (int)$row['comments_count'],
            'isLiked' => (bool)$row['is_liked']
        ];
    }
    
    http_response_code(200);
    echo json_encode([
        'success' => true,
        'posts' => $posts,
        'page' => $page,
        'limit' => $limit
    ]);
    
} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
}
?>
