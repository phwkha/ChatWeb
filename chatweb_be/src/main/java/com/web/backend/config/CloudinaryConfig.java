package com.web.backend.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class CloudinaryConfig {
    private static final String API_KEY_STRING = "api_key";
    private static final String API_SECRET_STRING = "api_secret";
    private static final String CLOUD_NAME_STRING = "cloud_name";


    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        Map<String, String> config = new HashMap<>();
        config.put(CLOUD_NAME_STRING, cloudName);
        config.put(API_KEY_STRING, apiKey);
        config.put(API_SECRET_STRING, apiSecret);
        return new Cloudinary(config);
    }
}
