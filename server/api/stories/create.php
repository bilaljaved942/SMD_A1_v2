<?php
/**
 * Create Story API Endpoint
 * POST /api/stories/create.php
 * Requires Authorization header
 */

header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");
header("Access-Control-Max-Age: 3600");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

require_once '../../config/database.php';
require_once '../../utils/auth.php';
require_once '../../utils/upload.php';

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

if (empty($data->mediaBase64)) {
    http_response_code(400);
    echo json_encode(['success' => false, 'message' => 'Story media is required']);
    exit();
}

try {
    // Upload media
    $uploadResult = Upload::uploadBase64($data->mediaBase64, 'stories', 'story_');
    
    if (!$uploadResult['success']) {
        http_response_code(500);
        echo json_encode(['success' => false, 'message' => 'Failed to upload media']);
        exit();
    }
    
    $storyId = uniqid('story_', true);
    $timestamp = round(microtime(true) * 1000);
    $expiresAt = $timestamp + (24 * 60 * 60 * 1000); // 24 hours from now
    $isVideo = isset($data->isVideo) ? (bool)$data->isVideo : false;
    
    // Insert story
    $query = "INSERT INTO stories (id, user_id, media_url, timestamp, expires_at, is_video) 
              VALUES (:id, :user_id, :media_url, :timestamp, :expires_at, :is_video)";
    
    $stmt = $db->prepare($query);
    $stmt->bindParam(':id', $storyId);
    $stmt->bindParam(':user_id', $userId);
    $stmt->bindParam(':media_url', $uploadResult['url']);
    $stmt->bindParam(':timestamp', $timestamp);
    $stmt->bindParam(':expires_at', $expiresAt);
    $stmt->bindParam(':is_video', $isVideo, PDO::PARAM_BOOL);
    
    $stmt->execute();
    
    // Fetch created story with user info
    $query = "SELECT s.*, u.name, u.profile_picture 
              FROM stories s 
              JOIN users u ON s.user_id = u.uid 
              WHERE s.id = :story_id";
    
    $stmt = $db->prepare($query);
    $stmt->bindParam(':story_id', $storyId);
    $stmt->execute();
    
    $story = $stmt->fetch(PDO::FETCH_ASSOC);
    
    $response = [
        'success' => true,
        'story' => [
            'id' => $story['id'],
            'userId' => $story['user_id'],
            'userName' => $story['name'],
            'userProfilePicture' => $story['profile_picture'],
            'mediaUrl' => $story['media_url'],
            'timestamp' => (int)$story['timestamp'],
            'expiresAt' => (int)$story['expires_at'],
            'isVideo' => (bool)$story['is_video']
        ]
    ];
    
    http_response_code(201);
    echo json_encode($response);
    
} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
}
?>
