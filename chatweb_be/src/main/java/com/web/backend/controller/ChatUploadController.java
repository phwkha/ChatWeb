package com.web.backend.controller;

import com.web.backend.controller.response.form.ApiResponse;
import com.web.backend.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/chat/")
@RequiredArgsConstructor
@Slf4j(topic = "CHAT-UPLOAD-CONTROLLER")
public class ChatUploadController {

    private final StorageService storageService;

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> uploadChatImage(@RequestParam("image") MultipartFile file) {
        log.info("upload image");
        String url = storageService.upLoadImage(file);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Upload thành công", url));
    }

    @PostMapping(value = "/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> uploadChatVideo(@RequestParam("video") MultipartFile file) {
        log.info("upload video");
        String url = storageService.uploadVideo(file);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Upload thành công", url));
    }
}
