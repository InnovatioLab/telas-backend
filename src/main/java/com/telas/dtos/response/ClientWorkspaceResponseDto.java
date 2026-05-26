package com.telas.dtos.response;

import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Getter
public final class ClientWorkspaceResponseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -3829104455012387401L;

    private final AdRequestClientResponseDto adRequest;
    private final List<LinkResponseDto> attachments;
    private final List<AdResponseDto> ads;

    public ClientWorkspaceResponseDto(
            AdRequestClientResponseDto adRequest,
            List<LinkResponseDto> attachments,
            List<AdResponseDto> ads) {
        this.adRequest = adRequest;
        this.attachments = attachments != null ? List.copyOf(attachments) : Collections.emptyList();
        this.ads = ads != null ? List.copyOf(ads) : Collections.emptyList();
    }
}
