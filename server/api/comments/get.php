<?php
/**
 * Get Comments API Endpoint
 * GET /api/comments/get.php?postId=xxx
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

if (empty($_GET['postId'])) {
    http_response_code(400);
    echo json_encode(['success' => false, 'message' => 'Post ID is required']);
    exit();
}

$postId = $_GET['postId'];

try {
    // Fetch comments for post
    $query = "SELECT c.*, u.name, u.profile_picture 
              FROM comments c 
              JOIN users u ON c.user_id = u.uid 
              WHERE c.post_id = :post_id 
              ORDER BY c.timestamp DESC";
    
    $stmt = $db->prepare($query);
    $stmt->bindParam(':post_id', $postId);
    $stmt->execute();
    
    $comments = [];
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        $comments[] = [
            'commentId' => $row['comment_id'],
            'postId' => $row['post_id'],
            'userId' => $row['user_id'],
            'userName' => $row['name'],
            'userProfilePicture' => $row['profile_picture'],
            'content' => $row['content'],
            'timestamp' => (int)$row['timestamp']
        ];
    }
    
    http_response_code(200);
    echo json_encode([
        'success' => true,
        'comments' => $comments
    ]);
    
} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
}
?>
