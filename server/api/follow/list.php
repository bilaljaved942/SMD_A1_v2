<?php
/**
 * Get Followers/Following List API Endpoint
 * GET /api/follow/list.php?userId=xxx&type=followers
 * GET /api/follow/list.php?userId=xxx&type=following
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

if (empty($_GET['userId']) || empty($_GET['type'])) {
    http_response_code(400);
    echo json_encode(['success' => false, 'message' => 'User ID and type (followers/following) are required']);
    exit();
}

$userId = $_GET['userId'];
$type = $_GET['type'];

try {
    if ($type === 'followers') {
        // Get followers
        $query = "SELECT u.uid, u.name, u.profile_picture, u.bio 
                  FROM users u 
                  JOIN follows f ON u.uid = f.follower_id 
                  WHERE f.following_id = :user_id AND f.status = 'accepted'
                  ORDER BY f.timestamp DESC";
    } elseif ($type === 'following') {
        // Get following
        $query = "SELECT u.uid, u.name, u.profile_picture, u.bio 
                  FROM users u 
                  JOIN follows f ON u.uid = f.following_id 
                  WHERE f.follower_id = :user_id AND f.status = 'accepted'
                  ORDER BY f.timestamp DESC";
    } else {
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'Invalid type. Use followers or following']);
        exit();
    }
    
    $stmt = $db->prepare($query);
    $stmt->bindParam(':user_id', $userId);
    $stmt->execute();
    
    $users = [];
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        $users[] = [
            'uid' => $row['uid'],
            'name' => $row['name'],
            'profilePicture' => $row['profile_picture'],
            'bio' => $row['bio']
        ];
    }
    
    http_response_code(200);
    echo json_encode([
        'success' => true,
        'users' => $users,
        'count' => count($users)
    ]);
    
} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
}
?>
