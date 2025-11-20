<?php
/**
 * Create Post API Endpoint
 * POST /api/posts/create.php
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

if (empty($data->caption) && empty($data->mediaBase64)) {
    http_response_code(400);
    echo json_encode(['success' => false, 'message' => 'Post must have caption or media']);
    exit();
}

try {
    $postId = uniqid('post_', true);
    $timestamp = round(microtime(true) * 1000);
    $mediaUrl = null;
    $mediaType = 'image';
    
    // Handle media upload if provided
    if (!empty($data->mediaBase64)) {
        $uploadResult = Upload::uploadBase64($data->mediaBase64, 'posts', 'post_');
        if ($uploadResult['success']) {
            $mediaUrl = $uploadResult['url'];
            // Detect if video or image
            $mediaType = (strpos($mediaUrl, '.mp4') !== false || strpos($mediaUrl, '.mov') !== false) ? 'video' : 'image';
        }
    }
    
    $caption = isset($data->caption) ? $data->caption : null;
    $location = isset($data->location) ? $data->location : null;
    
    // Insert post
    $query = "INSERT INTO posts (post_id, user_id, media_url, media_type, caption, location, timestamp) 
              VALUES (:post_id, :user_id, :media_url, :media_type, :caption, :location, :timestamp)";
    
    $stmt = $db->prepare($query);
    $stmt->bindParam(':post_id', $postId);
    $stmt->bindParam(':user_id', $userId);
    $stmt->bindParam(':media_url', $mediaUrl);
    $stmt->bindParam(':media_type', $mediaType);
    $stmt->bindParam(':caption', $caption);
    $stmt->bindParam(':location', $location);
    $stmt->bindParam(':timestamp', $timestamp);
    
    $stmt->execute();
    
    // Fetch created post with user info
    $query = "SELECT p.*, u.name, u.profile_picture 
              FROM posts p 
              JOIN users u ON p.user_id = u.uid 
              WHERE p.post_id = :post_id";
    
    $stmt = $db->prepare($query);
    $stmt->bindParam(':post_id', $postId);
    $stmt->execute();
    
    $post = $stmt->fetch(PDO::FETCH_ASSOC);
    
    $response = [
        'success' => true,
        'post' => [
            'postId' => $post['post_id'],
            'userId' => $post['user_id'],
            'userName' => $post['name'],
            'userProfilePicture' => $post['profile_picture'],
            'mediaUrl' => $post['media_url'],
            'mediaType' => $post['media_type'],
            'caption' => $post['caption'],
            'location' => $post['location'],
            'timestamp' => (int)$post['timestamp'],
            'likesCount' => (int)$post['likes_count'],
            'commentsCount' => (int)$post['comments_count']
        ]
    ];
    
    http_response_code(201);
    echo json_encode($response);
    
} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
}
?>
