<?php
/**
 * Fetch Active Stories API Endpoint
 * GET /api/stories/fetch.php
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

$userId = Auth::getUserIdFromToken($token);
$currentTime = round(microtime(true) * 1000);

try {
    // Fetch active stories from users the current user follows + own stories
    // Group by user to show one circle per user
    $query = "SELECT s.*, u.name, u.profile_picture,
              EXISTS(SELECT 1 FROM story_views WHERE story_id = s.id AND viewer_id = :current_user) as is_viewed,
              (SELECT COUNT(*) FROM stories WHERE user_id = s.user_id AND expires_at > :current_time) as stories_count
              FROM stories s 
              JOIN users u ON s.user_id = u.uid 
              WHERE s.expires_at > :current_time
                AND (s.user_id = :current_user 
                     OR s.user_id IN (SELECT following_id FROM follows WHERE follower_id = :current_user AND status = 'accepted'))
              ORDER BY s.timestamp DESC";
    
    $stmt = $db->prepare($query);
    $stmt->bindParam(':current_user', $userId);
    $stmt->bindParam(':current_time', $currentTime);
    $stmt->execute();
    
    // Group stories by user
    $userStories = [];
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        $userIdKey = $row['user_id'];
        
        if (!isset($userStories[$userIdKey])) {
            $userStories[$userIdKey] = [
                'userId' => $row['user_id'],
                'userName' => $row['name'],
                'userProfilePicture' => $row['profile_picture'],
                'storiesCount' => (int)$row['stories_count'],
                'hasUnviewed' => false,
                'stories' => []
            ];
        }
        
        $userStories[$userIdKey]['stories'][] = [
            'id' => $row['id'],
            'mediaUrl' => $row['media_url'],
            'timestamp' => (int)$row['timestamp'],
            'expiresAt' => (int)$row['expires_at'],
            'isVideo' => (bool)$row['is_video'],
            'isViewed' => (bool)$row['is_viewed']
        ];
        
        if (!(bool)$row['is_viewed']) {
            $userStories[$userIdKey]['hasUnviewed'] = true;
        }
    }
    
    // Convert to array
    $stories = array_values($userStories);
    
    http_response_code(200);
    echo json_encode([
        'success' => true,
        'stories' => $stories
    ]);
    
} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
}
?>
