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

    @Override
    public String upload(MultipartFile avatarFile, String folder) {
        try {
            if (avatarFile.isEmpty()) throw new InvalidDataException("avatar không được để trống");

            if (!avatarFile.getContentType().startsWith("image/")) throw new InvalidDataException("Chỉ được phép tải ảnh");

            if (avatarFile.getSize() > maxSize) {
                throw new InvalidDataException("File ảnh quá lớn. Vui lòng chọn ảnh < 5MB");
            }

            Map upLoadResult = cloudinary.uploader().upload(avatarFile.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "public_id", UUID.randomUUID().toString()
                    ));
            String url = (String) upLoadResult.get("secure_url");
            log.info("Upload file success: {}", url);
            return url;
        } catch (IOException e) {
            log.error("Upload failed: {}" , e.getMessage());
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
