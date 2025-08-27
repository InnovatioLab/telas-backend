package com.telas.services.impl;

import com.telas.infra.config.BucketConfigProperties;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.services.BucketService;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.AttachmentValidationMessages;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Profile("dev")
@RequiredArgsConstructor
public class LocalBucketServiceImpl implements BucketService {
  private final Logger log = LoggerFactory.getLogger(LocalBucketServiceImpl.class);
  private final BucketConfigProperties bucketProperties;
  private final MinioClient minioClient;

  @Override
  @Async
  public void upload(byte[] bytes, String fileName, String contentType, InputStream inputStream) {
    try {
      minioClient.putObject(
              PutObjectArgs.builder()
                      .bucket(bucketProperties.getName())
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
  @Async
  public void deleteAttachment(String fileName) {
    try {
      minioClient.removeObject(
              RemoveObjectArgs.builder()
                      .bucket(bucketProperties.getName())
                      .object(fileName)
                      .build());
    } catch (Exception e) {
      throw new BusinessRuleException(
              AttachmentValidationMessages.ERROR_DELETE_ATTACHMENT + fileName + " message: " + e.getMessage());
    }
  }

  @Override
  public List<String> getLinksDownload(List<String> objectNames) {
    return objectNames.stream().map(this::getLink).collect(Collectors.toList());
  }

  @Override
  public String getLink(String objectName) {
    try {
      return minioClient.getPresignedObjectUrl(
              GetPresignedObjectUrlArgs.builder()
                      .method(Method.GET)
                      .bucket(bucketProperties.getName())
                      .object(objectName)
                      .expiry(SharedConstants.ATTACHMENT_LINK_EXPIRY_TIME)
                      .build());
    } catch (Exception e) {
      throw new BusinessRuleException(
              AttachmentValidationMessages.ERROR_ATTACHMENT_LINK + objectName + " message: " + e.getMessage());
    }
  }
}