package com.cyxz.upload.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传服务接口
 * <p>提供头像、帖子图片的上传与删除能力。
 */
public interface UploadService {

    /**
     * 上传用户头像
     *
     * @param file   图片文件
     * @param userId 用户 ID
     * @return 文件访问 URL
     */
    String uploadAvatar(MultipartFile file, Long userId);

    /**
     * 上传帖子图片
     *
     * @param file 图片文件
     * @return 文件访问 URL
     */
    String uploadPostImage(MultipartFile file);

    /**
     * 删除文件
     *
     * @param objectName MinIO 中的对象路径
     */
    void deleteFile(String objectName);
}
