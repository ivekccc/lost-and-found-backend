package com.example.demo.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.demo.dto.CloudinarySignatureDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    // Zasto se ovo dupla, kada vec ima u konfiguraicji?
    //Sta je public id
    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    @Value("${cloudinary.upload-folder}")
    private String uploadFolder;

    @SuppressWarnings("unchecked")
    public CloudinarySignatureDTO getCloudinarySignature() {
        long timestamp = System.currentTimeMillis() / 1000;

        Map<String, Object> params = ObjectUtils.asMap(
                "timestamp", timestamp,
                "folder", uploadFolder
        );
        String signature = cloudinary.apiSignRequest(params, apiSecret);

        return new CloudinarySignatureDTO(signature, timestamp, cloudName, apiKey, uploadFolder);
    }

    public void deleteImage(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (Exception e) {
            log.error("Failed  to delete image from Cloudinary : {}", publicId, e);
        }
    }

    public void deleteImages(List<String> publicIds) {
        publicIds.forEach(this::deleteImage);
    }
}
