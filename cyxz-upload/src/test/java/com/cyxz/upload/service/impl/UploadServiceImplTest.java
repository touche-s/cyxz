package com.cyxz.upload.service.impl;

import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.upload.config.MinioConfig;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UploadServiceImpl 单元测试
 * <p>重点覆盖 validateImage 校验逻辑与 deleteFile 异常处理；
 * 成功上传路径用真实 1x1 PNG 流通过 ImageIO.read 校验。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UploadServiceImpl 文件上传")
class UploadServiceImplTest {

    @Mock private MinioClient minioClient;
    @Mock private MinioConfig minioConfig;
    @Mock private MultipartFile file;

    @InjectMocks
    private UploadServiceImpl uploadService;

    private static final Long USER_ID = 100L;
    private static final Long CIRCLE_ID = 7L;

    /** 真实 1x1 PNG 字节，用于通过 validateImage 的 ImageIO.read 校验 */
    private static byte[] PNG_BYTES;

    @BeforeAll
    static void preparePng() throws Exception {
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        PNG_BYTES = baos.toByteArray();
    }

    /** 构造一个能通过 validateImage 全部校验的图片文件 mock */
    private void setupValidImageFile() throws Exception {
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("test.png");
        when(file.getContentType()).thenReturn("image/png");
        when(file.getInputStream()).thenAnswer(inv -> new ByteArrayInputStream(PNG_BYTES));
    }

    // ==================== uploadAvatar ====================

    @Nested
    @DisplayName("uploadAvatar — 头像上传")
    class UploadAvatar {

        @Test
        @DisplayName("空文件被拒")
        void shouldRejectEmptyFile() throws Exception {
            when(file.isEmpty()).thenReturn(true);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> uploadService.uploadAvatar(file, USER_ID));

            assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
            assertTrue(ex.getMessage().contains("不能为空"));
            verify(minioClient, never()).putObject(any(PutObjectArgs.class));
        }

        @Test
        @DisplayName("非法扩展名被拒")
        void shouldRejectInvalidExtension() {
            when(file.isEmpty()).thenReturn(false);
            when(file.getOriginalFilename()).thenReturn("test.txt");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> uploadService.uploadAvatar(file, USER_ID));

            assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
            assertTrue(ex.getMessage().contains("jpg"));
        }

        @Test
        @DisplayName("非法 ContentType 被拒")
        void shouldRejectInvalidContentType() {
            when(file.isEmpty()).thenReturn(false);
            when(file.getOriginalFilename()).thenReturn("test.png");
            when(file.getContentType()).thenReturn("text/plain");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> uploadService.uploadAvatar(file, USER_ID));

            assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
            assertTrue(ex.getMessage().contains("类型不合法"));
        }

        @Test
        @DisplayName("正常上传头像返回可访问 URL")
        void shouldUploadAvatarSuccessfully() throws Exception {
            setupValidImageFile();
            when(file.getSize()).thenReturn((long) PNG_BYTES.length);
            when(minioConfig.getEndpoint()).thenReturn("http://localhost:9000");
            when(minioConfig.getBucketName()).thenReturn("test-bucket");

            String url = uploadService.uploadAvatar(file, USER_ID);

            assertTrue(url.contains("avatar/" + USER_ID + "/"));
            assertTrue(url.startsWith("http://localhost:9000/test-bucket/"));
            verify(minioClient).putObject(any(PutObjectArgs.class));
        }
    }

    // ==================== uploadPostImage ====================

    @Nested
    @DisplayName("uploadPostImage — 帖子图片上传")
    class UploadPostImage {

        @Test
        @DisplayName("正常上传帖子图片且路径含 userId")
        void shouldUploadPostImageWithUserIdInPath() throws Exception {
            setupValidImageFile();
            when(file.getSize()).thenReturn((long) PNG_BYTES.length);
            when(minioConfig.getEndpoint()).thenReturn("http://localhost:9000");
            when(minioConfig.getBucketName()).thenReturn("test-bucket");

            String url = uploadService.uploadPostImage(file, USER_ID);

            assertTrue(url.contains("post/image/" + USER_ID + "/"));
            assertTrue(url.startsWith("http://localhost:9000/test-bucket/"));
            verify(minioClient).putObject(any(PutObjectArgs.class));
        }
    }

    // ==================== deleteFile ====================

    @Nested
    @DisplayName("deleteFile — 文件删除")
    class DeleteFile {

        @Test
        @DisplayName("正常删除文件")
        void shouldDeleteFileSuccessfully() throws Exception {
            when(minioConfig.getBucketName()).thenReturn("test-bucket");

            uploadService.deleteFile("avatar/" + USER_ID + "/abc.png");

            verify(minioClient).removeObject(any(RemoveObjectArgs.class));
        }

        @Test
        @DisplayName("MinIO 异常抛 BusinessException")
        void shouldThrowWhenMinioError() throws Exception {
            when(minioConfig.getBucketName()).thenReturn("test-bucket");
            doThrow(new RuntimeException("minio down"))
                    .when(minioClient).removeObject(any(RemoveObjectArgs.class));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> uploadService.deleteFile("avatar/" + USER_ID + "/abc.png"));

            assertEquals(ErrorCode.FAIL.getCode(), ex.getCode());
            assertTrue(ex.getMessage().contains("文件删除失败"));
        }
    }

    // ==================== uploadCircleResource ====================

    @Nested
    @DisplayName("uploadCircleResource — 圈子资源上传")
    class UploadCircleResource {

        @Test
        @DisplayName("非法 type 被拒")
        void shouldRejectInvalidType() throws Exception {
            setupValidImageFile();

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> uploadService.uploadCircleResource(file, CIRCLE_ID, "invalid"));

            assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
            assertTrue(ex.getMessage().contains("不支持的资源类型"));
        }

        @Test
        @DisplayName("正常上传圈子头像资源")
        void shouldUploadCircleResourceSuccessfully() throws Exception {
            setupValidImageFile();
            when(file.getSize()).thenReturn((long) PNG_BYTES.length);
            when(minioConfig.getEndpoint()).thenReturn("http://localhost:9000");
            when(minioConfig.getBucketName()).thenReturn("test-bucket");

            String url = uploadService.uploadCircleResource(file, CIRCLE_ID, "avatar");

            assertTrue(url.contains("circle/" + CIRCLE_ID + "/avatar/"));
            assertTrue(url.startsWith("http://localhost:9000/test-bucket/"));
            verify(minioClient).putObject(any(PutObjectArgs.class));
        }
    }
}
