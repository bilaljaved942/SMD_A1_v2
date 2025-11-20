<?php
/**
 * Add Comment API Endpoint
 * POST /api/comments/add.php
 * Requires Authorization header
 */

header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");
header("Access-Control-Max-Age: 3600");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

require_once '../../config/database.php';
require_once '../../utils/auth.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
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

// Get POST data
$data = json_decode(file_get_contents("php://input"));

if (empty($data->postId) || empty($data->content)) {
    http_response_code(400);
    echo json_encode(['success' => false, 'message' => 'Post ID and content are required']);
    exit();
}

try {
    $db->beginTransaction();
    
    $commentId = uniqid('comment_', true);
    $timestamp = round(microtime(true) * 1000);
    
    // Insert comment
    $query = "INSERT INTO comments (comment_id, post_id, user_id, content, timestamp) 
              VALUES (:comment_id, :post_id, :user_id, :content, :timestamp)";
    
    $stmt = $db->prepare($query);
    $stmt->bindParam(':comment_id', $commentId);
    $stmt->bindParam(':post_id', $data->postId);
    $stmt->bindParam(':user_id', $userId);
    $stmt->bindParam(':content', $data->content);
    $stmt->bindParam(':timestamp', $timestamp);
    $stmt->execute();
    
    // Increment comments count on post
    $query = "UPDATE posts SET comments_count = comments_count + 1 WHERE post_id = :post_id";
    $stmt = $db->prepare($query);
    $stmt->bindParam(':post_id', $data->postId);
    $stmt->execute();
    
    // Fetch comment with user info
    $query = "SELECT c.*, u.name, u.profile_picture 
              FROM comments c 
              JOIN users u ON c.user_id = u.uid 
              WHERE c.comment_id = :comment_id";
    
    $stmt = $db->prepare($query);
    $stmt->bindParam(':comment_id', $commentId);
    $stmt->execute();
    
    $comment = $stmt->fetch(PDO::FETCH_ASSOC);
    
    $db->commit();
    
    $response = [
        'success' => true,
        'comment' => [
            'commentId' => $comment['comment_id'],
            'postId' => $comment['post_id'],
            'userId' => $comment['user_id'],
            'userName' => $comment['name'],
            'userProfilePicture' => $comment['profile_picture'],
            'content' => $comment['content'],
            'timestamp' => (int)$comment['timestamp']
        ]
    ];
    
    http_response_code(201);
    echo json_encode($response);
    
} catch (PDOException $e) {
    $db->rollBack();
    http_response_code(500);
    echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
}
?>
