package com.cyxz.common.feign;

import com.cyxz.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "cyxz-upload")
public interface UploadFeignClient {

    @PostMapping(value = "/upload/avatar", consumes = "multipart/form-data")
    Result<String> uploadAvatar(@RequestParam("file") MultipartFile file,
                                @RequestParam("userId") Long userId);

    @PostMapping(value = "/upload/post-image", consumes = "multipart/form-data")
    Result<String> uploadPostImage(@RequestParam("file") MultipartFile file);
}
