package com.marketingproject.services;

import java.io.InputStream;
import java.util.List;

public interface BucketService {
    void upload(byte[] bytes, String fileName, String contentType, InputStream inputStream);

    void deleteAttachment(String fileName);

    List<String> getLinksDownload(List<String> objectNames);

    String getLink(String objectName);
}
