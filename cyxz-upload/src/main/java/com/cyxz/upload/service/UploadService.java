package com.cyxz.upload.service;

import org.springframework.web.multipart.MultipartFile;

public interface UploadService {

    String uploadAvatar(MultipartFile file, Long userId);

    String uploadPostImage(MultipartFile file);

    void deleteFile(String objectName);
}
