package com.web.backend.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.web.backend.exception.InvalidDataException;
import com.web.backend.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "STORAGE-SERVICE")
public class StorageServiceImpl implements StorageService {

    private final Cloudinary cloudinary;

    @Value("${app.upload.avatar.max-size}")
    private Long maxSize;

    @Value("${app.upload.avatar.max-size-video}")
    private Long maxSizeVideo;

    @Override
    public String uploadAvatar(MultipartFile avatar) {
        try {
            if (avatar.isEmpty()) throw new InvalidDataException("ảnh avatar không được để trống");

            if (!avatar.getContentType().startsWith("image/")) throw new InvalidDataException("Chỉ được phép tải ảnh");

            if (avatar.getSize() > maxSize) throw new InvalidDataException("ảnh avatar quá lớn. Vui lòng chọn ảnh < 5MB");

            Map upLoadResult = cloudinary.uploader().upload(avatar.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "avatars",
                            "public_id", UUID.randomUUID().toString()
                    ));
            String url = (String) upLoadResult.get("secure_url");
            log.info("Upload avatar success: {}", url);
            return url;
        } catch (IOException e) {
            log.error("Upload avatar failed: {}" , e.getMessage());
            throw new RuntimeException("Lỗi upload file: " + e.getMessage());
        }
    }

    @Override
    public String upLoadImage(MultipartFile image) {
        try {
            if (image.isEmpty()) throw new InvalidDataException("ảnh không được để trống");

            if (!image.getContentType().startsWith("image/")) throw new InvalidDataException("Chỉ được phép tải ảnh");

            if (image.getSize() > maxSize) throw new InvalidDataException("ảnh quá lớn. Vui lòng chọn ảnh < 5MB");

            Map upLoadResult = cloudinary.uploader().upload(image.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "images",
                            "public_id", UUID.randomUUID().toString()
                    ));
            String url = (String) upLoadResult.get("secure_url");
            log.info("Upload image success: {}", url);
            return url;
        } catch (IOException e) {
            log.error("Upload image failed: {}" , e.getMessage());
            throw new RuntimeException("Lỗi upload file: " + e.getMessage());
        }
    }

    @Override
    public String uploadVideo(MultipartFile video) {
        try {
            if (video.isEmpty()) throw new InvalidDataException("video không được để trống");

            if (!video.getContentType().startsWith("video")) throw new InvalidDataException("chỉ được phép tải video ");

            if (video.getSize() > maxSizeVideo) throw new InvalidDataException("video quá lớn. Vui lòng chọn video dưới 20MB");

            Map upLoadResult = cloudinary.uploader().upload(video.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "videos",
                            "public_id", UUID.randomUUID().toString()
                    ));
            String url = (String) upLoadResult.get("secure_url");
            log.info("Upload video success: {}", url);
            return url;
        } catch (IOException e) {
            log.error("Upload video failed: {}" , e.getMessage());
            throw new RuntimeException("Lỗi upload file: " + e.getMessage());
        }
    }

    @Override
    @Async
    public void delete(String url, String folder) {
        try {
            if (url == null || url.isEmpty()) return;

            int startIndex = url.lastIndexOf(folder + "/");
            int endIndex = url.lastIndexOf(".");

            if (startIndex != -1 && endIndex != -1) {
                String publicId = url.substring(startIndex, endIndex);
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                log.info("Deleted old avatar: {}", publicId);
            }
        } catch (IOException e) {
            log.error("Failed to delete file: {}", e.getMessage());
        }
    }
}
