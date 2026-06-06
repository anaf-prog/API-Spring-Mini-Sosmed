package com.sosmed.config;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;

import com.cloudinary.Cloudinary;

@Configuration
@ImportRuntimeHints(CloudinaryRuntimeHint.class)
public class CloudinaryConfig {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", cloudName);
        config.put("api_key", apiKey);
        config.put("api_secret", apiSecret);
        
        return new Cloudinary(config);
    }

    @Bean(name = "cloudinaryVirtualThreadExecutor")
    public AsyncTaskExecutor cloudinaryVirtualThreadExecutor() {
        // Membuat Executor khusus yang selalu menelurkan Virtual Thread baru untuk setiap tugas background
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }
    
}
