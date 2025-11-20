<?php
/**
 * Send Message API Endpoint
 * POST /api/messages/send.php
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

$senderId = Auth::getUserIdFromToken($token);

// Get POST data
$data = json_decode(file_get_contents("php://input"));

if (empty($data->receiverId) || (empty($data->content) && empty($data->mediaBase64))) {
    http_response_code(400);
    echo json_encode(['success' => false, 'message' => 'Receiver ID and content/media are required']);
    exit();
}

try {
    $messageId = uniqid('msg_', true);
    $timestamp = round(microtime(true) * 1000);
    $mediaUrl = null;
    $type = isset($data->type) ? $data->type : 'text';
    $vanishMode = isset($data->vanishMode) ? (bool)$data->vanishMode : false;
    
    // Handle media upload if provided
    if (!empty($data->mediaBase64)) {
        $uploadResult = Upload::uploadBase64($data->mediaBase64, 'messages', 'msg_');
        if ($uploadResult['success']) {
            $mediaUrl = $uploadResult['url'];
            // Set type based on media
            if (strpos($mediaUrl, '.mp4') !== false || strpos($mediaUrl, '.mov') !== false) {
                $type = 'video';
            } elseif (strpos($mediaUrl, '.jpg') !== false || strpos($mediaUrl, '.png') !== false) {
                $type = 'image';
            } else {
                $type = 'document';
            }
        }
    }
    
    $content = isset($data->content) ? $data->content : '';
    
    // Insert message
    $query = "INSERT INTO messages (id, sender_id, receiver_id, content, timestamp, media_url, type, vanish_mode) 
              VALUES (:id, :sender_id, :receiver_id, :content, :timestamp, :media_url, :type, :vanish_mode)";
    
    $stmt = $db->prepare($query);
    $stmt->bindParam(':id', $messageId);
    $stmt->bindParam(':sender_id', $senderId);
    $stmt->bindParam(':receiver_id', $data->receiverId);
    $stmt->bindParam(':content', $content);
    $stmt->bindParam(':timestamp', $timestamp);
    $stmt->bindParam(':media_url', $mediaUrl);
    $stmt->bindParam(':type', $type);
    $stmt->bindParam(':vanish_mode', $vanishMode, PDO::PARAM_BOOL);
    
    $stmt->execute();
    
    $response = [
        'success' => true,
        'message' => [
            'id' => $messageId,
            'senderId' => $senderId,
            'receiverId' => $data->receiverId,
            'content' => $content,
            'timestamp' => $timestamp,
            'mediaUrl' => $mediaUrl,
            'type' => $type,
            'vanishMode' => $vanishMode,
            'isEdited' => false,
            'isDeleted' => false,
            'isSeen' => false
        ]
    ];
    
    http_response_code(201);
    echo json_encode($response);
    
} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
}
?>
