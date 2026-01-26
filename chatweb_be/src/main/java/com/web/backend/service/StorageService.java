package com.web.backend.service;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    String upload(MultipartFile avatarFile, String folder);
    void delete(String url, String folder);
}
