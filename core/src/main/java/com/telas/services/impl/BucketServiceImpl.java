package com.telas.services.impl;

import com.telas.infra.config.BucketConfigProperties;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.services.BucketService;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.AttachmentValidationMessages;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Profile("prod")
@RequiredArgsConstructor
public class BucketServiceImpl implements BucketService {
    private final Logger log = LoggerFactory.getLogger(BucketServiceImpl.class);
    private final BucketConfigProperties bucketProperties;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Override
    @Async
    public void upload(byte[] bytes, String fileName, String contentType, InputStream inputStream) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketProperties.getName())
                    .key(fileName)
                    .contentType(contentType)
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, bytes.length));
            log.info("File {} uploaded successfully to bucket {}.", fileName, bucketProperties.getName());
        } catch (Exception e) {
            log.error("Error uploading file '{}': {}", fileName, e.getMessage());
            throw new BusinessRuleException(AttachmentValidationMessages.ERROR_UPLOAD + " message: " + e.getMessage());
        }
    }

    @Override
    @Async
    public void deleteAttachment(String fileName) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketProperties.getName())
                    .key(fileName)
                    .build();
            // Remove o objeto do bucket
            s3Client.deleteObject(deleteObjectRequest);
            log.info("File {} deleted successfully from bucket {}.", fileName, bucketProperties.getName());
        } catch (Exception e) {
            log.error("Error deleting file '{}': {}", fileName, e.getMessage());
            throw new BusinessRuleException(
                    AttachmentValidationMessages.ERROR_DELETE_ATTACHMENT + fileName + " message: " + e.getMessage());
        }
    }

    @Override
    public List<String> getLinksDownload(List<String> objectNames) {
        return objectNames.stream()
                .map(this::getLink)
                .collect(Collectors.toList());
    }

    @Override
    public String getLink(String objectName) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketProperties.getName())
                    .key(objectName)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(SharedConstants.ATTACHMENT_LINK_EXPIRY_TIME))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            URL url = presignedRequest.url();

            return url.toString();
        } catch (Exception e) {
            log.error("Error generating presigned URL for object '{}': {}", objectName, e.getMessage());
            throw new BusinessRuleException(
                    AttachmentValidationMessages.ERROR_ATTACHMENT_LINK + objectName + " message: " + e.getMessage());
        }
    }
}