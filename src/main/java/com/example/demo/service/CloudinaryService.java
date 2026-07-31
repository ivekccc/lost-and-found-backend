package com.example.demo.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.demo.dto.CloudinarySignatureDTO;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
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
    private final UserRepository userRepository;

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

    /**
     * Potpis za jedno otpremanje, vezan za korisnika kroz ime fajla.
     *
     * Ranije se potpisivalo samo {@code {timestamp, folder}}, pa je otpremljena slika bila
     * anonimna: nigde se nije beleZilo ko ju je poslao, a klijent je sam birao ime. Posto
     * brisanje naloga uklanja slike bas po {@code publicId}-u, prijavljivanje tudjeg imena je
     * omogucavalo unistavanje tudje fotografije. To je bilo zakrpljeno provera­om da ime nije
     * vec zauzeto; sada se resava u korenu.
     *
     * Server generise {@code lost-and-found/{userId}/{uuid}} i potpisuje BAS TO. Klijent ne
     * moze da otpremi pod drugim imenom, jer bi potpis prestao da vazi. Vlasnistvo je time deo
     * putanje i proverava se bez ijedne dodatne tabele — vidi {@link #isOwnedBy}.
     *
     * Potpis vazi za JEDNU sliku: {@code public_id} je jedinstven, pa klijent trazi nov potpis
     * za svaku sledecu.
     */
    @SuppressWarnings("unchecked")
    public CloudinarySignatureDTO getCloudinarySignature(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        long timestamp = System.currentTimeMillis() / 1000;
        String publicId = ownerPrefix(user.getId()) + UUID.randomUUID();

        // Potpisuju se tacno oni parametri koje klijent salje (bez file, api_key i cloud_name).
        // Zato folder vise ne ide zasebno — putanja je vec unutar public_id-a.
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
