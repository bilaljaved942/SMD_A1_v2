<?php
/**
 * Follow Request API Endpoint
 * POST /api/follow/request.php
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

$followerId = Auth::getUserIdFromToken($token);

// Get POST data
$data = json_decode(file_get_contents("php://input"));

if (empty($data->followingId)) {
    http_response_code(400);
    echo json_encode(['success' => false, 'message' => 'Following user ID is required']);
    exit();
}

if ($followerId === $data->followingId) {
    http_response_code(400);
    echo json_encode(['success' => false, 'message' => 'Cannot follow yourself']);
    exit();
}

try {
    // Check if already following or request exists
    $query = "SELECT * FROM follows WHERE follower_id = :follower_id AND following_id = :following_id";
    $stmt = $db->prepare($query);
    $stmt->bindParam(':follower_id', $followerId);
    $stmt->bindParam(':following_id', $data->followingId);
    $stmt->execute();
    
    if ($stmt->rowCount() > 0) {
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'Follow request already exists']);
        exit();
    }
    
    $timestamp = round(microtime(true) * 1000);
    $status = 'accepted'; // Auto-accept for now, can be 'pending' if you want approval system
    
    // Insert follow relationship
    $query = "INSERT INTO follows (follower_id, following_id, status, timestamp) 
              VALUES (:follower_id, :following_id, :status, :timestamp)";
    
    $stmt = $db->prepare($query);
    $stmt->bindParam(':follower_id', $followerId);
    $stmt->bindParam(':following_id', $data->followingId);
    $stmt->bindParam(':status', $status);
    $stmt->bindParam(':timestamp', $timestamp);
    $stmt->execute();
    
    http_response_code(201);
    echo json_encode([
        'success' => true,
        'message' => 'Follow request sent successfully',
        'status' => $status
    ]);
    
} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
}
?>
