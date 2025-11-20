<?php
/**
 * Fetch Messages API Endpoint
 * GET /api/messages/fetch.php?otherUserId=xxx&since=0
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

if (empty($_GET['otherUserId'])) {
    http_response_code(400);
    echo json_encode(['success' => false, 'message' => 'Other user ID is required']);
    exit();
}

$otherUserId = $_GET['otherUserId'];
$since = isset($_GET['since']) ? (int)$_GET['since'] : 0;

try {
    // Fetch conversation between two users
    $query = "SELECT m.* 
              FROM messages m 
              WHERE ((m.sender_id = :user_id AND m.receiver_id = :other_user_id) 
                     OR (m.sender_id = :other_user_id AND m.receiver_id = :user_id))
                AND m.is_deleted = FALSE
                AND m.timestamp > :since
              ORDER BY m.timestamp ASC";
    
    $stmt = $db->prepare($query);
    $stmt->bindParam(':user_id', $userId);
    $stmt->bindParam(':other_user_id', $otherUserId);
    $stmt->bindParam(':since', $since);
    $stmt->execute();
    
    $messages = [];
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        $messages[] = [
            'id' => $row['id'],
            'senderId' => $row['sender_id'],
            'receiverId' => $row['receiver_id'],
            'content' => $row['content'],
            'timestamp' => (int)$row['timestamp'],
            'mediaUrl' => $row['media_url'],
            'type' => $row['type'],
            'isEdited' => (bool)$row['is_edited'],
            'editedAt' => $row['edited_at'] ? (int)$row['edited_at'] : null,
            'isSeen' => (bool)$row['is_seen'],
            'seenAt' => $row['seen_at'] ? (int)$row['seen_at'] : null,
            'vanishMode' => (bool)$row['vanish_mode']
        ];
    }
    
    // Mark messages as seen if they're sent to current user
    $updateQuery = "UPDATE messages 
                    SET is_seen = TRUE, seen_at = :seen_at 
                    WHERE receiver_id = :user_id 
                      AND sender_id = :other_user_id 
                      AND is_seen = FALSE";
    
    $stmt = $db->prepare($updateQuery);
    $seenAt = round(microtime(true) * 1000);
    $stmt->bindParam(':user_id', $userId);
    $stmt->bindParam(':other_user_id', $otherUserId);
    $stmt->bindParam(':seen_at', $seenAt);
    $stmt->execute();
    
    http_response_code(200);
    echo json_encode([
        'success' => true,
        'messages' => $messages
    ]);
    
} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
}
?>
