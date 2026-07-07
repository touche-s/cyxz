package com.cyxz.common.feign;

import com.cyxz.common.base.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * 上传服务 Feign 客户端
 * <p>供其他服务通过 Feign 调用 cyxz-upload 上传文件。
 */
@FeignClient(name = "cyxz-upload", path = "/upload")
public interface UploadFeignClient {

    /**
     * 上传用户头像
     *
     * @param file   图片文件
     * @param userId 用户 ID
     * @return 文件访问 URL
     */
    @PostMapping(value = "/avatar", consumes = "multipart/form-data")
    Result<String> uploadAvatar(@RequestParam("file") MultipartFile file,
                                @RequestParam("userId") Long userId);

    /**
     * 上传帖子图片
     *
     * @param file 图片文件
     * @return 文件访问 URL
     */
    @PostMapping(value = "/post-image", consumes = "multipart/form-data")
    Result<String> uploadPostImage(@RequestParam("file") MultipartFile file);
}
