package com.marketingproject.services.impl;


import com.marketingproject.infra.exceptions.BusinessRuleException;
import com.marketingproject.services.BucketService;
import com.marketingproject.shared.constants.SharedConstants;
import com.marketingproject.shared.constants.valitation.AttachmentValidationMessages;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BucketServiceImpl implements BucketService {
    private final Logger log = LoggerFactory.getLogger(BucketServiceImpl.class);
    private final MinioClient minioClient;

    @Value("${minio.bucket.name}")
    private String bucketName;

    @Autowired
    BucketServiceImpl(
            @Value("${minio.endpoint}") String endpoint,
            @Value("${minio.access.key}") String accessKey,
            @Value("${minio.secret.key}") String secretKey) {
        minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    @Override
    @Transactional
    public void upload(byte[] bytes, String fileName, String contentType, InputStream inputStream) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(inputStream, bytes.length, -1)
                            .contentType(contentType)
                            .build());
        } catch (MinioException | InvalidKeyException | IOException | NoSuchAlgorithmException e) {
            log.error("Error uploading file: {}", e.getMessage());
            throw new BusinessRuleException(AttachmentValidationMessages.ERROR_UPLOAD + " message: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteAttachment(String fileName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build());
        } catch (Exception e) {
            throw new BusinessRuleException(
                    AttachmentValidationMessages.ERROR_DELETE_ATTACHMENT + fileName + " message: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public List<String> getLinksDownload(List<String> objectNames) {
        return objectNames.stream().map(this::getLink).collect(Collectors.toList());
    }

    protected String getLink(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(SharedConstants.ATTACHMENT_LINK_EXPIRY_TIME)
                            .build());
        } catch (Exception e) {
            throw new BusinessRuleException(
                    AttachmentValidationMessages.ERROR_ATTACHMENT_LINK + objectName + " message: " + e.getMessage());
        }
    }
}