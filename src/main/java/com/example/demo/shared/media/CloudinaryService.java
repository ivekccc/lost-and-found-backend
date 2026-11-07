package com.example.demo.shared.media;

import com.example.demo.shared.ratelimit.RateLimitPolicy;
import com.example.demo.shared.ratelimit.RateLimiter;
import com.example.demo.user.UserFacade;
import com.example.demo.user.model.User;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    static final String ALLOWED_FORMATS = "jpg,jpeg,png,webp,heic";
    static final String INCOMING_TRANSFORMATION = "c_limit,w_2000,h_2000";

    private final Cloudinary cloudinary;
    private final UserFacade userFacade;
    private final RateLimiter rateLimiter;

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    @Value("${cloudinary.upload-folder}")
    private String uploadFolder;

    public CloudinarySignatureDTO getCloudinarySignature(String userEmail) {
        User user = userFacade.requireByEmail(userEmail);
        rateLimiter.consume(RateLimitPolicy.UPLOAD_SIGNATURE_PER_USER, user.getId());

        long timestamp = System.currentTimeMillis() / 1000;
        String publicId = ownerPrefix(user.getId()) + UUID.randomUUID();

        Map<String, Object> params = ObjectUtils.asMap(
                "allowed_formats", ALLOWED_FORMATS,
                "overwrite", "false",
                "public_id", publicId,
                "timestamp", timestamp,
                "transformation", INCOMING_TRANSFORMATION
        );
        String signature = cloudinary.apiSignRequest(params, apiSecret, cloudinary.config.signatureVersion);

        return new CloudinarySignatureDTO(signature, timestamp, cloudName, apiKey, publicId,
                ALLOWED_FORMATS, false, INCOMING_TRANSFORMATION);
    }

    public boolean isOwnedBy(String publicId, Long userId) {
        return publicId != null && publicId.startsWith(ownerPrefix(userId)) && !publicId.contains("..");
    }

    public String deliveryUrl(String publicId) {
        return cloudinary.url()
                .secure(true)
                .transformation(new Transformation().fetchFormat("auto"))
                .generate(publicId);
    }

    private String ownerPrefix(Long userId) {
        return uploadFolder + "/" + userId + "/";
    }

    @SuppressWarnings("unchecked")
    public UploadedImage uploadImageFromUrl(String imageUrl) {
        try {
            Map<String, Object> result = cloudinary.uploader().upload(imageUrl, ObjectUtils.asMap(
                    "folder", uploadFolder,
                    "transformation", INCOMING_TRANSFORMATION
            ));
            String publicId = (String) result.get("public_id");
            return new UploadedImage(deliveryUrl(publicId), publicId);
        } catch (Exception e) {
            log.error("Failed to upload a remote image to Cloudinary", e);
            return null;
        }
    }

    public record UploadedImage(String url, String publicId) {
    }

    public void deleteImage(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (Exception e) {
            log.error("Failed to delete image from Cloudinary: {}", publicId, e);
        }
    }

    public void deleteImages(List<String> publicIds) {
        publicIds.forEach(this::deleteImage);
    }
}
