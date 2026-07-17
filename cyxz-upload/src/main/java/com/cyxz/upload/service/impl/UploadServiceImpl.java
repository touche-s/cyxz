package com.cyxz.upload.service.impl;

import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.upload.config.MinioConfig;
import com.cyxz.upload.service.UploadService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传服务实现
 * <p>所有文件通过 MinIO 客户端上传至配置的 Bucket，
 * 返回可直接访问的 URL。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif");
    private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/gif");

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @Override
    public String uploadAvatar(MultipartFile file, Long userId) {
        validateImage(file, "头像");
        String objectName = "avatar/" + userId + "/" + generateFileName(file);
        return upload(file, objectName);
    }

    @Override
    public String uploadCover(MultipartFile file) {
        validateImage(file, "帖子封面");
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String objectName = "post/cover/" + datePath + "/" + generateFileName(file);
        return upload(file, objectName);
    }

    @Override
    public String uploadPostImage(MultipartFile file) {
        validateImage(file, "帖子图片");
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String objectName = "post/image/" + datePath + "/" + generateFileName(file);
        return upload(file, objectName);
    }

    @Override
    public void deleteFile(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .build()
            );
            log.info("文件已删除: {}", objectName);
        } catch (Exception e) {
            log.error("删除文件失败: {}", objectName, e);
            throw new BusinessException(ErrorCode.FAIL, "文件删除失败");
        }
    }

    /**
     * 执行实际上传
     *
     * @param file       文件
     * @param objectName MinIO 对象路径
     * @return 文件访问 URL
     */
    private String upload(MultipartFile file, String objectName) {
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(normalizeContentType(file.getContentType()))
                            .build()
            );
            String url = minioConfig.getEndpoint() + "/" + minioConfig.getBucketName() + "/" + objectName;
            log.info("文件上传成功: {}", url);
            return url;
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ErrorCode.FAIL, "文件上传失败");
        }
    }

    /**
     * 生成唯一文件名
     *
     * @param file 原始文件
     * @return UUID + 原始扩展名
     */
    private String generateFileName(MultipartFile file) {
        String extension = getExtension(file.getOriginalFilename());
        return UUID.randomUUID().toString().replace("-", "") + extension;
    }

    private void validateImage(MultipartFile file, String bizType) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, bizType + "不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, bizType + "文件名不能为空");
        }

        String extension = getExtension(originalFilename);
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, bizType + "仅支持 jpg、jpeg、png、gif 格式");
        }

        String contentType = normalizeContentType(file.getContentType());
        if (!ALLOWED_IMAGE_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, bizType + "类型不合法");
        }

        try (InputStream inputStream = file.getInputStream()) {
            if (ImageIO.read(inputStream) == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, bizType + "内容不合法");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, bizType + "内容不合法", e);
        }
    }

    private String getExtension(String fileName) {
        if (StringUtils.hasText(fileName) && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".")).toLowerCase(Locale.ROOT);
        }
        return "";
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
    }
}
