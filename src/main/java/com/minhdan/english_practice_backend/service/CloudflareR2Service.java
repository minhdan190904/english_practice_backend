package com.minhdan.english_practice_backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
public class CloudflareR2Service {

    private final S3Client s3Client;
    private final String bucket;
    private final String publicUrl;

    /** JPEG quality: 0.0 (worst) → 1.0 (best). 0.80 = good balance of quality & size */
    private static final float JPEG_QUALITY = 0.80f;

    public CloudflareR2Service(
            @Value("${cloudflare.r2.access-key}") String accessKey,
            @Value("${cloudflare.r2.secret-key}") String secretKey,
            @Value("${cloudflare.r2.endpoint}") String endpoint,
            @Value("${cloudflare.r2.bucket}") String bucket,
            @Value("${cloudflare.r2.public-url}") String publicUrl) {
        this.bucket = bucket;
        this.publicUrl = publicUrl;

        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.of("auto"))
                .forcePathStyle(true)
                .build();

        log.info("☁️ Cloudflare R2 initialized — bucket={}, publicUrl={}", bucket, publicUrl);
    }

    /**
     * Upload a base64-encoded image to R2 and return the public URL.
     * The image is compressed from PNG → JPEG (quality 80%) to reduce file size
     * by ~80% without changing dimensions.
     *
     * @param base64Image raw base64 string (no data URI prefix)
     * @param lessonTitle used for logging
     * @return public URL of the uploaded image, or null on error
     */
    public String uploadImage(String base64Image, String lessonTitle) {
        try {
            byte[] originalBytes = Base64.getDecoder().decode(base64Image);
            int originalSize = originalBytes.length;

            // Compress: PNG → JPEG (same dimensions, reduced file size)
            byte[] compressedBytes = compressToJpeg(originalBytes);
            int compressedSize = compressedBytes.length;
            int savedPercent = (int) ((1.0 - (double) compressedSize / originalSize) * 100);

            log.info("🗜️ Image compressed: {}KB → {}KB (saved {}%)",
                    originalSize / 1024, compressedSize / 1024, savedPercent);

            // Generate unique filename: lessons/2026/05/uuid.jpg
            String key = "lessons/" + java.time.LocalDate.now().toString().substring(0, 7).replace('-', '/')
                    + "/" + UUID.randomUUID().toString().substring(0, 12) + ".jpg";

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType("image/jpeg")
                    .cacheControl("public, max-age=31536000, immutable")
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(compressedBytes));

            String url = publicUrl + "/" + key;
            log.info("☁️ Image uploaded to R2: {} ({}KB)", url, compressedSize / 1024);
            return url;

        } catch (Exception e) {
            log.error("☁️ ❌ R2 upload failed: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Compress PNG bytes → JPEG bytes at the configured quality level.
     * Dimensions are preserved — only file size is reduced.
     */
    private byte[] compressToJpeg(byte[] pngBytes) throws Exception {
        // Read PNG into BufferedImage
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(pngBytes));

        // If image has alpha channel (ARGB), convert to RGB (JPEG doesn't support alpha)
        if (image.getType() == BufferedImage.TYPE_4BYTE_ABGR
                || image.getType() == BufferedImage.TYPE_INT_ARGB) {
            BufferedImage rgb = new BufferedImage(
                    image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            rgb.createGraphics().drawImage(image, 0, 0, java.awt.Color.WHITE, null);
            image = rgb;
        }

        // Write as JPEG with quality control
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(JPEG_QUALITY);

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }

        return out.toByteArray();
    }
}
