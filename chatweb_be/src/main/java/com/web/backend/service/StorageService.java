package com.web.backend.service;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    String uploadAvatar(MultipartFile avatar);
    String upLoadImage(MultipartFile image);
    void delete(String url, String folder);
}
