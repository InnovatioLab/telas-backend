package com.telas.dtos.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;

@Getter
@Setter
@NoArgsConstructor
public class SmartPlugUpdateRequestDto extends SmartPlugMetadataRequestDto {
    @Serial
    private static final long serialVersionUID = 1L;
}
