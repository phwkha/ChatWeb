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
    private Long maxAvatarSize;

    @Value("${app.upload.avatar.max-size-video}")
    private Long maxVideoSize;

    @Override
    public String uploadAvatar(MultipartFile avatar) {
        return uploadFile(avatar, "avatars", maxAvatarSize, "image");
    }

    @Override
    public String upLoadImage(MultipartFile image) {
        return uploadFile(image, "images", maxAvatarSize, "image");
    }

    @Override
    public String uploadVideo(MultipartFile video) {
        return uploadFile(video, "videos", maxVideoSize, "video");
    }

    private String uploadFile(MultipartFile file, String folder, Long maxSize, String resourceType) {
        try {
            if (file.isEmpty()) {
                throw new InvalidDataException("File không được để trống");
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith(resourceType + "/")) {
                throw new InvalidDataException("Định dạng file không hợp lệ. Chỉ chấp nhận: " + resourceType);
            }

            if (file.getSize() > maxSize) {
                long sizeInMb = maxSize / (1024 * 1024);
                throw new InvalidDataException("File quá lớn. Vui lòng chọn file dưới " + sizeInMb + "MB");
            }

            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "public_id", UUID.randomUUID().toString(),
                            "resource_type", resourceType
                    ));

            String url = (String) uploadResult.get("secure_url");
            log.info("Upload {} success: {}", resourceType, url);
            return url;

        } catch (IOException e) {
            log.error("Upload failed: {}", e.getMessage());
            throw new RuntimeException("Lỗi upload file: " + e.getMessage());
        }
    }

    @Override
    @Async
    public void delete(String url, String folder) {
        try {
            if (url == null || url.isEmpty()) return;

            String resourceType = folder.equals("videos") ? "video" : "image";
            String publicId = extractPublicId(url, folder);

            if (publicId != null) {
                cloudinary.uploader().destroy(publicId,
                        ObjectUtils.asMap("resource_type", resourceType));
                log.info("Deleted old file: {} (type: {})", publicId, resourceType);
            }
        } catch (IOException e) {
            log.error("Failed to delete file: {}", e.getMessage());
        }
    }

    private String extractPublicId(String url, String folder) {
        try {
            int startIndex = url.lastIndexOf(folder + "/");
            int endIndex = url.lastIndexOf(".");
            if (startIndex != -1 && endIndex != -1) {
                return url.substring(startIndex, endIndex);
            }
        } catch (Exception e) {
            log.warn("Cannot extract publicId from url: {}", url);
        }
        return null;
    }
}