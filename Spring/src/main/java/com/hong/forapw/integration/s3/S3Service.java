package com.hong.forapw.integration.s3;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    public URL generatePresignedUrl(Long userId, HttpMethod httpMethod) {
        Date expirationTime = calculateExpirationTime();

        String objectKey = createObjectKey(userId);
        GeneratePresignedUrlRequest preSignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, objectKey)
                .withMethod(httpMethod)
                .withExpiration(expirationTime);

        return amazonS3.generatePresignedUrl(preSignedUrlRequest);
    }

    private Date calculateExpirationTime() {
        return new Date(System.currentTimeMillis() + ((long) 5 * 60 * 1000)); // 현재 시간으로부터 15분
    }

    private String createObjectKey(Long userId) {
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uniqueId = UUID.randomUUID().toString();
        return String.format("%d/%s-%s", userId, currentTime, uniqueId);
    }
}