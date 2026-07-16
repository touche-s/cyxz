package com.cyxz.upload.controller;

import com.cyxz.common.base.Result;
import com.cyxz.common.web.CurrentUser;
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
     * @param userId 当前登录用户 ID（由 Gateway 注入）
     * @return 文件访问 URL
     */
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file,
                                       @CurrentUser Long userId) {
        String url = uploadService.uploadAvatar(file, userId);
        return Result.success("操作成功", url);
    }

    /**
     * 上传帖子封面
     *
     * @param file 图片文件
     * @return 文件访问 URL
     */
    @PostMapping("/cover")
    public Result<String> uploadCover(@RequestParam("file") MultipartFile file) {
        String url = uploadService.uploadCover(file);
        return Result.success("操作成功", url);
    }

    /**
     * 上传帖子图片
     *
     * @param file 图片文件
     * @return 文件访问 URL
     */
    @PostMapping("/post-image")
    public Result<String> uploadPostImage(@RequestParam("file") MultipartFile file) {
        String url = uploadService.uploadPostImage(file);
        return Result.success("操作成功", url);
    }
}
