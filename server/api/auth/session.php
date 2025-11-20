<?php
/**
 * Session Check API Endpoint
 * GET /api/auth/session
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

// Get authorization header
$authHeader = Auth::getAuthorizationHeader();

if (empty($authHeader)) {
    http_response_code(401);
    echo json_encode(['success' => false, 'isLoggedIn' => false, 'message' => 'No authorization token provided']);
    exit();
}

// Extract token (remove "Bearer " prefix if present)
$token = str_replace('Bearer ', '', $authHeader);

// Validate token format
if (!Auth::validateToken($token)) {
    http_response_code(401);
    echo json_encode(['success' => false, 'isLoggedIn' => false, 'message' => 'Invalid token format']);
    exit();
}

// Get user ID from token
$userId = Auth::getUserIdFromToken($token);

try {
    // Fetch user data
    $query = "SELECT uid, name, email, profile_picture, cover_photo, bio, online, last_online 
              FROM users WHERE uid = :uid";
    
    $stmt = $db->prepare($query);
    $stmt->bindParam(':uid', $userId);
    $stmt->execute();
    
    if ($stmt->rowCount() === 0) {
        http_response_code(401);
        echo json_encode(['success' => false, 'isLoggedIn' => false, 'message' => 'User not found']);
        exit();
    }
    
    $user = $stmt->fetch(PDO::FETCH_ASSOC);
    
    // Prepare response
    $response = [
        'success' => true,
        'isLoggedIn' => true,
        'user' => [
            'uid' => $user['uid'],
            'name' => $user['name'],
            'email' => $user['email'],
            'profilePicture' => $user['profile_picture'],
            'coverPhoto' => $user['cover_photo'],
            'bio' => $user['bio'],
            'online' => (bool)$user['online'],
            'lastOnline' => (int)$user['last_online']
        ]
    ];
    
    http_response_code(200);
    echo json_encode($response);
    
} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
}
?>
