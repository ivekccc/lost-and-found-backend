package com.example.demo.shared.media;

import com.example.demo.user.model.User;
import com.example.demo.user.UserFacade;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.util.Map;
@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;
    private final UserFacade userFacade;

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    @Value("${cloudinary.upload-folder}")
    private String uploadFolder;

    /**
     * Signature for ONE upload, tied to the user through the file name. Signing only
     * {timestamp, folder} let any account upload under someone else's path; the public_id is now
     * server-generated as lost-and-found/{userId}/{uuid} and signed, so renaming breaks the
     * signature.
     */
    @SuppressWarnings("unchecked")
    public CloudinarySignatureDTO getCloudinarySignature(String userEmail) {
        User user = userFacade.requireByEmail(userEmail);

        long timestamp = System.currentTimeMillis() / 1000;
        String publicId = ownerPrefix(user.getId()) + UUID.randomUUID();

        // Exactly the parameters the client sends are signed — no file, api_key or cloud_name.
        Map<String, Object> params = ObjectUtils.asMap(
                "timestamp", timestamp,
                "public_id", publicId
        );
        String signature = cloudinary.apiSignRequest(params, apiSecret);

        return new CloudinarySignatureDTO(signature, timestamp, cloudName, apiKey, publicId);
    }

    /**
     * Da li slika sa datim {@code publicId}-em pripada korisniku, po prefiksu putanje.
     */
    public boolean isOwnedBy(String publicId, Long userId) {
        return publicId != null && publicId.startsWith(ownerPrefix(userId));
    }

    private String ownerPrefix(Long userId) {
        return uploadFolder + "/" + userId + "/";
    }

    @SuppressWarnings("unchecked")
    public UploadedImage uploadImageFromUrl(String imageUrl) {
        try {
            Map<String, Object> result = cloudinary.uploader().upload(imageUrl, ObjectUtils.asMap(
                    "folder", uploadFolder
            ));
            return new UploadedImage(
                    (String) result.get("secure_url"),
                    (String) result.get("public_id"));
        } catch (Exception e) {
            log.error("Failed to upload image to Cloudinary from URL: {}", imageUrl, e);
            return null;
        }
    }

    public record UploadedImage(String url, String publicId) {
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
