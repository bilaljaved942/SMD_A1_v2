<?php
/**
 * Delete Comment API Endpoint
 * DELETE /api/comments/delete.php
 * Requires Authorization header
 */

header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: DELETE, POST");
header("Access-Control-Max-Age: 3600");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

require_once '../../config/database.php';
require_once '../../utils/auth.php';

if ($_SERVER['REQUEST_METHOD'] !== 'DELETE' && $_SERVER['REQUEST_METHOD'] !== 'POST') {
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

// Get data
$data = json_decode(file_get_contents("php://input"));

if (empty($data->commentId)) {
    http_response_code(400);
    echo json_encode(['success' => false, 'message' => 'Comment ID is required']);
    exit();
}

try {
    $db->beginTransaction();
    
    // Check if comment belongs to user
    $query = "SELECT post_id FROM comments WHERE comment_id = :comment_id AND user_id = :user_id";
    $stmt = $db->prepare($query);
    $stmt->bindParam(':comment_id', $data->commentId);
    $stmt->bindParam(':user_id', $userId);
    $stmt->execute();
    
    if ($stmt->rowCount() === 0) {
        http_response_code(403);
        echo json_encode(['success' => false, 'message' => 'You can only delete your own comments']);
        exit();
    }
    
    $comment = $stmt->fetch(PDO::FETCH_ASSOC);
    $postId = $comment['post_id'];
    
    // Delete comment
    $query = "DELETE FROM comments WHERE comment_id = :comment_id";
    $stmt = $db->prepare($query);
    $stmt->bindParam(':comment_id', $data->commentId);
    $stmt->execute();
    
    // Decrement comments count
    $query = "UPDATE posts SET comments_count = GREATEST(0, comments_count - 1) WHERE post_id = :post_id";
    $stmt = $db->prepare($query);
    $stmt->bindParam(':post_id', $postId);
    $stmt->execute();
    
    $db->commit();
    
    http_response_code(200);
    echo json_encode([
        'success' => true,
        'message' => 'Comment deleted successfully'
    ]);
    
} catch (PDOException $e) {
    $db->rollBack();
    http_response_code(500);
    echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
}
?>
