<?php
/**
 * File Upload Utilities
 */

class Upload {
    
    private $upload_dir = "../uploads/";
    private $allowed_image_types = ['image/jpeg', 'image/png', 'image/jpg', 'image/gif'];
    private $allowed_video_types = ['video/mp4', 'video/mpeg', 'video/quicktime'];
    private $max_file_size = 10485760; // 10MB
    
    /**
     * Upload base64 encoded file
     */
    public function uploadBase64($base64Data, $folder, $prefix = '') {
        try {
            // Remove data URI scheme if present
            if (strpos($base64Data, 'data:') === 0) {
                $base64Data = explode(',', $base64Data)[1];
            }
            
            // Decode base64
            $fileData = base64_decode($base64Data);
            
            if ($fileData === false) {
                return ['success' => false, 'message' => 'Invalid base64 data'];
            }
            
            // Generate unique filename
            $fileName = $prefix . uniqid() . '_' . time();
            
            // Detect file type
            $finfo = finfo_open(FILEINFO_MIME_TYPE);
            $mimeType = finfo_buffer($finfo, $fileData);
            finfo_close($finfo);
            
            // Set file extension based on mime type
            $extension = $this->getExtensionFromMime($mimeType);
            if (!$extension) {
                return ['success' => false, 'message' => 'Unsupported file type'];
            }
            
            $fileName .= '.' . $extension;
            
            // Create directory if not exists
            $fullPath = $this->upload_dir . $folder . '/';
            if (!is_dir($fullPath)) {
                mkdir($fullPath, 0755, true);
            }
            
            // Save file
            $filePath = $fullPath . $fileName;
            if (file_put_contents($filePath, $fileData)) {
                return [
                    'success' => true,
                    'url' => $folder . '/' . $fileName,
                    'full_path' => $filePath
                ];
            } else {
                return ['success' => false, 'message' => 'Failed to save file'];
            }
            
        } catch (Exception $e) {
            return ['success' => false, 'message' => $e->getMessage()];
        }
    }
    
    /**
     * Get file extension from MIME type
     */
    private function getExtensionFromMime($mimeType) {
        $mimeMap = [
            'image/jpeg' => 'jpg',
            'image/jpg' => 'jpg',
            'image/png' => 'png',
            'image/gif' => 'gif',
            'video/mp4' => 'mp4',
            'video/mpeg' => 'mpeg',
            'video/quicktime' => 'mov'
        ];
        
        return isset($mimeMap[$mimeType]) ? $mimeMap[$mimeType] : null;
    }
    
    /**
     * Delete file
     */
    public function deleteFile($filePath) {
        $fullPath = $this->upload_dir . $filePath;
        if (file_exists($fullPath)) {
            return unlink($fullPath);
        }
        return false;
    }
}
?>
