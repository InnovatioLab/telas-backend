package com.telas.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.io.Serial;
import java.io.Serializable;

@Value
@AllArgsConstructor
public class EmailAlertCategoryOptionDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    String code;
    String labelEn;
}
