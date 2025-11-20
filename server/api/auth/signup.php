<?php
/**
 * Signup API Endpoint
 * POST /api/auth/signup
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
if (empty($data->name) || empty($data->email) || empty($data->password)) {
    http_response_code(400);
    echo json_encode(['success' => false, 'message' => 'Name, email, and password are required']);
    exit();
}

// Validate email format
if (!filter_var($data->email, FILTER_VALIDATE_EMAIL)) {
    http_response_code(400);
    echo json_encode(['success' => false, 'message' => 'Invalid email format']);
    exit();
}

// Validate password length
if (strlen($data->password) < 6) {
    http_response_code(400);
    echo json_encode(['success' => false, 'message' => 'Password must be at least 6 characters']);
    exit();
}

try {
    // Check if email already exists
    $query = "SELECT uid FROM users WHERE email = :email";
    $stmt = $db->prepare($query);
    $stmt->bindParam(':email', $data->email);
    $stmt->execute();
    
    if ($stmt->rowCount() > 0) {
        http_response_code(409);
        echo json_encode(['success' => false, 'message' => 'Email already exists']);
        exit();
    }
    
    // Generate user ID
    $userId = uniqid('user_', true);
    
    // Hash password
    $passwordHash = Auth::hashPassword($data->password);
    
    // Insert user
    $query = "INSERT INTO users (uid, name, email, password_hash, created_at, online) 
              VALUES (:uid, :name, :email, :password_hash, :created_at, true)";
    
    $stmt = $db->prepare($query);
    $timestamp = round(microtime(true) * 1000);
    
    $stmt->bindParam(':uid', $userId);
    $stmt->bindParam(':name', $data->name);
    $stmt->bindParam(':email', $data->email);
    $stmt->bindParam(':password_hash', $passwordHash);
    $stmt->bindParam(':created_at', $timestamp);
    
    if ($stmt->execute()) {
        // Generate auth token
        $token = Auth::generateToken($userId);
        
        // Prepare response
        $response = [
            'success' => true,
            'message' => 'User registered successfully',
            'user' => [
                'uid' => $userId,
                'name' => $data->name,
                'email' => $data->email,
                'profilePicture' => null,
                'coverPhoto' => null,
                'bio' => null,
                'online' => true,
                'lastOnline' => $timestamp
            ],
            'token' => $token
        ];
        
        http_response_code(201);
        echo json_encode($response);
    } else {
        http_response_code(500);
        echo json_encode(['success' => false, 'message' => 'Failed to create user']);
    }
    
} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
}
?>
