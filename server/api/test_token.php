<?php
// Test token parsing
require_once '../utils/auth.php';

// Use your actual token from login response
$token = "688a4c76b362e8ea27c39b9e7a66176956822a1eb21e1660c7b8c4e452f5dad_user_6919761742e410.95247198_1763276422";

echo "Testing Token Parsing:\n";
echo "Token: " . $token . "\n\n";

echo "Is Valid: " . (Auth::validateToken($token) ? "YES" : "NO") . "\n";
echo "User ID: " . Auth::getUserIdFromToken($token) . "\n";
echo "Expected: user_6919761742e410.95247198\n";
?>
