<?php
/**
 * Like/Unlike Post API Endpoint
 * POST /api/posts/like.php
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

if (empty($data->postId)) {
    http_response_code(400);
    echo json_encode(['success' => false, 'message' => 'Post ID is required']);
    exit();
}

try {
    $db->beginTransaction();
    
    // Check if already liked
    $query = "SELECT * FROM likes WHERE post_id = :post_id AND user_id = :user_id";
    $stmt = $db->prepare($query);
    $stmt->bindParam(':post_id', $data->postId);
    $stmt->bindParam(':user_id', $userId);
    $stmt->execute();
    
    if ($stmt->rowCount() > 0) {
        // Unlike
        $query = "DELETE FROM likes WHERE post_id = :post_id AND user_id = :user_id";
        $stmt = $db->prepare($query);
        $stmt->bindParam(':post_id', $data->postId);
        $stmt->bindParam(':user_id', $userId);
        $stmt->execute();
        
        // Decrease likes count
        $query = "UPDATE posts SET likes_count = likes_count - 1 WHERE post_id = :post_id";
        $stmt = $db->prepare($query);
        $stmt->bindParam(':post_id', $data->postId);
        $stmt->execute();
        
        $isLiked = false;
        $message = 'Post unliked successfully';
    } else {
        // Like
        $timestamp = round(microtime(true) * 1000);
        $query = "INSERT INTO likes (post_id, user_id, timestamp) VALUES (:post_id, :user_id, :timestamp)";
        $stmt = $db->prepare($query);
        $stmt->bindParam(':post_id', $data->postId);
        $stmt->bindParam(':user_id', $userId);
        $stmt->bindParam(':timestamp', $timestamp);
        $stmt->execute();
        
        // Increase likes count
        $query = "UPDATE posts SET likes_count = likes_count + 1 WHERE post_id = :post_id";
        $stmt = $db->prepare($query);
        $stmt->bindParam(':post_id', $data->postId);
        $stmt->execute();
        
        $isLiked = true;
        $message = 'Post liked successfully';
    }
    
    // Get updated likes count
    $query = "SELECT likes_count FROM posts WHERE post_id = :post_id";
    $stmt = $db->prepare($query);
    $stmt->bindParam(':post_id', $data->postId);
    $stmt->execute();
    $post = $stmt->fetch(PDO::FETCH_ASSOC);
    
    $db->commit();
    
    http_response_code(200);
    echo json_encode([
        'success' => true,
        'message' => $message,
        'isLiked' => $isLiked,
        'likesCount' => (int)$post['likes_count']
    ]);
    
} catch (PDOException $e) {
    $db->rollBack();
    http_response_code(500);
    echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
}
?>
