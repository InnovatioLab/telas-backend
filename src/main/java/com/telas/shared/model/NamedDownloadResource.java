package com.telas.shared.model;

import org.springframework.http.MediaType;

public record NamedDownloadResource(byte[] content, String fileName, String contentType) {

    public static NamedDownloadResource textPlain(byte[] content, String fileName) {
        return new NamedDownloadResource(content, fileName, MediaType.TEXT_PLAIN_VALUE);
    }
}
