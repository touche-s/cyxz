package com.cyxz.upload.controller;

import com.cyxz.common.base.Result;
import com.cyxz.common.web.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import com.cyxz.upload.config.MinioConfig;
import com.cyxz.upload.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件上传控制器
 * <p>提供头像和帖子图片的上传接口。
 */
@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;
    private final MinioConfig minioConfig;

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
     * 上传帖子图片
     *
     * @param file 图片文件
     * @return 文件访问 URL
     */
    @PostMapping("/post-image")
    public Result<String> uploadPostImage(@RequestParam("file") MultipartFile file,
                                          @CurrentUser Long userId) {
        String url = uploadService.uploadPostImage(file, userId);
        return Result.success("操作成功", url);
    }

    /**
     * 删除已上传的文件
     * <p>仅允许删除自己的文件：头像路径格式为 avatar/{userId}/...，
     * 会校验当前登录用户是否与路径中的 userId 一致。
     *
     * @param url    文件完整 URL
     * @param userId 当前登录用户 ID（由 Gateway 注入）
     * @return 操作结果
     */
    @DeleteMapping("/file")
    public Result<Void> deleteFile(@RequestParam("url") String url, @CurrentUser Long userId) {
        String prefix = minioConfig.getEndpoint() + "/" + minioConfig.getBucketName() + "/";
        if (!url.startsWith(prefix)) {
            return Result.fail("非法的文件地址");
        }
        String objectName = url.substring(prefix.length());

        // 校验文件归属：avatar/{userId}/... 和 post/image/{userId}/...
        if (objectName.startsWith("avatar/") || objectName.startsWith("post/image/")) {
            String pathPrefix = objectName.startsWith("avatar/") ? "avatar/" : "post/image/";
            String remaining = objectName.substring(pathPrefix.length());
            int slashIdx = remaining.indexOf('/');
            if (slashIdx > 0) {
                String ownerIdStr = remaining.substring(0, slashIdx);
                if (!String.valueOf(userId).equals(ownerIdStr)) {
                    return Result.fail("无权删除他人的文件");
                }
            }
        }

        uploadService.deleteFile(objectName);
        return Result.success("操作成功", null);
    }

    /**
     * 上传圈子资源（头像或封面）
     * <p>站主/平台管理员可上传任意圈子资源；圈主/圈子管理员可上传自己圈子资源。
     *
     * @param file     图片文件
     * @param circleId 圈子 ID
     * @param type     资源类型：avatar 或 cover
     * @param userId   当前登录用户 ID（由 Gateway 注入）
     * @return 文件访问 URL
     */
    @PreAuthorize("hasRole('SITE_OWNER') or hasRole('PLATFORM_ADMIN') or " +
                  "@circlePerm.hasAuthority('circle:resource:upload', #circleId)")
    @PostMapping("/circle-resource")
    public Result<String> uploadCircleResource(@RequestParam("file") MultipartFile file,
                                                @RequestParam("circleId") Long circleId,
                                                @RequestParam("type") String type,
                                                @CurrentUser Long userId) {
        String url = uploadService.uploadCircleResource(file, circleId, type);
        return Result.success("操作成功", url);
    }

    /**
     * 查询当前登录用户的头像上传历史
     *
     * @param userId 当前登录用户 ID（由 Gateway 注入）
     * @return 头像文件访问 URL 列表，按上传时间倒序
     */
    @GetMapping("/avatar-history")
    public Result<List<String>> getAvatarHistory(@CurrentUser Long userId) {
        List<String> urls = uploadService.listAvatarHistory(userId);
        return Result.success("操作成功", urls);
    }
}
