<?php
/**
 * Login API Endpoint
 * POST /api/auth/login
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

// Get posted data
$data = json_decode(file_get_contents("php://input"));

// Validate input
if (empty($data->email) || empty($data->password)) {
    http_response_code(400);
    echo json_encode(['success' => false, 'message' => 'Email and password are required']);
    exit();
}

try {
    // Find user by email
    $query = "SELECT uid, name, email, password_hash, profile_picture, cover_photo, bio, last_online 
              FROM users WHERE email = :email";
    
    $stmt = $db->prepare($query);
    $stmt->bindParam(':email', $data->email);
    $stmt->execute();
    
    if ($stmt->rowCount() === 0) {
        http_response_code(401);
        echo json_encode(['success' => false, 'message' => 'Invalid email or password']);
        exit();
    }
    
    $user = $stmt->fetch(PDO::FETCH_ASSOC);
    
    // Verify password
    if (!Auth::verifyPassword($data->password, $user['password_hash'])) {
        http_response_code(401);
        echo json_encode(['success' => false, 'message' => 'Invalid email or password']);
        exit();
    }
    
    // Update user online status
    $timestamp = round(microtime(true) * 1000);
    $updateQuery = "UPDATE users SET online = true, last_online = :timestamp WHERE uid = :uid";
    $updateStmt = $db->prepare($updateQuery);
    $updateStmt->bindParam(':timestamp', $timestamp);
    $updateStmt->bindParam(':uid', $user['uid']);
    $updateStmt->execute();
    
    // Generate auth token
    $token = Auth::generateToken($user['uid']);
    
    // Prepare response
    $response = [
        'success' => true,
        'message' => 'Login successful',
        'user' => [
            'uid' => $user['uid'],
            'name' => $user['name'],
            'email' => $user['email'],
            'profilePicture' => $user['profile_picture'],
            'coverPhoto' => $user['cover_photo'],
            'bio' => $user['bio'],
            'online' => true,
            'lastOnline' => $timestamp
        ],
        'token' => $token
    ];
    
    http_response_code(200);
    echo json_encode($response);
    
} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
}
?>
