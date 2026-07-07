package com.cyxz.upload.controller;

import com.cyxz.common.base.Result;
import com.cyxz.upload.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传控制器
 * <p>提供头像和帖子图片的上传接口。
 */
@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    /**
     * 上传用户头像
     *
     * @param file   图片文件
     * @param userId 用户 ID
     * @return 文件访问 URL
     */
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam MultipartFile file,
                                       @RequestParam Long userId) {
        String url = uploadService.uploadAvatar(file, userId);
        return Result.success(url);
    }

    /**
     * 上传帖子图片
     *
     * @param file 图片文件
     * @return 文件访问 URL
     */
    @PostMapping("/post-image")
    public Result<String> uploadPostImage(@RequestParam MultipartFile file) {
        String url = uploadService.uploadPostImage(file);
        return Result.success(url);
    }
}
