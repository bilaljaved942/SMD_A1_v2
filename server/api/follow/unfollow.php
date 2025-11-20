<?php
/**
 * Unfollow User API Endpoint
 * POST /api/follow/unfollow.php
 * Requires Authorization header
 */

header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST, DELETE");
header("Access-Control-Max-Age: 3600");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

require_once '../../config/database.php';
require_once '../../utils/auth.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST' && $_SERVER['REQUEST_METHOD'] !== 'DELETE') {
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

$followerId = Auth::getUserIdFromToken($token);

// Get data
$data = json_decode(file_get_contents("php://input"));

if (empty($data->followingId)) {
    http_response_code(400);
    echo json_encode(['success' => false, 'message' => 'Following user ID is required']);
    exit();
}

try {
    // Delete follow relationship
    $query = "DELETE FROM follows WHERE follower_id = :follower_id AND following_id = :following_id";
    $stmt = $db->prepare($query);
    $stmt->bindParam(':follower_id', $followerId);
    $stmt->bindParam(':following_id', $data->followingId);
    $stmt->execute();
    
    if ($stmt->rowCount() === 0) {
        http_response_code(404);
        echo json_encode(['success' => false, 'message' => 'Follow relationship not found']);
        exit();
    }
    
    http_response_code(200);
    echo json_encode([
        'success' => true,
        'message' => 'Unfollowed successfully'
    ]);
    
} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
}
?>
