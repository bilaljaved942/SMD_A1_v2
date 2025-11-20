<?php
/**
 * Authentication Utilities
 */

class Auth {
    
    /**
     * Generate a secure token for user session
     */
    public static function generateToken($userId) {
        return bin2hex(random_bytes(32)) . '_' . $userId . '_' . time();
    }
    
    /**
     * Validate token format
     */
    public static function validateToken($token) {
        if (empty($token)) {
            return false;
        }
        
        $parts = explode('_', $token);
        // Token format: hash_userId_timestamp (userId can contain dots/underscores)
        return count($parts) >= 3;
    }
    
    /**
     * Extract user ID from token
     */
    public static function getUserIdFromToken($token) {
        $parts = explode('_', $token);
        // Token format: hash_userId_timestamp
        // User ID is everything between first _ and last _
        if (count($parts) < 3) {
            return null;
        }
        
        // Remove first part (hash) and last part (timestamp)
        array_shift($parts); // Remove hash
        array_pop($parts);   // Remove timestamp
        
        // Join remaining parts (this is the user ID, which may contain underscores)
        return implode('_', $parts);
    }
    
    /**
     * Hash password securely
     */
    public static function hashPassword($password) {
        return password_hash($password, PASSWORD_BCRYPT, ['cost' => 12]);
    }
    
    /**
     * Verify password
     */
    public static function verifyPassword($password, $hash) {
        return password_verify($password, $hash);
    }
    
    /**
     * Get authorization header
     */
    public static function getAuthorizationHeader() {
        $headers = apache_request_headers();
        
        if (isset($headers['Authorization'])) {
            return $headers['Authorization'];
        }
        
        if (isset($_SERVER['HTTP_AUTHORIZATION'])) {
            return $_SERVER['HTTP_AUTHORIZATION'];
        }
        
        return null;
    }
}
?>
