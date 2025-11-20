<?php
/**
 * Search Users API Endpoint
 * GET /api/users/search.php?query=john
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

$currentUserId = Auth::getUserIdFromToken($token);

if (empty($_GET['query'])) {
    http_response_code(400);
    echo json_encode(['success' => false, 'message' => 'Search query is required']);
    exit();
}

$searchQuery = '%' . $_GET['query'] . '%';
$limit = isset($_GET['limit']) ? (int)$_GET['limit'] : 50;

try {
    // Search users by name or email
    $query = "SELECT u.uid, u.name, u.email, u.profile_picture, u.bio, u.online,
              EXISTS(SELECT 1 FROM follows WHERE follower_id = :current_user AND following_id = u.uid AND status = 'accepted') as is_following
              FROM users u 
              WHERE (u.name LIKE :search OR u.email LIKE :search)
                AND u.uid != :current_user
              ORDER BY u.name ASC 
              LIMIT :limit";
    
    $stmt = $db->prepare($query);
    $stmt->bindParam(':current_user', $currentUserId);
    $stmt->bindParam(':search', $searchQuery);
    $stmt->bindParam(':limit', $limit, PDO::PARAM_INT);
    $stmt->execute();
    
    $users = [];
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        $users[] = [
            'uid' => $row['uid'],
            'name' => $row['name'],
            'email' => $row['email'],
            'profilePicture' => $row['profile_picture'],
            'bio' => $row['bio'],
            'online' => (bool)$row['online'],
            'isFollowing' => (bool)$row['is_following']
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
