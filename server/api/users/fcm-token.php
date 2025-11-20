<?php
/**
 * Register FCM Token API Endpoint
 * POST /api/users/fcm-token.php
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

if (empty($data->fcmToken)) {
    http_response_code(400);
    echo json_encode(['success' => false, 'message' => 'FCM token is required']);
    exit();
}

try {
    // Update FCM token
    $query = "UPDATE users SET fcm_token = :fcm_token WHERE uid = :uid";
    
    $stmt = $db->prepare($query);
    $stmt->bindParam(':fcm_token', $data->fcmToken);
    $stmt->bindParam(':uid', $userId);
    $stmt->execute();
    
    http_response_code(200);
    echo json_encode([
        'success' => true,
        'message' => 'FCM token registered successfully'
    ]);
    
} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
}
?>
