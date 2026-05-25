package com.telas.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Getter
@AllArgsConstructor
public class AdRequestMediaResponseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final LinkResponseDto ad;
    private final List<LinkResponseDto> attachments;
}
