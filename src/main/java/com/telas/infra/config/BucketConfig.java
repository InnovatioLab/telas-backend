package com.telas.infra.config;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@RequiredArgsConstructor
public class BucketConfig {
  @Bean
  @Profile("dev")
  public MinioClient minioClient(BucketConfigProperties bucketConfig) {
    return MinioClient.builder()
            .endpoint(bucketConfig.getEndpoint())
            .credentials(bucketConfig.getAccessKey(), bucketConfig.getSecretKey())
            .build();
  }

  @Bean
  @Profile("prod")
  public S3Client s3Client(BucketConfigProperties bucketConfig) {
    return S3Client.builder()
            .endpointOverride(URI.create(bucketConfig.getEndpoint()))
            .region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(bucketConfig.getAccessKey(), bucketConfig.getSecretKey())))
            .build();
  }

  @Bean
  @Profile("prod")
  public S3Presigner s3Presigner(BucketConfigProperties bucketConfig) {
    return S3Presigner.builder()
            .endpointOverride(URI.create(bucketConfig.getEndpoint()))
            .region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(bucketConfig.getAccessKey(), bucketConfig.getSecretKey())))
            .build();
  }
}
