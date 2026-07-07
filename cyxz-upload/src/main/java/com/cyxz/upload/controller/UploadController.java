package com.cyxz.upload.controller;

import com.cyxz.common.Result;
import com.cyxz.upload.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam MultipartFile file,
                                       @RequestParam Long userId) {
        String url = uploadService.uploadAvatar(file, userId);
        return Result.success(url);
    }

    @PostMapping("/post-image")
    public Result<String> uploadPostImage(@RequestParam MultipartFile file) {
        String url = uploadService.uploadPostImage(file);
        return Result.success(url);
    }
}
