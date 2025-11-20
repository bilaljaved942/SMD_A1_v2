<?php
/**
 * Logout API Endpoint
 * POST /api/auth/logout
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

// Get authorization header
$authHeader = Auth::getAuthorizationHeader();

if (empty($authHeader)) {
    http_response_code(401);
    echo json_encode(['success' => false, 'message' => 'No authorization token provided']);
    exit();
}

// Extract token
$token = str_replace('Bearer ', '', $authHeader);

// Validate token
if (!Auth::validateToken($token)) {
    http_response_code(401);
    echo json_encode(['success' => false, 'message' => 'Invalid token format']);
    exit();
}

// Get user ID from token
$userId = Auth::getUserIdFromToken($token);

try {
    // Update user online status to false
    $timestamp = round(microtime(true) * 1000);
    $query = "UPDATE users SET online = false, last_online = :timestamp WHERE uid = :uid";
    
    $stmt = $db->prepare($query);
    $stmt->bindParam(':timestamp', $timestamp);
    $stmt->bindParam(':uid', $userId);
    
    if ($stmt->execute()) {
        http_response_code(200);
        echo json_encode(['success' => true, 'message' => 'Logged out successfully']);
    } else {
        http_response_code(500);
        echo json_encode(['success' => false, 'message' => 'Failed to logout']);
    }
    
} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
}
?>
